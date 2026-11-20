package com.nuvio.app.features.streams

import kotlin.test.Test
import kotlin.test.assertEquals

class CompactLinksTest {
    @Test
    fun `version of a link names its audio or its subtitles`() {
        assertEquals("VF", StreamLanguageInfo(audio = setOf("fr")).versionKey())
        assertEquals("VOSTFR", StreamLanguageInfo(subtitles = setOf("fr")).versionKey())
        assertEquals("MULTI", StreamLanguageInfo(multi = true).versionKey())
        assertEquals("EN", StreamLanguageInfo(audio = setOf("en")).versionKey())
        assertEquals("VOSTAR", StreamLanguageInfo(subbed = true, providerLanguage = "ar").versionKey())
        assertEquals("VO", StreamLanguageInfo(original = true).versionKey())
        assertEquals("VO", StreamLanguageInfo().versionKey())
    }

    @Test
    fun `dubbed and better links come first`() {
        val group = AddonStreamGroup(
            addonName = "Addon",
            addonId = "addon:test",
            streams = listOf(
                StreamItem(name = "Film 720p VOSTFR", url = "https://a/1", addonName = "Addon", addonId = "addon:test"),
                StreamItem(name = "Film 720p VF", url = "https://a/2", addonName = "Addon", addonId = "addon:test"),
                StreamItem(name = "Film 1080p VF", url = "https://a/3", addonName = "Addon", addonId = "addon:test"),
            ),
        )
        val links = compactLinks(
            groups = listOf(group),
            pinnedSourceIds = emptyList(),
            preferences = StreamLanguagePreferences(dubMode = StreamDubMode.PreferDubbed),
        )
        assertEquals(listOf("https://a/3", "https://a/2", "https://a/1"), links.map { it.stream.url })
        assertEquals(listOf("1080p", "720p", "720p"), links.map { it.resolution })
        assertEquals(listOf("VF", "VF", "VOSTFR"), links.map { it.version })
    }
}
