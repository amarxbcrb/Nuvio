package com.nuvio.app.features.details

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.tmdb.TmdbService
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.tmdb.buildTmdbUrl
import com.nuvio.app.features.trakt.TraktCommentReview
import com.nuvio.app.features.trakt.TraktCommentsPage
import com.nuvio.app.features.trakt.TraktCommentsRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reviews shown under a film or a series: Trakt comments (public, with or without a Trakt
 * account) followed by TMDB reviews (no account needed). Later pages come from Trakt only.
 */
/** What the comments show: a language (null: all) and, for a series, one episode (null: the whole series). */
data class MetaCommentsFilter(
    val language: String? = null,
    val episode: Pair<Int, Int>? = null,
)

internal object MetaCommentsRepository {
    private val log = Logger.withTag("MetaComments")
    private val json = Json { ignoreUnknownKeys = true }

    /** Languages offered for the TMDB reviews (ISO 639-1 and TMDB locale). */
    val languages: List<Pair<String, String>> = listOf(
        "fr" to "fr-FR", "en" to "en-US", "es" to "es-ES", "de" to "de-DE",
        "it" to "it-IT", "pt" to "pt-BR", "ar" to "ar-SA", "ja" to "ja-JP",
    )

    private val _filter = MutableStateFlow(MetaCommentsFilter())
    val filter: StateFlow<MetaCommentsFilter> = _filter.asStateFlow()
    private var filterMetaId: String? = null

    fun setLanguage(language: String?) {
        _filter.value = _filter.value.copy(language = language)
    }

    fun setEpisode(episode: Pair<Int, Int>?) {
        _filter.value = _filter.value.copy(episode = episode)
    }

    /** A new title starts on « all languages, whole series ». */
    fun onTitleShown(metaId: String) {
        if (filterMetaId == metaId) return
        filterMetaId = metaId
        _filter.value = MetaCommentsFilter()
    }

    suspend fun getCommentsPage(meta: MetaDetails, page: Int = 1, forceRefresh: Boolean = false): TraktCommentsPage {
        val current = _filter.value
        // Trakt comments carry no language: they are written in English almost always.
        val withTrakt = current.language == null || current.language == "en"
        val trakt = if (!withTrakt) null else runCatching {
            TraktCommentsRepository.getCommentsPage(meta, page, forceRefresh, episode = current.episode)
        }.onFailure { log.w { "Trakt comments failed: ${it.message}" } }.getOrNull()
        // TMDB has reviews for the whole film or series only.
        if (page > 1 || current.episode != null) return trakt ?: TraktCommentsPage(emptyList(), page, 0, 0)
        val tmdb = runCatching { tmdbReviews(meta, current.language) }
            .onFailure { log.w { "TMDB reviews failed: ${it.message}" } }
            .getOrDefault(emptyList())
        val items = trakt?.items.orEmpty() + tmdb
        return TraktCommentsPage(
            items = items,
            currentPage = 1,
            pageCount = maxOf(trakt?.pageCount ?: 0, if (items.isEmpty()) 0 else 1),
            itemCount = (trakt?.itemCount ?: 0) + tmdb.size,
        )
    }

    private suspend fun tmdbReviews(meta: MetaDetails, language: String?): List<TraktCommentReview> {
        val mediaType = TmdbService.normalizeMediaType(meta.type)
        if (mediaType != "movie" && mediaType != "tv") return emptyList()
        val apiKey = TmdbSettingsRepository.effectiveApiKey().takeIf { it.isNotBlank() } ?: return emptyList()
        val tmdbId = TmdbService.ensureTmdbId(meta.id, mediaType, meta.imdbId) ?: return emptyList()
        // TMDB files reviews by language: all the offered ones (French first), or the one chosen,
        // with up to 3 pages each.
        val locales = languages.filter { language == null || it.first == language }.map { it.second }
        val reviews = coroutineScope {
            locales.map { locale ->
                async {
                    val found = mutableListOf<TmdbReviewDto>()
                    for (page in 1..3) {
                        val url = buildTmdbUrl("$mediaType/$tmdbId/reviews", apiKey, mapOf("language" to locale, "page" to page.toString()))
                        val response = runCatching { json.decodeFromString<TmdbReviewsResponse>(httpGetText(url)) }.getOrNull() ?: break
                        found += response.results
                        if (page >= response.totalPages) break
                    }
                    found
                }
            }.awaitAll().flatten()
        }.distinctBy { it.id }
        return reviews.mapNotNull { review ->
            val content = review.content?.trim().orEmpty()
            if (content.isEmpty()) return@mapNotNull null
            TraktCommentReview(
                id = review.id.hashCode().toLong(),
                authorDisplayName = review.authorDetails?.name?.takeIf { it.isNotBlank() }
                    ?: review.author?.takeIf { it.isNotBlank() }
                    ?: "TMDB",
                authorUsername = review.authorDetails?.username,
                comment = content,
                review = true,
                rating = review.authorDetails?.rating?.let { kotlin.math.round(it).toInt() }?.takeIf { it in 1..10 },
                createdAt = review.createdAt,
                updatedAt = review.updatedAt,
            )
        }
    }
}

@Serializable
private data class TmdbReviewsResponse(
    @SerialName("results") val results: List<TmdbReviewDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
private data class TmdbReviewDto(
    @SerialName("id") val id: String = "",
    @SerialName("author") val author: String? = null,
    @SerialName("author_details") val authorDetails: TmdbReviewAuthorDto? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class TmdbReviewAuthorDto(
    @SerialName("name") val name: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("rating") val rating: Double? = null,
)
