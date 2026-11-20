package com.nuvio.app.features.streams

/** Kind of a technical badge, which sets its colour. */
internal enum class TechBadgeKind { Resolution, Release, Codec, Hdr, Audio }

internal data class TechBadge(val text: String, val kind: TechBadgeKind)

/** « 1080p », « BluRay », « AVC », « HDR », « DD 5.1 »: what a link's name and description say about it. */
internal object StreamTechBadges {
    private fun pattern(body: String) = Regex("(?<![A-Z0-9])(?:$body)(?![A-Z0-9])")

    private val codecs = listOf(
        pattern("HEVC|X265|H[ .]?265") to "HEVC",
        pattern("AV1") to "AV1",
        pattern("AVC|X264|H[ .]?264") to "AVC",
    )

    private val audioFormats = listOf(
        pattern("ATMOS") to "Atmos",
        pattern("TRUEHD|TRUE[ .-]HD") to "TrueHD",
        pattern("DTS[ .-]?HD|DTS[ .-]?X|DTS") to "DTS",
        pattern("DDP|DD\\+|EAC3|E[ .-]AC[ .-]?3") to "DD+",
        pattern("DD|AC3|DOLBY[ .-]DIGITAL") to "DD",
        pattern("AAC") to "AAC",
        pattern("OPUS") to "Opus",
    )

    private val channels = Regex("(?<![0-9])([257])[ .]([01])(?![0-9])")

    fun detect(stream: StreamItem): List<TechBadge> {
        val texts = listOfNotNull(stream.name, stream.title, stream.description, stream.behaviorHints.filename)
        val quality = StreamQualityDetector.detect(texts)
        val text = texts.joinToString(" ").uppercase()
        return buildList {
            quality.resolution?.let { add(TechBadge(it, TechBadgeKind.Resolution)) }
            quality.release?.let { add(TechBadge(it, TechBadgeKind.Release)) }
            codecs.firstOrNull { (regex, _) -> regex.containsMatchIn(text) }?.let { add(TechBadge(it.second, TechBadgeKind.Codec)) }
            quality.hdr?.let { add(TechBadge(it, TechBadgeKind.Hdr)) }
            audioFormats.firstOrNull { (regex, _) -> regex.containsMatchIn(text) }?.let { (_, audio) ->
                val layout = channels.find(text)?.let { "${it.groupValues[1]}.${it.groupValues[2]}" }
                add(TechBadge(if (layout != null) "$audio $layout" else audio, TechBadgeKind.Audio))
            }
        }
    }
}
