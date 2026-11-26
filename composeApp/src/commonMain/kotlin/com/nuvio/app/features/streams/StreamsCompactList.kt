package com.nuvio.app.features.streams

import com.nuvio.app.core.ui.glass.isAmbientLiquidGlassActive
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Brush
import nuvio.composeapp.generated.resources.links_show_less
import nuvio.composeapp.generated.resources.links_show_more
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import nuvio.composeapp.generated.resources.links_current
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.i18n.localizedByteUnit
import com.nuvio.app.core.ui.Menu
import com.nuvio.app.core.ui.MenuItem
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.core.ui.glass.ambientLiquidGlass
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioPressScale
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import kotlin.math.round
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_play
import nuvio.composeapp.generated.resources.links_all
import nuvio.composeapp.generated.resources.links_change
import nuvio.composeapp.generated.resources.links_clear
import nuvio.composeapp.generated.resources.links_count
import nuvio.composeapp.generated.resources.links_favorites_first
import nuvio.composeapp.generated.resources.links_film_source_header
import nuvio.composeapp.generated.resources.links_hide_sources
import nuvio.composeapp.generated.resources.links_others
import nuvio.composeapp.generated.resources.links_other_sources
import nuvio.composeapp.generated.resources.links_count_one
import nuvio.composeapp.generated.resources.links_profile
import nuvio.composeapp.generated.resources.links_quality
import nuvio.composeapp.generated.resources.links_resume_at
import nuvio.composeapp.generated.resources.links_resume_percent
import nuvio.composeapp.generated.resources.links_source
import nuvio.composeapp.generated.resources.links_subtitles_list
import nuvio.composeapp.generated.resources.stream_lang_ar
import nuvio.composeapp.generated.resources.stream_lang_de
import nuvio.composeapp.generated.resources.stream_lang_en
import nuvio.composeapp.generated.resources.stream_lang_es
import nuvio.composeapp.generated.resources.stream_lang_fr
import nuvio.composeapp.generated.resources.stream_lang_hi
import nuvio.composeapp.generated.resources.stream_lang_it
import nuvio.composeapp.generated.resources.stream_lang_ja
import nuvio.composeapp.generated.resources.stream_lang_ko
import nuvio.composeapp.generated.resources.stream_lang_pt
import nuvio.composeapp.generated.resources.stream_lang_ru
import nuvio.composeapp.generated.resources.stream_lang_tr
import nuvio.composeapp.generated.resources.stream_lang_zh
import nuvio.composeapp.generated.resources.streams_checking_more_addons
import nuvio.composeapp.generated.resources.streams_copy_link
import nuvio.composeapp.generated.resources.streams_download_file
import nuvio.composeapp.generated.resources.streams_finding_streams
import nuvio.composeapp.generated.resources.streams_show_details
import nuvio.composeapp.generated.resources.streams_torrent_not_supported
import nuvio.composeapp.generated.resources.streams_type_debrid
import nuvio.composeapp.generated.resources.streams_type_direct
import nuvio.composeapp.generated.resources.streams_type_external
import nuvio.composeapp.generated.resources.streams_type_torrent
import nuvio.composeapp.generated.resources.streams_version_vost
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/*
 * Links screen of the phone: the source of the title first, filters for profile, quality and
 * source, then every link as « source ★ / quality · language / origin / badges ».
 */

/** A link with what the list sorts and filters it by, computed once per result change. */
internal data class CompactLink(
    val key: String,
    val stream: StreamItem,
    val resolution: String?,
    val qualityRank: Int,
    val version: String,
    val audioCode: String?,
    val info: StreamLanguageInfo,
    val favorite: Boolean,
    val dubRank: Int,
)

private val resolutionRanks = mapOf("4K" to 5, "1440p" to 4, "1080p" to 3, "720p" to 2, "480p" to 1)

/** Short version name of a link: VF, VOSTFR, MULTI, an audio language, VOST… or VO. */
internal fun StreamLanguageInfo.versionKey(): String = when {
    multi -> "MULTI"
    "fr" in audio -> "VF"
    audio.isNotEmpty() -> if (audio.size == 1) audio.first().uppercase() else "MULTI"
    subtitles.isNotEmpty() -> "VOST" + subtitles.sorted().first().uppercase()
    subbed && providerLanguage != null -> "VOST" + providerLanguage.uppercase()
    providerLanguage != null && !original && !subbed -> providerLanguage.uppercase()
    else -> "VO"
}

