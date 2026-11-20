package com.nuvio.app.features.streams

/**
 * Entries some addons and extensions put among the links that are not videos: "Join our Discord",
 * "Upgrade to Premium", "support the project! (donating hides this message)". They are removed from
 * every link list.
 */
private val promotionalHosts = listOf(
    "discord.gg", "discord.com/invite", "discordapp.com/invite", "patreon.com", "ko-fi.com",
    "buymeacoffee.com", "paypal.me", "paypal.com/donate", "liberapay.com", "opencollective.com",
    "t.me/", "telegram.me/", "/donate",
)

private val promotionalPhrases = listOf(
    "join our discord", "join discord", "our discord", "discord server",
    "upgrade to premium", "go premium", "get premium",
    "support the project", "support my work", "support us", "support the addon",
    "donate", "donating", "donation", "buy me a coffee", "patreon", "ko-fi",
    "join our telegram", "telegram channel",
)

internal fun StreamItem.isPromotional(): Boolean {
    val links = listOfNotNull(url, externalUrl).joinToString(" ").lowercase()
    if (promotionalHosts.any { it in links }) return true
    // A torrent is a real video, whatever its name says.
    if (!infoHash.isNullOrBlank()) return false
    val text = listOfNotNull(name, title, description).joinToString(" ").lowercase()
    if (text.isBlank()) return false
    if (name?.trim()?.trimStart { !it.isLetterOrDigit() }?.equals("discord", ignoreCase = true) == true) return true
    return promotionalPhrases.any { it in text }
}

internal fun AddonStreamGroup.withoutPromotions(): AddonStreamGroup {
    if (streams.none { it.isPromotional() }) return this
    return copy(streams = streams.filterNot { it.isPromotional() })
}
