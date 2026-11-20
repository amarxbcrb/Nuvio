package com.nuvio.app.features.streams

import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A way of watching, created and named by the user: its link languages, link order, subtitles,
 * extension languages, favorite sources, where to search and the sources it skips. The built-in
 * [ALL] searches every source in every language, without favorites or filters.
 *
 * Two profiles are the same profile when they have the same [id]; the name can change.
 */
class StreamProfile(val id: String, val name: String) {
    /** Whether the profile has favorites, link languages and a search scope ("Tout" has none). */
    val hasPreferences: Boolean
        get() = id != ALL_ID

    /** Subtitle language the player picks for this profile, null to keep the player setting. */
    val subtitleLanguage: String?
        get() = StreamProfileRepository.subtitleLanguageFor(this)

    override fun equals(other: Any?): Boolean = other is StreamProfile && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "StreamProfile($id, $name)"

    companion object {
        const val ALL_ID = "all"

        /** Id of the first profile, which keeps the link settings made before profiles existed. */
        const val LEGACY_ID = "fr"

        val ALL = StreamProfile(ALL_ID, "")
    }
}

/**
 * The user's profiles, which profile each title uses (remembered once chosen), the profile of new
 * titles, and which profile the favorites and link languages currently follow.
 */
object StreamProfileRepository {
    private const val PROFILES_KEY = "stream_profiles_v2"
    private const val TITLES_KEY = "stream_profile_titles_v1"
    private const val DEFAULT_KEY = "stream_profile_default_v1"
    private const val SUBTITLES_KEY = "stream_profile_subtitles_v1"

    /** Stored for "keep the player setting", so it differs from "never chosen". */
    private const val PLAYER_SUBTITLES = "-"
    private const val SEPARATOR = "\t"

    /** Name of the profile made from the settings that existed before profiles could be created. */
    private const val LEGACY_NAME = "Français"

    private val _profiles = MutableStateFlow<List<StreamProfile>>(emptyList())

    /** The user's profiles, in their order ("Tout" is not in this list). */
    val profiles: StateFlow<List<StreamProfile>> = _profiles.asStateFlow()

    /** Profile id of each title key. */
    private val _titleProfiles = MutableStateFlow<Map<String, String>>(emptyMap())
    val titleProfiles: StateFlow<Map<String, String>> = _titleProfiles.asStateFlow()

    private val _defaultProfile = MutableStateFlow(StreamProfile.ALL)

    /** Profile of the titles that have none of their own. */
    val defaultProfile: StateFlow<StreamProfile> = _defaultProfile.asStateFlow()

    private val _active = MutableStateFlow(StreamProfile.ALL)

    /** The profile the favorites and link languages follow right now. */
    val active: StateFlow<StreamProfile> = _active.asStateFlow()

    /** The title whose links are being searched, used by the search sheet and manual choices. */
    var currentTitleKey: String? = null
        private set

    private val _subtitleChoices = MutableStateFlow<Map<String, String>>(emptyMap())

    /** Subtitle language chosen for each profile id ("-" keeps the player setting). */
    val subtitleChoices: StateFlow<Map<String, String>> = _subtitleChoices.asStateFlow()