/** Every link of [groups] in the order the list shows them: favorites, dub preference, quality. */
internal fun compactLinks(
    groups: List<AddonStreamGroup>,
    pinnedSourceIds: List<String>,
    preferences: StreamLanguagePreferences,
): List<CompactLink> {
    val providerLanguages = cloudStreamProviderLanguages()
    val links = mutableListOf<CompactLink>()
    groups.forEachIndexed { groupIndex, group ->
        group.streams.forEachIndexed { index, stream ->
            val info = StreamLanguageDetector.detect(stream, providerLanguages[stream.addonId]).forPreferences(preferences)
            val resolution = StreamQualityDetector.detect(stream).resolution
            val sourceKey = PinnedStreamSourcesRepository.sourceKeyFor(stream)
            links += CompactLink(
                key = "$groupIndex:$index:${stream.addonId}:${stream.url ?: stream.infoHash ?: stream.externalUrl ?: stream.streamLabel}",
                stream = stream,
                resolution = resolution,
                qualityRank = resolutionRanks[resolution] ?: 0,
                version = info.versionKey(),
                audioCode = info.audio.singleOrNull(),
                info = info,
                favorite = pinnedSourceIds.any { it == stream.addonId || it == sourceKey },
                dubRank = info.preferenceRank(preferences.dubMode),
            )
        }
    }
    val dubMatters = preferences.dubMode != StreamDubMode.Any
    return links.sortedWith(
        compareBy<CompactLink>(
            { if (it.favorite) 0 else 1 },
            { if (dubMatters) it.dubRank else 0 },
            { -it.qualityRank },
        ),
    )
}

private val languageNames: Map<String, StringResource> = mapOf(
    "fr" to Res.string.stream_lang_fr, "en" to Res.string.stream_lang_en, "ar" to Res.string.stream_lang_ar,
    "es" to Res.string.stream_lang_es, "de" to Res.string.stream_lang_de, "it" to Res.string.stream_lang_it,
    "pt" to Res.string.stream_lang_pt, "ja" to Res.string.stream_lang_ja, "ko" to Res.string.stream_lang_ko,
    "zh" to Res.string.stream_lang_zh, "ru" to Res.string.stream_lang_ru, "tr" to Res.string.stream_lang_tr,
    "hi" to Res.string.stream_lang_hi,
)

/** Name of a language in the app language ("Arabe" for "ar"), or its code. */
@Composable
internal fun localizedLanguageName(code: String): String =
    languageNames[code.lowercase()]?.let { stringResource(it) } ?: code.uppercase()

/** A version as the user reads it: VF, VOSTFR, Multi, VO, « Arabe », « VOST anglais »… */
@Composable
internal fun versionDisplayName(version: String): String = when {
    version == "VF" || version == "VO" || version == "VOSTFR" -> version
    version == "MULTI" -> "Multi"
    version.startsWith("VOST") -> stringResource(Res.string.streams_version_vost, localizedLanguageName(version.removePrefix("VOST")).lowercase())
    else -> localizedLanguageName(version)
}

/**
 * The phone list. [header] is the episode or movie block that scrolls away; [onOpenProfiles] opens
 * « Profils ». The link launched last for the title stays first (« Source du film ») and
 * « Reprendre » uses it.
 */
