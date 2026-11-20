package com.nuvio.app.features.streams

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreamPromotionsTest {
    private fun stream(name: String, description: String? = null, url: String? = "https://cdn.example/v.mp4", externalUrl: String? = null, infoHash: String? = null) =
        StreamItem(name = name, description = description, url = url, externalUrl = externalUrl, infoHash = infoHash, addonName = "A", addonId = "a")

    @Test
    fun removesDiscordUpgradeAndDonationEntries() {
        assertTrue(stream("✅ Discord", "Join our Discord", url = null, externalUrl = "https://discord.gg/abc").isPromotional())
        assertTrue(stream("Upgrade", "\$9/Year - Upgrade to Premium").isPromotional())
        assertTrue(stream("✨ | support the project!", "pengu.uk/donate (donating hides this message)").isPromotional())
    }

    @Test
    fun keepsRealLinks() {
        assertFalse(stream("PenguPlay 4K • 2Peckle", "The Hangover (2009) 4K • MKV • WEBRip").isPromotional())
        assertFalse(stream("Torrentio 1080p", "Movie Donate 2020", infoHash = "abc").isPromotional())
    }
}
