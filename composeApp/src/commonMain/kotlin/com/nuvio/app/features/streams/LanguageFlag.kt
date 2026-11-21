package com.nuvio.app.features.streams

/** Flag emoji for a language code (« fr » → 🇫🇷), null when the language has no obvious flag. */
internal fun languageFlag(code: String): String? {
    val country = when (code.trim().lowercase().substringBefore('-')) {
        "fr" -> "FR"; "en" -> "GB"; "ar" -> "DZ"; "es" -> "ES"; "pt" -> "PT"; "it" -> "IT"
        "de" -> "DE"; "tr" -> "TR"; "ru" -> "RU"; "ja" -> "JP"; "ko" -> "KR"; "zh" -> "CN"
        "hi" -> "IN"; "id" -> "ID"; "vi" -> "VN"; "th" -> "TH"; "pl" -> "PL"; "nl" -> "NL"
        "uk" -> "UA"; "fa" -> "IR"; "he" -> "IL"; "ms" -> "MY"; "ro" -> "RO"; "el" -> "GR"
        else -> return null
    }
    // Regional indicator symbols ('A' = U+1F1E6), written as UTF-16 surrogate pairs.
    return country.map { char ->
        val offset = 0x1F1E6 + (char - 'A') - 0x10000
        "${((offset shr 10) + 0xD800).toChar()}${((offset and 0x3FF) + 0xDC00).toChar()}"
    }.joinToString("")
}

/** Flag for a link's version: its single audio language (VF → 🇫🇷), else the subtitle language. Null for Multi/VO. */
internal fun StreamLanguageInfo.versionFlag(): String? {
    if (multi) return null
    audio.singleOrNull()?.let { return languageFlag(it) }
    if (audio.isEmpty()) subtitles.sorted().firstOrNull()?.let { return languageFlag(it) }
    if (audio.isEmpty() && subtitles.isEmpty()) providerLanguage?.let { return languageFlag(it) }
    return null
}