@Composable
internal fun CompactStreamList(
    uiState: StreamsUiState,
    searchQuery: String,
    showSearchField: Boolean,
    debridEnabled: Boolean,
    resumePositionMs: Long?,
    resumeProgressFraction: Float?,
    onStreamSelected: (stream: StreamItem, resumePositionMs: Long?, resumeProgressFraction: Float?) -> Unit,
    onMoreActions: (StreamItem) -> Unit,
    onDownload: (StreamItem) -> Unit,
    onCopyLink: (StreamItem) -> Unit,
    onOpenProfiles: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
) {
    val pinnedSourceIds by rememberPinnedStreamSourceIds()
    val preferences by StreamLanguagePreferencesRepository.preferences.collectAsStateWithLifecycle()
    val activeProfile by StreamProfileRepository.active.collectAsStateWithLifecycle()
    val favoritesOnly by StreamSearchScopeRepository.favoritesOnly.collectAsStateWithLifecycle()
    val titleKey = StreamProfileRepository.currentTitleKey
    var filmSourceRevision by remember { mutableStateOf(0) }
    val searched = rememberStreamSearchResult(uiState.copy(selectedFilter = null), searchQuery, showSearchField)
    val allLinks = remember(searched.uiState.groups, pinnedSourceIds, preferences) {
        compactLinks(searched.uiState.groups, pinnedSourceIds, preferences)
    }
    var quality by rememberSaveable { mutableStateOf<String?>(null) }
    var source by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }

    val qualities = remember(allLinks) {
        allLinks.mapNotNull { it.resolution }.groupingBy { it }.eachCount().entries
            .sortedByDescending { resolutionRanks[it.key] ?: 0 }.map { it.key to it.value }
    }
    val sources = remember(allLinks) {
        allLinks.groupingBy { it.stream.addonId to it.stream.addonName }.eachCount().entries
            .sortedByDescending { it.value }.map { Triple(it.key.first, it.key.second, it.value) }
    }
    if (quality != null && qualities.none { it.first == quality }) quality = null
    if (source != null && sources.none { it.first == source }) source = null
    val filtering = quality != null || source != null
    val filmSource = remember(allLinks, titleKey, filmSourceRevision) {
        FilmSourceRepository.find(titleKey, allLinks) { it.stream }
    }
    val shown = allLinks.filter { link ->
        (quality == null || link.resolution == quality) && (source == null || link.stream.addonId == source)
    }
    // Without filters the source of the title has its own card; with them it stays in the list, marked.
    val listLinks = if (filtering || filmSource == null) shown else shown.filter { it !== filmSource }
    val anyLoading = uiState.groups.any { it.isLoading }
    val hasAnyStreams = uiState.groups.any { it.streams.isNotEmpty() }
    val sharedStream = LocalSharedStream.current
    val torrentNotSupportedText = stringResource(Res.string.streams_torrent_not_supported)
    val all = stringResource(Res.string.links_all)

    val play: (StreamItem) -> Unit = { stream ->
        val selectable = stream.isSelectableForPlayback(debridEnabled)
        val unsupportedTorrent = stream.needsLocalDebridResolve && !AppFeaturePolicy.p2pEnabled &&
            !(debridEnabled && stream.isAddonDebridCandidate)
        when {
            selectable -> {
                // The link launched becomes the source of the title.
                FilmSourceRepository.remember(titleKey, stream)
                filmSourceRevision++
                onStreamSelected(stream, resumePositionMs, resumeProgressFraction)
            }
            unsupportedTorrent -> NuvioToastController.show(torrentNotSupportedText)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = nuvioSafeBottomPadding(if (showSearchField) 96.dp else 24.dp)),
    ) {
        item(key = "hero") { header() }

        val resumeTarget = filmSource ?: shown.firstOrNull { it.stream.isSelectableForPlayback(debridEnabled) }
        val hasResume = (resumePositionMs != null && resumePositionMs > 0L) ||
            (resumeProgressFraction != null && resumeProgressFraction > 0f)
        if (resumeTarget != null && (hasResume || filmSource != null)) {
            item(key = "resume") {
                PlayButton(
                    title = when {
                        resumeProgressFraction != null && resumeProgressFraction > 0f ->
                            stringResource(Res.string.links_resume_percent, (resumeProgressFraction * 100f).toInt())
                        resumePositionMs != null && resumePositionMs > 0L ->
                            stringResource(Res.string.links_resume_at, resumePositionMs.toPlaybackClock())
                        else -> stringResource(Res.string.action_play)
                    },
                    subtitle = buildString {
                        append(resumeTarget.stream.addonName)
                        resumeTarget.resolution?.let { append(" · ").append(it) }
                        append(" · ").append(versionShort(resumeTarget.version))
                    },
                    progress = resumeProgressFraction,
                    onClick = { play(resumeTarget.stream) },
                )
            }
        }

        // Profil, qualité, source.
        item(key = "filters") {
            // Works like the navigation bar: one glass track, and a glass pill that slides with a
            // spring (and stretches a little on the way) to the segment touched, which then opens.
            var selectedSegment by rememberSaveable { mutableIntStateOf(0) }
            val segmentBounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
            val target = segmentBounds[selectedSegment]
            val pillX by animateFloatAsState(
                targetValue = target?.first ?: 0f,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
                label = "filter_pill_x",
            )
            val pillWidth by animateFloatAsState(
                targetValue = target?.second ?: 0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 260f),
                label = "filter_pill_width",
            )
            val density = LocalDensity.current
            val pill = RoundedCornerShape(percent = 50)
            fun Modifier.segment(index: Int): Modifier = onGloballyPositioned {
                val bounds = it.positionInParent().x to it.size.width.toFloat()
                if (segmentBounds[index] != bounds) segmentBounds[index] = bounds
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(50.dp)
                    .linksSurface(pill)
                    .padding(5.dp),
            ) {
                if (target != null && pillWidth > 0f) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(pillX.roundToInt(), 0) }
                            .width(with(density) { pillWidth.toDp() })
                            .fillMaxHeight()
                            .clip(pill)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.10f)),
                                ),
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.20f), pill),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterPill(
                        label = stringResource(Res.string.links_profile),
                        value = activeProfile.displayName(),
                        highlighted = false,
                        onClick = {
                            selectedSegment = 0
                            onOpenProfiles()
                        },
                        // The profile name (« Français ») is the longest: it gets more room.
                        modifier = Modifier.weight(1.4f).segment(0),
                    )
                    MenuFilterPill(
                        label = stringResource(Res.string.links_quality),
                        // Without a filter the pill names what it filters (« Qualité »), not « Toutes ».
                        value = quality ?: stringResource(Res.string.links_quality),
                        highlighted = quality != null,
                        options = listOf(null to all) + qualities.map { (key, count) -> key to "$key ($count)" },
                        selected = quality,
                        onOpen = { selectedSegment = 1 },
                        onSelect = { quality = it; expandedKey = null },
                        modifier = Modifier.weight(1f).segment(1),
                    )
                    MenuFilterPill(
                        label = stringResource(Res.string.links_source),
                        value = sources.firstOrNull { it.first == source }?.second ?: stringResource(Res.string.links_source),
                        highlighted = source != null,
                        options = listOf(null to all) + sources.map { (id, name, count) -> id to "$name ($count)" },
                        selected = source,
                        onOpen = { selectedSegment = 2 },
                        onSelect = { source = it; expandedKey = null },
                        modifier = Modifier.weight(1f).segment(2),
                    )
                }
            }
        }

        sharedStream?.let { shared ->
            item(key = "shared") {
                LinkGroup {
                    LinkRow(
                        stream = shared.stream,
                        isFilmSource = false,
                        tag = shared.label,
                        expanded = false,
                        onPlay = { onStreamSelected(shared.stream, resumePositionMs, resumeProgressFraction) },
                        onToggle = null,
                        onLongPress = null,
                        onDownload = onDownload,
                        onCopyLink = onCopyLink,
                    )
                }
            }
        }

        when {
            anyLoading && !hasAnyStreams -> item(key = "loading") {
                LoadingStateBlock(
                    findingStreamsText = stringResource(Res.string.streams_finding_streams),
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }
            !hasAnyStreams && !uiState.isAnyLoading -> if (sharedStream == null) {
                item(key = "empty") {
                    EmptyStateBlock(
                        reason = uiState.emptyStateReason,
                        details = uiState.groups
                            .mapNotNull { group -> group.error?.takeIf(String::isNotBlank)?.let { "${group.addonName} : $it" } }
                            .distinct()
                            .take(EMPTY_STATE_MAX_ERRORS)
                            .joinToString("\n")
                            .takeIf(String::isNotEmpty),
                    )
                }
            }
            searched.isEmptyBecauseOfSearch -> item(key = "search_empty") { StreamSearchEmptyBlock() }
            else -> {
                if (!filtering && filmSource != null) {
                    item(key = "film_source") {
                        SectionHeader(
                            title = stringResource(Res.string.links_film_source_header),
                            action = stringResource(Res.string.links_change),
                            onAction = {
                                FilmSourceRepository.forget(titleKey)
                                filmSourceRevision++
                            },
                        )
                        // One card: the source's name, its server and badges, Download and Play.
                        LinkGroup {
                            LinkRow(
                                stream = filmSource.stream,
                                isFilmSource = false,
                                headerName = filmSource.stream.addonName,
                                playFilled = true,
                                expanded = expandedKey == filmSource.key,
                                onPlay = { play(filmSource.stream) },
                                onToggle = { expandedKey = if (expandedKey == filmSource.key) null else filmSource.key },
                                onLongPress = { onMoreActions(filmSource.stream) },
                                onDownload = onDownload,
                                onCopyLink = onCopyLink,
                            )
                        }
                    }
                }
                if (listLinks.isNotEmpty()) {
                    item(key = "list_header") {
                        if (filtering) {
                            SectionHeader(
                                title = linksCountLabel(listLinks.size),
                                action = stringResource(Res.string.links_clear),
                                onAction = { quality = null; source = null },
                            )
                        } else {
                            val count = if (filmSource != null) {
                                stringResource(Res.string.links_other_sources, listLinks.size)
                            } else {
                                linksCountLabel(listLinks.size)
                            }
                            val parts = buildList {
                                if (activeProfile.hasPreferences) {
                                    add(stringResource(preferences.dubMode.labelRes))
                                    if (pinnedSourceIds.isNotEmpty()) {
                                        add(
                                            if (favoritesOnly) {
                                                stringResource(Res.string.links_hide_sources)
                                            } else {
                                                stringResource(Res.string.links_favorites_first)
                                            },
                                        )
                                    }
                                }
                            }
                            SectionHeader(title = count, trailing = parts.joinToString(" · ").takeIf { it.isNotEmpty() })
                        }
                    }
                    val bySource = listLinks.groupBy { it.stream.addonId }.entries.toList()
                    // All the other sources in one glass card, split by hairlines.
                    item(key = "sources") {
                        LinkGroup {
                          bySource.forEachIndexed { index, (addonId, links) ->
                           androidx.compose.runtime.key(addonId) {
                            SourceCard(
                                name = links.first().stream.addonName,
                                addonId = addonId,
                                isFirst = index == 0,
                                isLast = index == bySource.lastIndex,
                                links = links,
                                filmSource = filmSource,
                                expandedKey = expandedKey,
                                onToggle = { key -> expandedKey = if (expandedKey == key) null else key },
                                play = play,
                                onMoreActions = onMoreActions,
                                onDownload = onDownload,
                                onCopyLink = onCopyLink,
                            )
                           }
                          }
                        }
                    }
                }
                if (anyLoading) {
                    item(key = "loading_more") {
                        FooterLoadingBlock(checkingMoreAddonsText = stringResource(Res.string.streams_checking_more_addons))
                    }
                }
            }
        }
    }
}

