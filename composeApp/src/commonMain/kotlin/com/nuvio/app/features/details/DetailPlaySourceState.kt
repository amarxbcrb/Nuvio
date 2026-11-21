package com.nuvio.app.features.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.details.components.DetailPlaySource
import com.nuvio.app.features.streams.FilmSourceRepository
import com.nuvio.app.features.streams.StreamLanguageDetector
import com.nuvio.app.features.streams.StreamQualityDetector
import com.nuvio.app.features.streams.StreamsRepository

/**
 * Source « Lire » will use for [titleKey] (meta id, the key used by [FilmSourceRepository]):
 * the link last launched for this title, with its language and resolution badges.
 * Null when no link was launched yet: nothing is known before a search, so nothing is shown.
 */
@Composable
internal fun rememberDetailPlaySource(titleKey: String, refreshKey: Any? = null): DetailPlaySource? =
    remember(titleKey, refreshKey) {
        val remembered = FilmSourceRepository.get(titleKey) ?: return@remember null
        val texts = listOf(remembered.name, remembered.sourceName).filter { it.isNotBlank() }
        val language = StreamLanguageDetector.detect(texts, sourceName = remembered.addonId).label
        val resolution = StreamQualityDetector.detect(texts).resolution
        DetailPlaySource(
            name = remembered.sourceName.ifBlank { remembered.name }.ifBlank { remembered.addonId },
            badges = listOfNotNull(language, resolution),
        )
    }

/**
 * False only when the last link search for this title is finished and found exactly one link:
 * the source segment would offer no choice. While unknown (no search yet, or still loading) the
 * segment stays visible, since the number of links is only known after a search.
 */
@Composable
internal fun rememberShowSourceSegment(type: String, videoId: String): Boolean {
    val state by StreamsRepository.uiState.collectAsStateWithLifecycle()
    val token = state.requestToken ?: return true
    if (!token.startsWith("$type::$videoId::") || state.isAnyLoading) return true
    return state.groups.sumOf { it.streams.size } != 1
}
