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

/**
 * Reviews shown under a film or a series: Trakt comments (public, with or without a Trakt
 * account) followed by TMDB reviews (no account needed). Later pages come from Trakt only.
 */
internal object MetaCommentsRepository {
    private val log = Logger.withTag("MetaComments")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getCommentsPage(meta: MetaDetails, page: Int = 1, forceRefresh: Boolean = false): TraktCommentsPage {
        val trakt = runCatching { TraktCommentsRepository.getCommentsPage(meta, page, forceRefresh) }
            .onFailure { log.w { "Trakt comments failed: ${it.message}" } }
            .getOrNull()
        if (page > 1) return trakt ?: TraktCommentsPage(emptyList(), page, 0, 0)
        val tmdb = runCatching { tmdbReviews(meta) }
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

    private suspend fun tmdbReviews(meta: MetaDetails): List<TraktCommentReview> {
        val mediaType = TmdbService.normalizeMediaType(meta.type)
        if (mediaType != "movie" && mediaType != "tv") return emptyList()
        val apiKey = TmdbSettingsRepository.effectiveApiKey().takeIf { it.isNotBlank() } ?: return emptyList()
        val tmdbId = TmdbService.ensureTmdbId(meta.id, mediaType, meta.imdbId) ?: return emptyList()
        // French reviews first; TMDB has few, so the English ones follow.
        val reviews = listOf("fr-FR", "en-US").flatMap { language ->
            val url = buildTmdbUrl("$mediaType/$tmdbId/reviews", apiKey, mapOf("language" to language, "page" to "1"))
            runCatching { json.decodeFromString<TmdbReviewsResponse>(httpGetText(url)).results }.getOrDefault(emptyList())
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