/** « 1080p VF » in the resume button. */
private fun versionShort(version: String): String = if (version == "MULTI") "Multi" else version

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // « SOURCE ACTUELLE », « 10 AUTRES SOURCES »: small capitals, as on iOS.
        // The count never gets cut: the trailing info (« Doublé d'abord · … ») gives way first.
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color = Color.White.copy(alpha = 0.62f),
            maxLines = 1,
            softWrap = false,
            modifier = if (trailing == null) Modifier.weight(1f) else Modifier,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
        }
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.nuvio.colors.accent,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

/**
 * Surface of the links screen. Liquid glass theme: dark frosted glass (dark enough to read over any
 * artwork) with a light edge brighter at the top, as on iOS. AMOLED theme: pure black with a hairline.
 * [accent] tints it (the Profil pill, a filter in use).
 */
@Composable
internal fun Modifier.linksSurface(shape: Shape, accent: Color? = null): Modifier =
    if (isAmbientLiquidGlassActive()) {
        val base = Color(0xFF0E0E12).copy(alpha = 0.66f)
        val tint = accent?.copy(alpha = 0.45f)?.compositeOver(base) ?: base
        clip(shape)
            .ambientLiquidGlass(
                shape = shape,
                tint = tint,
                fallback = Color(0xFF0E0E12).copy(alpha = 0.94f),
                blurRadius = 12.dp,
                refractionHeight = 8.dp,
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        (accent ?: Color.White).copy(alpha = if (accent != null) 0.65f else 0.30f),
                        Color.White.copy(alpha = 0.06f),
                    ),
                ),
                shape = shape,
            )
    } else {
        clip(shape)
            .background(accent?.copy(alpha = 0.32f)?.compositeOver(Color.Black) ?: Color.Black)
            .border(1.dp, accent?.copy(alpha = 0.55f) ?: Color.White.copy(alpha = 0.12f), shape)
    }

