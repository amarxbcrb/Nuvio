package com.nuvio.app.features.streams

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A stream source that can be searched on its own (addon, plugin provider, CloudStream provider or server). */
data class StreamSourceOption(
    val id: String,
    val name: String,
)

/**
 * Where the links of a title are searched. The single source is remembered for each title (a source
 * chosen for one anime does not limit the films); "favorites only" is set for each watching profile.
 * `null` scope means every source is searched. When the chosen source cannot serve a title, every
 * source is searched for that title instead, so playback never dead-ends on an incompatible choice.
 */
object StreamSearchScopeRepository {
    private const val FIELD = "\t"
    private const val LINE = "\n"
    private const val FAVORITES_ONLY_KEY = "stream_favorites_only_v1"

    private val _scope = MutableStateFlow<StreamSourceOption?>(null)

    /** Single source of the title being searched, null for every source. */
    val scope: StateFlow<StreamSourceOption?> = _scope.asStateFlow()

    private val _favoritesOnly = MutableStateFlow(false)

    /** Only the favorite (starred) sources of the active profile are searched, when one can serve the title. */
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

    /** Single source chosen for each title key. */
    private var titleScopes: Map<String, StreamSourceOption> = emptyMap()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    /** Called when the app profile, the watching profile or synced settings change. */
    fun onProfileChanged() {
        loadFromDisk()
    }

    /** Called when another title is searched: shows that title's single source. */
    fun onTitleChanged() {
        ensureLoaded()
        _scope.value = StreamProfileRepository.currentTitleKey?.let { titleScopes[it] }
    }

    /** Limits the search of the current title to [option], or searches every source with null. */
    fun setScope(option: StreamSourceOption?) {
        ensureLoaded()
        if (option == _scope.value) return
        _scope.value = option
        val titleKey = StreamProfileRepository.currentTitleKey ?: return
        titleScopes = if (option == null) titleScopes - titleKey else titleScopes + (titleKey to option)
        StreamBadgeSettingsStorage.saveStreamSearchScope(
            titleScopes.entries.joinToString(LINE) { (key, source) ->
                listOf(key, source.id, source.name).joinToString(FIELD) { it.replace(FIELD, " ").replace(LINE, " ") }
            },
        )
    }

    fun setFavoritesOnly(enabled: Boolean) {
        ensureLoaded()
        if (enabled == _favoritesOnly.value) return
        _favoritesOnly.value = enabled
        BingeGroupCacheStorage.save(favoritesOnlyKey(), enabled.toString())
    }

    /** French keeps the older storage; the other profiles have their own. */
    private fun favoritesOnlyKey(): String =
        StreamProfileRepository.storageKey(FAVORITES_ONLY_KEY) ?: FAVORITES_ONLY_KEY

    /** Part of the stream request key: changes when the favorites-only search or the favorites change. */
    fun favoritesSignature(): String {
        ensureLoaded()
        if (!StreamProfileRepository.active.value.hasPreferences) return "all-profile"
        if (!_favoritesOnly.value) return "all"
        PinnedStreamSourcesRepository.ensureLoaded()
        return "fav:" + PinnedStreamSourcesRepository.pinnedSourceIds.value.sorted().joinToString(",")
    }

    /**
     * Which sources (by id) a stream search covers among [available]: the profile's favorites when
     * only favorites are searched and one of them can serve the title, else every source. The source
     * filter of the links screen only changes what is shown, never the search.
     */
    fun searchFilter(available: List<StreamSourceOption>): (String) -> Boolean {
        // The "All" profile searches every source.
        if (!StreamProfileRepository.active.value.hasPreferences) return { true }
        if (_favoritesOnly.value && available.any { PinnedStreamSourcesRepository.isFavoriteGroup(it.id) }) {
            return { id -> PinnedStreamSourcesRepository.isFavoriteGroup(id) }
        }
        return { true }
    }

    /** No single-source search any more: every title searches its profile's sources. */
    @Suppress("UNUSED_PARAMETER")
    fun resolveScopeId(available: List<StreamSourceOption>): String? = null

    private fun loadFromDisk() {
        hasLoaded = true
        StreamProfileRepository.ensureLoaded()
        _favoritesOnly.value = BingeGroupCacheStorage.load(favoritesOnlyKey()) == "true"
        // One line per title: key, source id, source name. The older single global choice (no
        // tab) is dropped, so it no longer limits every title.
        titleScopes = StreamBadgeSettingsStorage.loadStreamSearchScope()
            ?.split(LINE)
            ?.mapNotNull { line ->
                val parts = line.split(FIELD)
                if (parts.size < 2) return@mapNotNull null
                val key = parts[0].trim()
                val id = parts[1].trim()
                if (key.isEmpty() || id.isEmpty()) return@mapNotNull null
                key to StreamSourceOption(id = id, name = parts.getOrNull(2)?.trim()?.ifEmpty { null } ?: id)
            }
            ?.toMap()
            .orEmpty()
        _scope.value = StreamProfileRepository.currentTitleKey?.let { titleScopes[it] }
    }
}

internal fun buildStreamSourceOptions(
    streamAddons: List<InstalledStreamAddonTarget>,
    pluginProviderGroups: List<PluginProviderGroup>,
    cloudStreamProviderGroups: List<CloudStreamProviderGroup>,
    serverGroups: List<AddonStreamGroup>,
): List<StreamSourceOption> =
    (
        streamAddons.map { StreamSourceOption(it.addonId, it.addonName) } +
            pluginProviderGroups.map { StreamSourceOption(it.addonId, it.addonName) } +
            cloudStreamProviderGroups.map { StreamSourceOption(it.addonId, it.addonName) } +
            serverGroups.map { StreamSourceOption(it.addonId, it.addonName) }
        ).distinctBy { it.id }
