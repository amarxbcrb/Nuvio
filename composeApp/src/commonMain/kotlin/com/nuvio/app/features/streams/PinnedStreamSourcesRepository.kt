package com.nuvio.app.features.streams

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PROFILE_PINNED_KEY = "pinned_stream_sources"
private const val PROFILE_DISABLED_KEY = "stream_disabled_sources"

object PinnedStreamSourcesRepository {
    private const val SEPARATOR = "\n"

    private val _pinnedSourceIds = MutableStateFlow<List<String>>(emptyList())
    val pinnedSourceIds: StateFlow<List<String>> = _pinnedSourceIds.asStateFlow()

    private val _disabledSourceIds = MutableStateFlow<Set<String>>(emptySet())

    /** Sources (addon or extension ids) the active profile does not search: their switch is off. */
    val disabledSourceIds: StateFlow<Set<String>> = _disabledSourceIds.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun sourceKeyFor(addonId: String, sourceName: String?): String {
        val source = sourceName?.trim()?.takeIf { it.isNotEmpty() }
        return if (source == null) addonId else "$addonId|$source"
    }

    fun sourceKeyFor(stream: StreamItem): String =
        sourceKeyFor(addonId = stream.addonId, sourceName = stream.sourceName)

    fun isPinned(sourceKey: String): Boolean = sourceKey in _pinnedSourceIds.value

    fun setPinned(sourceKey: String, pinned: Boolean) {
        val normalized = sourceKey.trim()
        if (normalized.isEmpty()) return
        ensureLoaded()
        val current = _pinnedSourceIds.value
        val updated = when {
            pinned && normalized in current -> return
            pinned -> current + normalized
            else -> current.filterNot { it == normalized }
        }
        if (updated == current) return
        _pinnedSourceIds.value = updated
        persist(updated)
    }

    /** Makes the whole source [addonId] a favorite, or removes it and every one of its sub-sources. */
    fun setGroupFavorite(addonId: String, favorite: Boolean) {
        ensureLoaded()
        val current = _pinnedSourceIds.value
        val prefix = "$addonId|"
        val updated = if (favorite) {
            if (current.any { it == addonId || it.startsWith(prefix) }) return
            current + addonId
        } else {
            current.filterNot { it == addonId || it.startsWith(prefix) }
        }
        if (updated == current) return
        _pinnedSourceIds.value = updated
        persist(updated)
    }

    fun toggle(sourceKey: String) {
        setPinned(sourceKey, !isPinned(sourceKey))
    }

    fun isSourceEnabled(sourceId: String): Boolean {
        ensureLoaded()
        return sourceId !in _disabledSourceIds.value
    }

    /** Turns the search of [sourceId] on or off for the active profile. */
    fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        ensureLoaded()
        val current = _disabledSourceIds.value
        val updated = if (enabled) current - sourceId else current + sourceId
        if (updated == current) return
        _disabledSourceIds.value = updated
        // Each profile has its own; none is ever stored for the first profile's older storage.
        val key = StreamProfileRepository.storageKey(PROFILE_DISABLED_KEY) ?: PROFILE_DISABLED_KEY
        BingeGroupCacheStorage.save(key, updated.joinToString(SEPARATOR))
    }

    /** Whether the source group [addonId], or one of its sources, is a favorite. */
    fun isFavoriteGroup(addonId: String): Boolean {
        ensureLoaded()
        val prefix = "$addonId|"
        return _pinnedSourceIds.value.any { key -> key == addonId || key.startsWith(prefix) }
    }

    private fun loadFromDisk() {
        hasLoaded = true
        // Each watching profile has its own favorites; the French one keeps the older storage.
        val profileKey = StreamProfileRepository.storageKey(PROFILE_PINNED_KEY)
        val stored = if (profileKey == null) {
            StreamBadgeSettingsStorage.loadPinnedStreamSources()
        } else {
            BingeGroupCacheStorage.load(profileKey)
        }
        _pinnedSourceIds.value = stored
            ?.split(SEPARATOR)
            ?.map(String::trim)
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            .orEmpty()
        val disabledKey = StreamProfileRepository.storageKey(PROFILE_DISABLED_KEY) ?: PROFILE_DISABLED_KEY
        // "Tout" searches every source.
        _disabledSourceIds.value = if (StreamProfileRepository.active.value.hasPreferences) {
            BingeGroupCacheStorage.load(disabledKey)
                ?.split(SEPARATOR)
                ?.map(String::trim)
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                .orEmpty()
        } else {
            emptySet()
        }
    }

    private fun persist(ids: List<String>) {
        val profileKey = StreamProfileRepository.storageKey(PROFILE_PINNED_KEY)
        if (profileKey == null) {
            StreamBadgeSettingsStorage.savePinnedStreamSources(ids.joinToString(SEPARATOR))
        } else {
            BingeGroupCacheStorage.save(profileKey, ids.joinToString(SEPARATOR))
        }
    }
}

/** Favorite sources first, the order being otherwise kept, so they are queried and picked first. */
internal fun <T> List<T>.favoriteSourcesFirst(addonIdOf: (T) -> String): List<T> =
    sortedBy { item -> if (PinnedStreamSourcesRepository.isFavoriteGroup(addonIdOf(item))) 0 else 1 }