/** A card holding rows of links. */
@Composable
private fun LinkGroup(isFirst: Boolean = true, isLast: Boolean = true, content: @Composable () -> Unit) {
    // Consecutive groups (first … last) read as one card split by hairlines.
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 28.dp else 0.dp,
        topEnd = if (isFirst) 28.dp else 0.dp,
        bottomStart = if (isLast) 28.dp else 0.dp,
        bottomEnd = if (isLast) 28.dp else 0.dp,
    )
    Column(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .padding(top = if (isFirst) 6.dp else 0.dp, bottom = if (isLast) 6.dp else 0.dp)
            .fillMaxWidth()
            .linksSurface(shape)
            .animateContentSize(),
    ) {
        content()
    }
}

@Composable
private fun LinkDivider() {
    Box(Modifier.padding(start = 20.dp).fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.10f)))
}

/** A source, folded: « NoTorrent · 28 liens · ♥ · ⌄ ». The arrow (or the header) shows its links. */
@Composable
private fun SourceCard(
    name: String,
    addonId: String,
    isFirst: Boolean,
    isLast: Boolean,
    links: List<CompactLink>,
    filmSource: CompactLink?,
    expandedKey: String?,
    onToggle: (String) -> Unit,
    play: (StreamItem) -> Unit,
    onMoreActions: (StreamItem) -> Unit,
    onDownload: (StreamItem) -> Unit,
    onCopyLink: (StreamItem) -> Unit,
) {
    var open by rememberSaveable(addonId) { mutableStateOf(false) }
    Column(modifier = Modifier.animateContentSize()) {
        if (!isFirst) LinkDivider()
        SourceHeader(name = name, addonId = addonId, count = links.size, open = open, onToggleOpen = { open = !open })
        AnimatedVisibility(visible = open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                links.forEach { link ->
                    LinkDivider()
                    LinkRow(
                        stream = link.stream,
                        isFilmSource = link === filmSource,
                        expanded = expandedKey == link.key,
                        onPlay = { play(link.stream) },
                        onToggle = { onToggle(link.key) },
                        onLongPress = { onMoreActions(link.stream) },
                        onDownload = onDownload,
                        onCopyLink = onCopyLink,
                    )
                }
            }
        }
    }
}

