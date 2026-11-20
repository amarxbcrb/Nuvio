package com.nuvio.app.features.streams

/**
 * « Source du film »: the link last launched for each title (movie, or series for all its
 * episodes). It stays first in the links list and « Reprendre » uses it, until another link is
 * launched or the user taps « Changer ».
 */
object FilmSourceRepository {
    private const val KEY = "stream_film_sources_v1"
    private const val FIELD = "\t"
    private const val MAX_TITLES = 400

    /** What identifies a link from one search to the next. */
    data class Remembered(val addonId: String, val sourceName: String, val name: String, val url: String)

    private var sources: LinkedHashMap<String, Remembered>? = null

    private fun load(): LinkedHashMap<String, Remembered> {
        sources?.let { return it }
        val loaded = LinkedHashMap<String, Remembered>()
        BingeGroupCacheStorage.load(KEY)?.lineSequence()?.forEach { line ->
            val parts = line.split(FIELD)
            if (parts.size >= 5 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                loaded[parts[0]] = Remembered(parts[1], parts[2], parts[3], parts[4])
            }
        }
        sources = loaded
        return loaded
    }

    fun get(titleKey: String?): Remembered? = titleKey?.let { load()[it] }

    /** Remembers [stream] as the source of [titleKey]. */
    fun remember(titleKey: String?, stream: StreamItem) {
        if (titleKey.isNullOrBlank() || stream.addonId.isBlank()) return
        val map = load()
        map.remove(titleKey)
        map[titleKey] = Remembered(
            addonId = stream.addonId.clean(),
            sourceName = stream.sourceName.orEmpty().clean(),
            name = stream.cleanDisplayName().clean(),
            url = (stream.url ?: stream.infoHash ?: stream.externalUrl).orEmpty().clean(),
        )
        while (map.size > MAX_TITLES) map.remove(map.keys.first())
        persist(map)
        // The automatic start (« Lire », « Reprendre », next episodes) uses the same source.
        SeriesSourceMemoryRepository.save(titleKey, stream.addonId, stream.serverKey())
    }

    fun forget(titleKey: String?) {
        if (titleKey == null) return
        val map = load()
        if (map.remove(titleKey) != null) persist(map)
        SeriesSourceMemoryRepository.forget(titleKey)
    }

    /** The link of [links] that is the remembered source: same address, else same source and name, else same source. */
    fun <T> find(titleKey: String?, links: List<T>, streamOf: (T) -> StreamItem): T? {
        val played = titleKey?.let(SeriesSourceMemoryRepository::get)
        // The source that last played in the player wins over an older choice made in the list.
        val remembered = get(titleKey)?.takeIf { played == null || it.addonId == played.addonId }
            ?: return played?.let { last ->
            // A source that played in the player (or an earlier version): same addon, its server first.
            links.filter { streamOf(it).addonId == last.addonId }
                .sortedBy { if (last.sourceName != null && streamOf(it).serverKey() == last.sourceName) 0 else 1 }
                .firstOrNull()
        }
        fun StreamItem.address() = (url ?: infoHash ?: externalUrl).orEmpty()
        val sameSource = links.filter {
            val stream = streamOf(it)
            stream.addonId == remembered.addonId && stream.sourceName.orEmpty() == remembered.sourceName
        }
        return links.firstOrNull { remembered.url.isNotEmpty() && streamOf(it).address() == remembered.url }
            ?: sameSource.firstOrNull { streamOf(it).cleanDisplayName() == remembered.name }
            ?: sameSource.firstOrNull()
    }

    private fun persist(map: Map<String, Remembered>) {
        BingeGroupCacheStorage.save(
            KEY,
            map.entries.joinToString("\n") { (key, value) ->
                listOf(key, value.addonId, value.sourceName, value.name, value.url).joinToString(FIELD)
            },
        )
    }

    private fun String.clean() = replace(FIELD, " ").replace("\n", " ")
}