    private var loaded = false

    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val storedProfiles = BingeGroupCacheStorage.load(PROFILES_KEY)
        _profiles.value = if (storedProfiles == null) {
            // Before profiles could be created, the French profile held the user's settings: it
            // becomes the first profile, the other built-in ones are dropped.
            listOf(StreamProfile(StreamProfile.LEGACY_ID, LEGACY_NAME)).also(::persistProfiles)
        } else {
            storedProfiles.lineSequence()
                .mapNotNull { line ->
                    val id = line.substringBefore(SEPARATOR, "").trim()
                    val name = line.substringAfter(SEPARATOR, "").trim()
                    if (id.isEmpty() || id == StreamProfile.ALL_ID || name.isEmpty()) null else StreamProfile(id, name)
                }
                .distinctBy { it.id }
                .toList()
        }
        _titleProfiles.value = BingeGroupCacheStorage.load(TITLES_KEY)
            ?.lineSequence()
            ?.mapNotNull { line ->
                val key = line.substringBefore(SEPARATOR, "").takeIf { it.isNotBlank() }
                val id = line.substringAfter(SEPARATOR, "").trim().takeIf { it.isNotEmpty() }
                if (key != null && id != null) key to id else null
            }
            ?.toMap()
            .orEmpty()
        _defaultProfile.value = BingeGroupCacheStorage.load(DEFAULT_KEY)?.let(::resolve)
            ?: _profiles.value.firstOrNull()
            ?: StreamProfile.ALL
        _subtitleChoices.value = BingeGroupCacheStorage.load(SUBTITLES_KEY)
            ?.lineSequence()
            ?.mapNotNull { line ->
                val id = line.substringBefore(SEPARATOR, "").trim()
                val code = line.substringAfter(SEPARATOR, "").trim()
                if (id.isNotEmpty() && code.isNotEmpty()) id to code else null
            }
            ?.toMap()
            .orEmpty()
        _active.value = resolve(_active.value.id) ?: _defaultProfile.value
    }

    /** The profile with [id] ("all" is "Tout"), null when it was deleted. */
    fun resolve(id: String?): StreamProfile? {
        ensureLoaded()
        if (id == StreamProfile.ALL_ID) return StreamProfile.ALL
        return _profiles.value.firstOrNull { it.id == id }
    }

    /** Creates a profile named [name] with default settings and returns it. */
    fun createProfile(name: String): StreamProfile? {
        ensureLoaded()
        val cleanName = name.trim().replace(SEPARATOR, " ").replace("\n", " ").take(MAX_NAME_LENGTH)
        if (cleanName.isEmpty()) return null
        val id = "p" + Clock.System.now().toEpochMilliseconds().toString(36) + Random.nextInt(1000).toString(36)
        val profile = StreamProfile(id, cleanName)
        _profiles.value = _profiles.value + profile
        persistProfiles(_profiles.value)
        if (_defaultProfile.value == StreamProfile.ALL && _profiles.value.size == 1) setDefault(profile)
        return profile
    }

    fun renameProfile(profile: StreamProfile, name: String) {
        ensureLoaded()
        val cleanName = name.trim().replace(SEPARATOR, " ").replace("\n", " ").take(MAX_NAME_LENGTH)
        if (cleanName.isEmpty() || !profile.hasPreferences) return
        val renamed = StreamProfile(profile.id, cleanName)
        _profiles.value = _profiles.value.map { if (it.id == profile.id) renamed else it }
        persistProfiles(_profiles.value)
        if (_active.value.id == profile.id) _active.value = renamed
        if (_defaultProfile.value.id == profile.id) _defaultProfile.value = renamed
    }

    /** Deletes [profile]: its titles go back to the default profile, its settings are cleared. */
    fun deleteProfile(profile: StreamProfile) {
        ensureLoaded()
        if (!profile.hasPreferences) return
        _profiles.value = _profiles.value.filterNot { it.id == profile.id }
        persistProfiles(_profiles.value)
        val titles = _titleProfiles.value.filterValues { it != profile.id }
        if (titles != _titleProfiles.value) {
            _titleProfiles.value = titles
            persistTitles(titles)
        }
        if (_defaultProfile.value.id == profile.id) {
            setDefault(_profiles.value.firstOrNull() ?: StreamProfile.ALL)
        }
        if (_subtitleChoices.value.containsKey(profile.id)) {
            _subtitleChoices.value = _subtitleChoices.value - profile.id
            persistSubtitles(_subtitleChoices.value)
        }
        // Its link languages, favorites and search choices are cleared, so a new profile never
        // inherits them (the first profile keeps the older storage, which is left as is).
        if (profile.id != StreamProfile.LEGACY_ID) {
            PROFILE_STORAGE_BASES.forEach { base -> BingeGroupCacheStorage.save("${base}_${profile.id}", "") }
        }
        if (_active.value.id == profile.id) switchActive(profileFor(currentTitleKey))
    }

    /** Subtitle language the player picks for [profile], null to keep the player setting. */
    fun subtitleLanguageFor(profile: StreamProfile): String? {
        ensureLoaded()
        val choice = _subtitleChoices.value[profile.id] ?: return null
        return choice.takeUnless { it == PLAYER_SUBTITLES }
    }

    /** Sets the subtitle language of [profile]; null keeps the player setting. */
    fun setSubtitleLanguage(profile: StreamProfile, language: String?) {
        ensureLoaded()
        if (!profile.hasPreferences) return
        val updated = _subtitleChoices.value + (profile.id to (language ?: PLAYER_SUBTITLES))
        if (updated == _subtitleChoices.value) return
        _subtitleChoices.value = updated
        persistSubtitles(updated)
    }

    /** Order of dubbed and subtitled links of [profile], as chosen by the user. */
    fun dubModeFor(profile: StreamProfile): StreamDubMode =
        if (profile == _active.value) {
            StreamLanguagePreferencesRepository.snapshot().dubMode
        } else {
            StreamLanguagePreferencesRepository.preferencesFor(profile).dubMode
        }

    /** Called when the app profile changes: reload everything and follow the default profile. */
    fun onProfileChanged() {
        loaded = false
        ensureLoaded()
        switchActive(_defaultProfile.value)
    }

    /** The profile chosen for [titleKey] on its links screen, else the default one (set in Settings). */
    fun profileFor(titleKey: String?): StreamProfile {
        ensureLoaded()
        return titleKey?.let { key -> _titleProfiles.value[key]?.let(::resolve) } ?: _defaultProfile.value
    }

    /** Chooses [profile] for the title on screen only; without a title, for every title. */
    fun select(profile: StreamProfile) {
        val titleKey = currentTitleKey
        if (titleKey == null) {
            setDefault(profile)
            switchActive(profile)
            return
        }
        if (profile == _defaultProfile.value) clearForTitle(titleKey) else setForTitle(titleKey, profile)
        switchActive(profile)
    }

    /** The title goes back to the default profile. */
    fun clearForTitle(titleKey: String) {
        ensureLoaded()
        if (!_titleProfiles.value.containsKey(titleKey)) return
        val updated = _titleProfiles.value - titleKey
        _titleProfiles.value = updated
        persistTitles(updated)
        if (currentTitleKey == titleKey) switchActive(_defaultProfile.value)
    }

    /** Sets the profile of every title that has none of its own (Settings). */
    fun selectDefault(profile: StreamProfile) {
        setDefault(profile)
        if (!hasOwnProfile(currentTitleKey)) switchActive(profile)
    }

    /** Whether [titleKey] has a profile of its own (else it follows the default profile). */
    fun hasOwnProfile(titleKey: String?): Boolean {
        ensureLoaded()
        return titleKey != null && _titleProfiles.value[titleKey]?.let(::resolve) != null
    }

    /** Makes the favorites and link languages follow the profile of [titleKey]. */
    fun activateForTitle(titleKey: String?) {
        val titleChanged = currentTitleKey != titleKey
        currentTitleKey = titleKey
        switchActive(profileFor(titleKey))
        if (titleChanged) StreamSearchScopeRepository.onTitleChanged()
    }

    /** Makes the favorites and link languages follow [profile] (fallback search, settings page). */
    fun activateTemporarily(profile: StreamProfile) {
        ensureLoaded()
        switchActive(profile)
    }

    /** Remembers [profile] for [titleKey]. */
    fun setForTitle(titleKey: String, profile: StreamProfile) {
        ensureLoaded()
        val updated = _titleProfiles.value + (titleKey to profile.id)
        if (updated != _titleProfiles.value) {
            _titleProfiles.value = updated
            persistTitles(updated)
        }
        if (currentTitleKey == titleKey) switchActive(profile)
    }

    /** Profile used by the titles that have none of their own. */
    fun setDefault(profile: StreamProfile) {
        ensureLoaded()
        if (_defaultProfile.value == profile && _defaultProfile.value.name == profile.name) return
        _defaultProfile.value = profile
        BingeGroupCacheStorage.save(DEFAULT_KEY, profile.id)
    }

    private fun switchActive(profile: StreamProfile) {
        if (_active.value == profile && _active.value.name == profile.name) return
        val changed = _active.value != profile
        _active.value = profile
        if (!changed) return
        PinnedStreamSourcesRepository.onProfileChanged()
        StreamLanguagePreferencesRepository.onProfileChanged()
        StreamSearchScopeRepository.onProfileChanged()
    }

    /** Storage key of a per-profile setting; null for the first profile, which keeps the old storage. */
    internal fun storageKey(base: String): String? {
        // Favorites or languages read before the profiles were loaded would use the wrong profile.
        ensureLoaded()
        return if (_active.value.id == StreamProfile.LEGACY_ID) null else "${base}_${_active.value.id}"
    }

    private fun persistProfiles(profiles: List<StreamProfile>) {
        BingeGroupCacheStorage.save(PROFILES_KEY, profiles.joinToString("\n") { "${it.id}$SEPARATOR${it.name}" })
    }

    private fun persistTitles(titles: Map<String, String>) {
        BingeGroupCacheStorage.save(TITLES_KEY, titles.entries.joinToString("\n") { "${it.key}$SEPARATOR${it.value}" })
    }

    private fun persistSubtitles(choices: Map<String, String>) {
        BingeGroupCacheStorage.save(SUBTITLES_KEY, choices.entries.joinToString("\n") { "${it.key}$SEPARATOR${it.value}" })
    }

    private const val MAX_NAME_LENGTH = 24

    /** Bases of the per-profile settings, cleared when a profile is deleted. */
    private val PROFILE_STORAGE_BASES = listOf(
        "stream_language_preferences",
        "pinned_stream_sources",
        "stream_favorites_only_v1",
        "stream_disabled_sources",
    )
}