/** « Movix   8 liens ......... ♥ ⌄ »: the heart and the arrow side by side, the same size. */
@Composable
private fun SourceHeader(
    name: String,
    addonId: String,
    count: Int?,
    open: Boolean? = null,
    onToggleOpen: (() -> Unit)? = null,
) {
    val rotation by animateFloatAsState(if (open == true) 180f else 0f, spring(dampingRatio = 0.7f), label = "source_arrow")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onToggleOpen != null) Modifier.clickable(onClick = onToggleOpen) else Modifier)
            .padding(start = 20.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (count != null) {
                Text(
                    text = linksCountLabel(count),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.62f),
                    maxLines = 1,
                )
            }
        }
        FavoriteHeartButton(addonId = addonId, size = 44.dp)
        if (onToggleOpen != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .nuvioPressScale(pressedScale = 0.88f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                    .clickable(onClick = onToggleOpen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(Res.string.streams_show_details),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = rotation },
                )
            }
        }
    }
}

/**
 * A link row: « Serveur 1 » (« · en cours » for the link last launched, with a red edge on the left),
 * coloured badges (quality, language, format, size), then round Download and Play buttons.
 * Tapping the row plays it; a long press opens the other actions (copy the link…).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun LinkRow(
    stream: StreamItem,
    isFilmSource: Boolean,
    expanded: Boolean,
    onPlay: () -> Unit,
    onToggle: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
    onDownload: (StreamItem) -> Unit,
    onCopyLink: (StreamItem) -> Unit,
    tag: String? = null,
    showDownload: Boolean = true,
    enabled: Boolean = true,
    /** Source name in bold above the server line (the « Source actuelle » card). */
    headerName: String? = null,
    playFilled: Boolean = false,
) {
    val tokens = MaterialTheme.nuvio
    val info = remember(stream) { StreamLanguageDetector.detect(stream, cloudStreamProviderLanguages()[stream.addonId]) }
    val resolution = remember(stream) { StreamQualityDetector.detect(stream).resolution }
    val namedBadges = remember(stream) {
        StreamTechBadges.detect(stream).filter {
            it.kind == TechBadgeKind.Hdr || it.kind == TechBadgeKind.Audio || it.kind == TechBadgeKind.Release
        }
    }
    // When the name says nothing about picture or sound, the HLS playlist is read (HDR, Dolby
    // Vision, Atmos, DD+).
    var playlistBadges by remember(stream.url) { mutableStateOf<List<TechBadge>>(emptyList()) }
    LaunchedEffect(stream.url) {
        if (namedBadges.none { it.kind == TechBadgeKind.Hdr || it.kind == TechBadgeKind.Audio } && HlsTechProbe.isHls(stream)) {
            playlistBadges = HlsTechProbe.probe(stream)
        }
    }
    val techBadges = (namedBadges + playlistBadges.filter { it.kind != TechBadgeKind.Resolution })
        .distinctBy { it.text }
    val origin = stream.sourceName?.trim()?.takeIf { it.isNotEmpty() && !it.equals(stream.addonName, ignoreCase = true) }
        ?: stream.cleanDisplayName().takeIf { !it.equals(stream.addonName, ignoreCase = true) }
        ?: stream.addonName
    val currentLabel = stringResource(Res.string.links_current)
    val edgeColor = tokens.colors.accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (isFilmSource) drawRect(edgeColor, size = Size(3.dp.toPx(), size.height))
            }
            .alpha(if (enabled) 1f else 0.45f)
            .combinedClickable(enabled = enabled, onClick = onPlay, onLongClick = onLongPress),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (headerName != null) {
                    Text(
                        text = headerName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = when {
                            headerName != null -> listOfNotNull(origin.takeIf { it != headerName }, resolution).joinToString(" · ")
                            isFilmSource -> "$origin · $currentLabel"
                            else -> origin
                        },
                        style = if (headerName != null) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                        color = if (headerName != null) Color.White.copy(alpha = 0.62f) else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (tag != null) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(tokens.colors.accent)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Fusion badge images (imported badge packs) replace the text quality badge, so the
                    // quality is not shown twice; without them the text badge stays.
                    val badgeImages = stream.badges.filter { it.imageURL.isNotBlank() }
                    if (badgeImages.isNotEmpty()) {
                        badgeImages.forEach { StreamBadgeImage(badge = it) }
                    } else {
                        resolution?.let {
                            LinkBadge(it, if (it == "480p") LinkBadgeTone.Neutral else LinkBadgeTone.Quality)
                        }
                    }
                    LinkBadge(
                        listOfNotNull(info.versionFlag(), versionShort(info.versionKey())).joinToString(" "),
                        LinkBadgeTone.Language,
                    )
                    // Picture and sound (Dolby Vision, HDR, Atmos, DD+…) and the release (BluRay,
                    // WEB…), read from the link's name. The container (hls, mp4) is not shown.
                    techBadges.forEach { badge ->
                        LinkBadge(
                            badge.text,
                            when (badge.kind) {
                                TechBadgeKind.Hdr -> LinkBadgeTone.Hdr
                                TechBadgeKind.Audio -> LinkBadgeTone.Audio
                                else -> LinkBadgeTone.Neutral
                            },
                        )
                    }
                    stream.behaviorHints.videoSize?.let { LinkBadge(formatLinkSizeShort(it), LinkBadgeTone.Neutral) }
                }
            }
            Spacer(Modifier.width(10.dp))
            if (showDownload) {
                RoundLinkButton(
                    icon = Icons.Rounded.Download,
                    label = stringResource(Res.string.streams_download_file),
                    tint = Color.White,
                    container = Color.White.copy(alpha = 0.14f),
                    onClick = { onDownload(stream) },
                )
                Spacer(Modifier.width(10.dp))
            }
            RoundLinkButton(
                icon = Icons.Rounded.PlayArrow,
                label = stringResource(Res.string.action_play),
                tint = Color.White,
                container = if (playFilled) tokens.colors.accent else tokens.colors.accent.copy(alpha = 0.6f),
                onClick = onPlay,
            )
        }
    }
}

