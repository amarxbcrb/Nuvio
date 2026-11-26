package com.nuvio.app.features.streams

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Reads the picture and sound of an HLS link from its playlist when its name says nothing:
 * VIDEO-RANGE=PQ or HLG (HDR), dvh1/dvhe codecs (Dolby Vision), ec-3 (DD+), « JOC » channels
 * (Dolby Atmos), ac-3 (DD), and the resolution. One small request per link, kept in memory.
 */
internal object HlsTechProbe {
    private const val TIMEOUT_MS = 6_000L
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, List<TechBadge>>()

    fun isHls(stream: StreamItem): Boolean {
        val path = (stream.url ?: return false).substringBefore('?').lowercase()
        return path.endsWith(".m3u8") || "/hls" in path || "m3u8" in path
    }

    suspend fun probe(stream: StreamItem): List<TechBadge> {
        val url = stream.url ?: return emptyList()
        if (!isHls(stream)) return emptyList()
        mutex.withLock { cache[url] }?.let { return it }
        val playlist = withTimeoutOrNull(TIMEOUT_MS) {
            runCatching {
                httpGetTextWithHeaders(url, stream.behaviorHints.proxyHeaders?.request.orEmpty())
            }.getOrNull()
        }
        val badges = playlist?.let(::parse).orEmpty()
        mutex.withLock { cache[url] = badges }
        return badges
    }

    internal fun parse(playlist: String): List<TechBadge> {
        if (!playlist.trimStart().startsWith("#EXTM3U")) return emptyList()
        val text = playlist.uppercase()
        return buildList {
            val resolution = Regex("RESOLUTION=\\d+X(\\d+)").findAll(text)
                .mapNotNull { it.groupValues[1].toIntOrNull() }.maxOrNull()
            resolution?.let { height ->
                val label = when {
                    height >= 2000 -> "4K"
                    height >= 1000 -> "1080p"
                    height >= 700 -> "720p"
                    else -> null
                }
                label?.let { add(TechBadge(it, TechBadgeKind.Resolution)) }
            }
            when {
                "DVH1" in text || "DVHE" in text || "DVAV" in text -> add(TechBadge("DV", TechBadgeKind.Hdr))
                "VIDEO-RANGE=PQ" in text || "VIDEO-RANGE=HLG" in text -> add(TechBadge("HDR", TechBadgeKind.Hdr))
            }
            val atmos = Regex("CHANNELS=\"\\d+/JOC\"").containsMatchIn(text)
            when {
                atmos -> add(TechBadge("Atmos", TechBadgeKind.Audio))
                "EC-3" in text -> add(TechBadge("DD+", TechBadgeKind.Audio))
                "AC-3" in text -> add(TechBadge("DD", TechBadgeKind.Audio))
            }
        }
    }
}