private enum class LinkBadgeTone { Quality, Language, Hdr, Audio, Neutral }

@Composable
private fun LinkBadge(text: String, tone: LinkBadgeTone) {
    val (fg, bg) = when (tone) {
        LinkBadgeTone.Quality -> Color(0xFFF2C46D) to Color(0xFF6B4F1D).copy(alpha = 0.55f)
        LinkBadgeTone.Language -> Color(0xFF9DB8FF) to Color(0xFF2F4F9E).copy(alpha = 0.50f)
        LinkBadgeTone.Hdr -> Color(0xFFE2B8FF) to Color(0xFF5B2E86).copy(alpha = 0.55f)
        LinkBadgeTone.Audio -> Color(0xFF8EE6DA) to Color(0xFF1F5E58).copy(alpha = 0.55f)
        LinkBadgeTone.Neutral -> Color.White.copy(alpha = 0.82f) to Color.White.copy(alpha = 0.10f)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun RoundLinkButton(icon: ImageVector, label: String, tint: Color, container: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .nuvioPressScale(pressedScale = 0.88f)
            .clip(CircleShape)
            .background(container)
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Container of the link read from its address or file name: mp4, mkv, hls, dash… */
private fun linkFormat(stream: StreamItem): String? {
    val candidates = listOfNotNull(stream.behaviorHints.filename, stream.url)
    for (raw in candidates) {
        val path = raw.substringBefore('?').substringBefore('#').lowercase()
        when {
            path.endsWith(".m3u8") || "/hls" in path -> return "hls"
            path.endsWith(".mpd") -> return "dash"
            path.endsWith(".mp4") -> return "mp4"
            path.endsWith(".mkv") -> return "mkv"
            path.endsWith(".webm") -> return "webm"
            path.endsWith(".avi") -> return "avi"
        }
    }
    return null
}

/** « 2,1 Go » / « 680 Mo », one decimal for gigabytes. */
private fun formatLinkSizeShort(bytes: Long): String {
    val gbUnit = localizedByteUnit("GB")
    val separator = if (gbUnit == "Go") "," else "."
    val gib = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return if (gib >= 1.0) {
        val tenths = round(gib * 10.0).toLong()
        "${tenths / 10}$separator${tenths % 10} $gbUnit"
    } else {
        "${round(bytes.toDouble() / (1024.0 * 1024.0)).toInt()} ${localizedByteUnit("MB")}"
    }
}

@Composable
private fun LinkDetails(stream: StreamItem, onPlay: () -> Unit, onDownload: () -> Unit, onCopyLink: () -> Unit) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        stream.behaviorHints.videoSize?.let { size -> DetailLine(Icons.Rounded.Download, formatLinkSize(size)) }
        val subtitles = stream.externalSubtitles.map { it.language }.filter { it.isNotBlank() }.distinct()
        if (subtitles.isNotEmpty()) {
            val names = subtitles.take(3).map { localizedLanguageName(it) }
            val text = names.joinToString(", ") + if (subtitles.size > 3) " +${subtitles.size - 3}" else ""
            DetailLine(Icons.Rounded.ClosedCaption, stringResource(Res.string.links_subtitles_list, text))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .nuvioPressScale(pressedScale = 0.97f)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(tokens.colors.accent)
                    .clickable(onClick = onPlay),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = tokens.colors.onAccent, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(Res.string.action_play),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.colors.onAccent,
                )
            }
            CircleAction(Icons.Rounded.Download, stringResource(Res.string.streams_download_file), onDownload)
            CircleAction(Icons.Rounded.Link, stringResource(Res.string.streams_copy_link), onCopyLink)
        }
    }
}

@Composable
private fun DetailLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CircleAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .nuvioPressScale(pressedScale = 0.9f)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun linkTypeLabel(stream: StreamItem): String = when {
    stream.isDirectDebridStream || stream.isCachedDebridTorrentStream -> stringResource(Res.string.streams_type_debrid)
    stream.isTorrentStream -> stringResource(Res.string.streams_type_torrent)
    stream.shouldOpenExternally -> stringResource(Res.string.streams_type_external)
    else -> stringResource(Res.string.streams_type_direct)
}

private fun formatLinkSize(bytes: Long): String {
    val gib = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return if (gib >= 1.0) {
        "${round(gib * 100.0) / 100.0} ${localizedByteUnit("GB")}"
    } else {
        "${round(bytes.toDouble() / (1024.0 * 1024.0)).toInt()} ${localizedByteUnit("MB")}"
    }
}

/**
 * « Reprendre à 10:58 » over « CNCVerse Bridge · 1080p VF »: a red pill with a soft glow, and
 * under it how far the episode was watched.
 */
@Composable
private fun PlayButton(title: String, subtitle: String, progress: Float? = null, onClick: () -> Unit) {
    val tokens = MaterialTheme.nuvio
    val accent = tokens.colors.accent
    val pill = RoundedCornerShape(percent = 50)
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .nuvioPressScale(pressedScale = 0.97f)
                .shadow(elevation = 18.dp, shape = pill, ambientColor = accent, spotColor = accent)
                .clip(pill)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.16f).compositeOver(accent), lerp(accent, Color.Black, 0.18f)),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.20f), pill)
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(8.dp))
            // Title and source on two tight lines, no extra height.
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (progress != null && progress > 0f) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(pill)
                    .background(Color.White.copy(alpha = 0.14f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(pill)
                        .background(accent),
                )
            }
        }
    }
}

/**
 * « Français ⇕ »: a glass pill with only its value (the label is read by screen readers).
 * The profile pill, and a filter in use, are filled with the accent colour.
 */
@Composable
private fun FilterPill(
    label: String,
    value: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .fillMaxHeight()
            .nuvioPressScale(pressedScale = 0.96f)
            .clip(shape)
            // The sliding pill behind marks the segment touched; a filter in use gets a dot.
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$label : $value" }
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (highlighted) {
            Spacer(Modifier.width(5.dp))
            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.nuvio.colors.accent))
        }
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun MenuFilterPill(
    label: String,
    value: String,
    highlighted: Boolean,
    options: List<Pair<String?, String>>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxHeight()) {
        FilterPill(
            label = label,
            value = value,
            highlighted = highlighted,
            onClick = {
                onOpen()
                expanded = true
            },
        )
        Menu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, text) ->
                MenuItem(text = text, selected = key == selected, onClick = { onSelect(key); expanded = false })
            }
        }
    }
}


/**
 * The links of one source, as on the links screen before playback, for the player's « Sources »
 * panel: header « LooStream · 4 liens · ♥ », then « Serveur » rows with their badges; the link
 * playing has the red edge and « en cours ».
 */
@Composable
internal fun PlayerSourceGroupCard(
    name: String,
    addonId: String,
    streams: List<StreamItem>,
    isCurrent: (StreamItem) -> Boolean,
    isEnabled: (StreamItem) -> Boolean,
    onPlay: (StreamItem) -> Unit,
) {
    LinkGroup {
        SourceHeader(name = name, addonId = addonId, count = streams.size)
        streams.forEach { stream ->
            LinkDivider()
            LinkRow(
                stream = stream,
                isFilmSource = isCurrent(stream),
                expanded = false,
                onPlay = { onPlay(stream) },
                onToggle = null,
                onLongPress = null,
                onDownload = {},
                onCopyLink = {},
                showDownload = false,
                enabled = isEnabled(stream),
            )
        }
    }
}


/** « 1 lien », « 7 liens ». */
@Composable
private fun linksCountLabel(count: Int): String =
    if (count == 1) stringResource(Res.string.links_count_one, count) else stringResource(Res.string.links_count, count)
