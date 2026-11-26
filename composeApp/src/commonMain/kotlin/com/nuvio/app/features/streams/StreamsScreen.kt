package com.nuvio.app.features.streams

import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import com.nuvio.app.core.ui.nuvioPressScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.ui.graphics.graphicsLayer
import nuvio.composeapp.generated.resources.streams_refresh
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import nuvio.composeapp.generated.resources.streams_hero_episode_overline
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.ui.NuvioBackButton
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioBottomSheetActionRow
import com.nuvio.app.core.ui.NuvioBottomSheetDivider
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.features.downloads.DownloadsRepository
import com.nuvio.app.features.details.MetaScreenSettingsRepository
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.core.ui.shimmer
import com.nuvio.app.core.ui.LocalVerticalBarPadding
import com.nuvio.app.core.ui.centeredVerticalBarPadding
import com.nuvio.app.features.debrid.DebridSettingsRepository
import com.nuvio.app.features.debrid.DirectDebridPlayableResult
import com.nuvio.app.features.debrid.DirectDebridPlaybackResolver
import com.nuvio.app.features.debrid.toastMessage
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watched.watchedItemKeys
import com.nuvio.app.navigation.LocalUseNativeNavigation
import com.nuvio.app.navigation.LocalNativeNavigationBarHidden
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import nuvio.composeapp.generated.resources.streams_show_fewer_links
import nuvio.composeapp.generated.resources.streams_show_more_links
import com.nuvio.app.core.ui.glass.LocalLiquidGlassBackdrop
import com.nuvio.app.core.ui.glass.ambientLiquidGlass
import com.nuvio.app.core.ui.glass.isAmbientLiquidGlassActive
import com.nuvio.app.core.ui.NuvioTokens
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.compositeOver
import com.nuvio.app.core.ui.glass.nuvioBackdropSource
import com.nuvio.app.core.ui.glass.rememberNuvioBackdrop

// ---------------------------------------------------------------------------
// Streams Screen
// ---------------------------------------------------------------------------

@Composable
fun StreamsScreen(
    type: String,
    videoId: String,
    parentMetaId: String,
    parentMetaType: String,
    title: String,
    logo: String? = null,
    poster: String? = null,
    background: String? = null,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
    episodeTitle: String? = null,
    episodeThumbnail: String? = null,
    resumePositionMs: Long? = null,
    resumeProgressFraction: Float? = null,
    manualSelection: Boolean = false,
    startFromBeginning: Boolean = false,
    showLoadingScreen: Boolean = false,
    downloadMode: Boolean = false,
    onStreamSelected: (stream: StreamItem, resumePositionMs: Long?, resumeProgressFraction: Float?) -> Unit = { _, _, _ -> },
    onStreamActionOpen: (
        stream: StreamItem,
        openExternally: Boolean,
        resumePositionMs: Long?,
        resumeProgressFraction: Float?,
    ) -> Unit = { _, _, _, _ -> },
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        com.nuvio.app.core.ui.glass.LocalGlassSurface provides com.nuvio.app.core.ui.glass.GlassSurface.SOURCES,
    ) {
    val useNativeNavigation = LocalUseNativeNavigation.current
    val nativeNavigationBarHidden = LocalNativeNavigationBarHidden.current
    val uiState by StreamsRepository.uiState.collectAsStateWithLifecycle()
    val streamDisplaySettings by remember {
        StreamBadgeSettingsRepository.ensureLoaded()
        StreamBadgeSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val playerSettings by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val debridSettings by remember {
        DebridSettingsRepository.ensureLoaded()
        DebridSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchProgressUiState by remember {
        WatchProgressRepository.ensureLoaded()
        WatchProgressRepository.uiState
    }.collectAsStateWithLifecycle()
    val metaScreenSettings by remember {
        MetaScreenSettingsRepository.ensureLoaded()
        MetaScreenSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    remember {
        DownloadsRepository.ensureLoaded()
    }
    val isEpisode = seasonNumber != null && episodeNumber != null
    val clipboardManager = LocalClipboardManager.current
    val streamLinkCopiedText = stringResource(Res.string.streams_link_copied)
    val noDirectStreamLinkText = stringResource(Res.string.streams_no_direct_link)
    var streamActionsTarget by remember(videoId) { mutableStateOf<StreamItem?>(null) }
    var streamSearchQuery by rememberSaveable(videoId) { mutableStateOf("") }
    val downloadScope = rememberCoroutineScope()
    var preferredFilterApplied by remember(videoId) { mutableStateOf(false) }
    val episodeProgress = watchProgressUiState.progressForVideo(
        videoId = videoId,
        parentMetaId = parentMetaId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
    )
    val storedProgress = if (startFromBeginning) {
        null
    } else {
        episodeProgress
    }
    val resumeState = resolveStreamResumeState(
        progress = episodeProgress,
        initialPositionMs = resumePositionMs,
        initialProgressFraction = resumeProgressFraction,
        startFromBeginning = startFromBeginning,
    )
    val effectiveResumePositionMs = resumeState.positionMs
    val effectiveResumeProgressFraction = resumeState.progressFraction

    LaunchedEffect(type, videoId, seasonNumber, episodeNumber, manualSelection) {
        StreamsRepository.load(
            type = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            season = seasonNumber,
            episode = episodeNumber,
            manualSelection = manualSelection,
            parentMetaType = parentMetaType,
            searchTitle = title,
        )
    }

    LaunchedEffect(uiState.groups, storedProgress?.providerAddonId, preferredFilterApplied) {
        if (preferredFilterApplied) return@LaunchedEffect
        val preferredAddonId = storedProgress?.providerAddonId ?: return@LaunchedEffect
        if (uiState.groups.any { it.addonId == preferredAddonId }) {
            StreamsRepository.selectFilter(preferredAddonId)
            preferredFilterApplied = true
        }
    }

    val heroArtwork = if (isEpisode) {
        episodeThumbnail ?: background ?: poster
    } else {
        background ?: poster
    }
    val isEpisodeWatched = episodeProgress?.isEffectivelyCompleted == true || watchedItemKeys(
        type = parentMetaType,
        id = parentMetaId,
        season = seasonNumber,
        episode = episodeNumber,
    ).any(watchedUiState.watchedKeys::contains)
    val blurEpisodeThumbnail = metaScreenSettings.blurUnwatchedEpisodes &&
        isEpisode &&
        !isEpisodeWatched &&
        !episodeThumbnail.isNullOrBlank()
    val reloadStreams: () -> Unit = {
        StreamsRepository.reload(
            type = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            season = seasonNumber,
            episode = episodeNumber,
            manualSelection = manualSelection,
            parentMetaType = parentMetaType,
            searchTitle = title,
        )
    }

    val startStreamDownload: (StreamItem) -> Unit = { stream ->

            if (DirectDebridPlaybackResolver.shouldResolveToPlayableStream(stream)) {
                downloadScope.launch {
                    val resolved = DirectDebridPlaybackResolver.resolveToPlayableStream(
                        stream = stream,
                        season = seasonNumber,
                        episode = episodeNumber,
                    )
                    when (resolved) {
                        is DirectDebridPlayableResult.Success -> {
                            val result = DownloadsRepository.enqueueFromStream(
                                contentType = type,
                                videoId = videoId,
                                parentMetaId = parentMetaId,
                                parentMetaType = parentMetaType,
                                title = title,
                                logo = logo,
                                poster = poster,
                                background = background,
                                seasonNumber = seasonNumber,
                                episodeNumber = episodeNumber,
                                episodeTitle = episodeTitle,
                                episodeThumbnail = episodeThumbnail,
                                stream = resolved.stream,
                            )
                            NuvioToastController.show(result.toastMessage())
                        }
                        else -> {
                            val message = resolved.toastMessage()
                            if (message != null) {
                                NuvioToastController.show(message)
                            }
                        }
                    }
                }
            } else {
                val result = DownloadsRepository.enqueueFromStream(
                    contentType = type,
                    videoId = videoId,
                    parentMetaId = parentMetaId,
                    parentMetaType = parentMetaType,
                    title = title,
                    logo = logo,
                    poster = poster,
                    background = background,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    episodeTitle = episodeTitle,
                    episodeThumbnail = episodeThumbnail,
                    stream = stream,
                )
                NuvioToastController.show(result.toastMessage())
            }
    }

    val copyStreamLink: (StreamItem) -> Unit = { stream ->
        val directUrl = stream.playableDirectUrl ?: stream.externalOpenUrl
        if (!directUrl.isNullOrBlank()) {
            clipboardManager.setText(AnnotatedString(directUrl))
            NuvioToastController.show(streamLinkCopiedText)
        } else if (DirectDebridPlaybackResolver.shouldResolveToPlayableStream(stream)) {
            downloadScope.launch {
                val resolved = DirectDebridPlaybackResolver.resolveToPlayableStream(
                    stream = stream,
                    season = seasonNumber,
                    episode = episodeNumber,
                )
                when (resolved) {
                    is DirectDebridPlayableResult.Success -> {
                        val resolvedUrl = resolved.stream.playableDirectUrl
                        if (!resolvedUrl.isNullOrBlank()) {
                            clipboardManager.setText(AnnotatedString(resolvedUrl))
                            NuvioToastController.show(streamLinkCopiedText)
                        } else {
                            NuvioToastController.show(noDirectStreamLinkText)
                        }
                    }
                    else -> {
                        val message = resolved.toastMessage()
                        if (message != null) {
                            NuvioToastController.show(message)
                        }
                    }
                }
            }
        } else {
            NuvioToastController.show(noDirectStreamLinkText)
        }
    }

    if (showLoadingScreen) return@CompositionLocalProvider

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val isTabletLayout = maxWidth >= 768.dp

        if (isTabletLayout) {
            TabletStreamsLayout(
                isEpisode = isEpisode,
                showSearchField = streamDisplaySettings.showStreamSearch,
                searchQuery = streamSearchQuery,
                onSearchQueryChange = { streamSearchQuery = it },
                title = title,
                logo = logo,
                poster = poster,
                background = background,
                episodeThumbnail = episodeThumbnail,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeTitle = episodeTitle,
                uiState = uiState,
                debridEnabled = debridSettings.canResolvePlayableLinks,
                appendInstantServiceToDefaultName = debridSettings.canResolvePlayableLinks && !debridSettings.hasCustomStreamFormatting,
                resumePositionMs = effectiveResumePositionMs,
                resumeProgressFraction = effectiveResumeProgressFraction,
                onStreamSelected = { stream, positionMs, progressFraction ->
                    if (downloadMode) {
                        startStreamDownload(stream)
                    } else {
                        onStreamSelected(stream, positionMs, progressFraction)
                    }
                },
                onStreamLongPress = { stream -> streamActionsTarget = stream },
                onRefresh = reloadStreams,
            )
        } else {
            MobileStreamsLayout(
                isEpisode = isEpisode,
                showSearchField = streamDisplaySettings.showStreamSearch,
                searchQuery = streamSearchQuery,
                onSearchQueryChange = { streamSearchQuery = it },
                backgroundMode = streamDisplaySettings.backgroundMode,
                title = title,
                logo = logo,
                heroArtwork = heroArtwork,
                backdrop = background ?: poster,
                genres = remember(parentMetaType, parentMetaId) {
                    com.nuvio.app.features.details.MetaDetailsRepository.peek(parentMetaType, parentMetaId)?.genres.orEmpty()
                },
                imdbRating = remember(parentMetaType, parentMetaId) {
                    com.nuvio.app.features.details.MetaDetailsRepository.peek(parentMetaType, parentMetaId)?.imdbRating
                },
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeTitle = episodeTitle,
                blurEpisodeThumbnail = blurEpisodeThumbnail,
                uiState = uiState,
                debridEnabled = debridSettings.canResolvePlayableLinks,
                resumePositionMs = effectiveResumePositionMs,
                resumeProgressFraction = effectiveResumeProgressFraction,
                onStreamSelected = { stream, positionMs, progressFraction ->
                    if (downloadMode) {
                        startStreamDownload(stream)
                    } else {
                        onStreamSelected(stream, positionMs, progressFraction)
                    }
                },
                onStreamLongPress = { stream -> streamActionsTarget = stream },
                onDownload = startStreamDownload,
                onCopyLink = copyStreamLink,
                onRefresh = reloadStreams,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(LocalVerticalBarPadding.current)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(
                    start = 12.dp,
                    top = if (useNativeNavigation && !nativeNavigationBarHidden) 52.dp else 8.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NuvioBackButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp),
                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                contentColor = MaterialTheme.colorScheme.onBackground,
            )
        }


        StreamActionsSheet(
            stream = streamActionsTarget,
            externalPlayerEnabled = playerSettings.externalPlayerEnabled,
            onDismiss = { streamActionsTarget = null },
            onCopyLink = copyStreamLink,
            onDownload = startStreamDownload,
            onOpen = { stream, openExternally ->
                onStreamActionOpen(
                    stream,
                    openExternally,
                    effectiveResumePositionMs,
                    effectiveResumeProgressFraction,
                )
            },
        )
    }
}
}

@Composable
private fun MobileStreamsLayout(
    isEpisode: Boolean,
    showSearchField: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    backgroundMode: StreamBackgroundMode,
    title: String,
    logo: String?,
    heroArtwork: String?,
    backdrop: String?,
    genres: List<String>,
    imdbRating: String? = null,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    blurEpisodeThumbnail: Boolean,
    uiState: StreamsUiState,
    debridEnabled: Boolean,
    resumePositionMs: Long?,
    resumeProgressFraction: Float?,
    onStreamSelected: (stream: StreamItem, resumePositionMs: Long?, resumeProgressFraction: Float?) -> Unit,
    onStreamLongPress: (StreamItem) -> Unit,
    onDownload: (StreamItem) -> Unit,
    onCopyLink: (StreamItem) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val streamsGlassBackdrop = rememberNuvioBackdrop()
    // The AMOLED black theme has no blurred artwork behind the links.
    // Follows the app Style, like the title page: Liquid glass gives the blurred artwork and glass
    // cards, Opaque gives plain dark cards.
    val glassStyle by com.nuvio.app.features.settings.ThemeSettingsRepository.liquidGlassEffectsEnabled.collectAsStateWithLifecycle()
    val cinematicBackground = heroArtwork != null && glassStyle && com.nuvio.app.core.ui.glass.isLiquidGlassAvailable() &&
        com.nuvio.app.core.ui.glass.GlassAppearance.isEnabledFor(com.nuvio.app.core.ui.glass.GlassSurface.SOURCES)
    val streamsGlassEnabled = cinematicBackground
    var showSearchSettings by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize()) {
        if (cinematicBackground) {
            AsyncImage(
                model = heroArtwork,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(10.dp)
                    .nuvioBackdropSource(streamsGlassBackdrop),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Light at the top, where the sharp artwork melts into it, darker toward the
                    // bottom so the links stay readable.
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                                0.35f to MaterialTheme.colorScheme.background.copy(alpha = 0.62f),
                                1.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.90f),
                            ),
                        ),
                    ),
            )
        }

        CompositionLocalProvider(
            LocalLiquidGlassBackdrop provides if (streamsGlassEnabled) streamsGlassBackdrop else null,
        ) {
            CompactStreamList(
                uiState = uiState,
                searchQuery = searchQuery,
                showSearchField = showSearchField,
                debridEnabled = debridEnabled,
                resumePositionMs = resumePositionMs,
                resumeProgressFraction = resumeProgressFraction,
                onStreamSelected = onStreamSelected,
                onMoreActions = onStreamLongPress,
                onDownload = onDownload,
                onCopyLink = onCopyLink,
                onOpenProfiles = { showSearchSettings = true },
            ) {
                // The same top as the title's page: its artwork, its logo, its genres.
                // The episode's own still first: the title's backdrop often has its rating and genres
                // printed on it, which doubled the chips below.
                LinksHero(
                    image = heroArtwork ?: backdrop,
                    blurImage = isEpisode && blurEpisodeThumbnail,
                    logo = logo,
                    title = title,
                    overline = if (isEpisode && seasonNumber != null && episodeNumber != null) {
                        stringResource(Res.string.streams_hero_episode_overline, seasonNumber, episodeNumber)
                    } else {
                        null
                    },
                    episodeTitle = episodeTitle?.takeIf { isEpisode && it.isNotBlank() },
                    imdbRating = imdbRating,
                    genres = genres,
                    fadeTo = if (cinematicBackground) Color.Transparent else MaterialTheme.colorScheme.background,
                )
            }

            // A soft shade under the status bar and the round buttons, so the list scrolling
            // under them (the resume button) does not collide with them.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                                0.55f to MaterialTheme.colorScheme.background.copy(alpha = 0.70f),
                                1.0f to Color.Transparent,
                            ),
                        ),
                    ),
            )
            // Same at the bottom behind the search field, so the list fades out under it.
            if (showSearchField) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 96.dp)
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.45f to MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                                    1.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                                ),
                            ),
                        ),
                )
            }

            // Search at the bottom, within reach of the thumb.
            if (showSearchField) {
                StreamSearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }

            // Refresh, top right, in a glass circle like the back button.
            RefreshGlassButton(
                isLoading = uiState.groups.any { it.isLoading },
                onClick = onRefresh,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(top = 8.dp, end = 12.dp),
            )
        }
    }

    if (showSearchSettings) {
        val canSearchOneSource = uiState.sourceOptions.size > 1 || StreamSearchScopeRepository.scope.value != null
        StreamSearchSheet(
            sourceOptions = uiState.sourceOptions,
            groups = uiState.groups,
            searchOptions = if (canSearchOneSource) uiState.sourceOptions else emptyList(),
            appliedScopeId = uiState.searchScopeId,
            onDismiss = { changed ->
                showSearchSettings = false
                if (changed) onRefresh()
            },
        )
    }
}

@Composable
private fun RefreshGlassButton(isLoading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val rotation by rememberInfiniteTransition(label = "refresh_spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "refresh_rotation",
    )
    Box(
        modifier = modifier
            .size(40.dp)
            .nuvioPressScale(pressedScale = 0.9f)
            .linksSurface(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = stringResource(Res.string.streams_refresh),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { rotationZ = if (isLoading) rotation else 0f },
        )
    }
}

internal data class StreamResumeState(
    val positionMs: Long? = null,
    val progressFraction: Float? = null,
)

internal fun resolveStreamResumeState(
    progress: WatchProgressEntry?,
    initialPositionMs: Long?,
    initialProgressFraction: Float?,
    startFromBeginning: Boolean,
): StreamResumeState {
    if (startFromBeginning || progress?.isResumable == false) return StreamResumeState()
    val fraction = (if (progress != null) progress.progressPercent?.div(100f) else initialProgressFraction)
        ?.takeIf { it > 0f }?.coerceIn(0f, 1f)
    val position = if (fraction != null) null
        else (progress?.lastPositionMs ?: initialPositionMs)?.takeIf { it > 0L }
    return StreamResumeState(positionMs = position, progressFraction = fraction)
}

@Composable
internal fun ResumeBanner(
    positionMs: Long?,
    progressFraction: Float? = null,
    modifier: Modifier = Modifier,
) {
    val resumeText = when {
        progressFraction != null && progressFraction > 0f -> stringResource(
            Res.string.streams_resume_from_percent,
            (progressFraction * 100f).roundToInt(),
        )
        positionMs != null && positionMs > 0L -> stringResource(
            Res.string.streams_resume_from_time,
            positionMs.toPlaybackClock(),
        )
        else -> null
    } ?: return

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .ambientLiquidGlass(
                shape = RoundedCornerShape(18.dp),
                tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                fallback = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = resumeText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Top of the links screen, as in the mockup: the artwork, then left-aligned « SAISON 1 · ÉPISODE 1 »,
 * the episode title (the title's logo or name for a film), the IMDb rating and the genres as chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinksHero(
    image: String?,
    logo: String?,
    title: String,
    overline: String?,
    episodeTitle: String?,
    imdbRating: String?,
    genres: List<String>,
    fadeTo: Color,
    blurImage: Boolean = false,
) {
    var logoFailed by remember(logo) { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
        if (image != null) {
            // The artwork itself fades out (alpha mask) over its lower half, so it melts into
            // what is behind it instead of ending on a visible line.
            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurImage) Modifier.blur(18.dp) else Modifier)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black,
                                    0.50f to Color.Black,
                                    0.80f to Color.Black.copy(alpha = 0.45f),
                                    1.0f to Color.Transparent,
                                ),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.30f),
                            0.22f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1.0f to fadeTo,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (overline != null) {
                Text(
                    text = overline.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                )
            }
            val logoUrl = logo?.takeIf { it.isNotBlank() && !logoFailed && episodeTitle == null }
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = title,
                    modifier = Modifier.height(84.dp).fillMaxWidth(0.72f),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    onError = { logoFailed = true },
                )
            } else {
                Text(
                    text = episodeTitle ?: title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (imdbRating != null || genres.isNotEmpty()) {
                // One line that scrolls sideways, instead of three lines of chips.
                Row(
                    modifier = Modifier.padding(top = 2.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    imdbRating?.takeIf { it.isNotBlank() && it != "0" && it != "N/A" }?.let { rating ->
                        Text(
                            text = "IMDb ${rating.replace('.', ',')}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color(0xFFF5C518))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    genres.take(3).forEach { genre ->
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(percent = 50))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Movie Hero
// ---------------------------------------------------------------------------

@Composable
private fun MovieHeroBlock(
    title: String,
    logo: String?,
    modifier: Modifier = Modifier,
) {
    var logoLoadError by remember(logo) { mutableStateOf(false) }
    val logoUrl = logo?.takeIf { it.isNotBlank() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(centeredVerticalBarPadding())
            .height(140.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        contentAlignment = Alignment.Center,
    ) {
        if (logoUrl != null && !logoLoadError) {
            AsyncImage(
                model = logoUrl,
                contentDescription = title,
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth(0.85f),
                contentScale = ContentScale.Fit,
                onError = { logoLoadError = true },
            )
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Episode Hero
// ---------------------------------------------------------------------------

@Composable
private fun EpisodeHeroBlock(
    seasonNumber: Int,
    episodeNumber: Int,
    episodeTitle: String,
    thumbnail: String?,
    blurred: Boolean,
    showTitle: String,
    modifier: Modifier = Modifier,
) {
    val heroBlendColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        // Thumbnail image
        if (thumbnail != null) {
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurred) Modifier.blur(18.dp) else Modifier),
                contentScale = ContentScale.Crop,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.1f)),
        )

        // Gradient overlay bottom-up
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.58f to Color.Transparent,
                            0.8f to Color.Black.copy(alpha = 0.42f),
                            0.93f to heroBlendColor.copy(alpha = 0.84f),
                            1.0f to heroBlendColor,
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                    ),
                ),
        )

        // Safe-area push-down for status bar, then content pinned to bottom
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(LocalVerticalBarPadding.current)
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Episode label
            Text(
                text = stringResource(Res.string.streams_episode_badge, seasonNumber, episodeNumber),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Episode title
            Text(
                text = episodeTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Show title
            Text(
                text = showTitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Stream List
// ---------------------------------------------------------------------------

@Composable
internal fun StreamList(
    uiState: StreamsUiState,
    debridEnabled: Boolean,
    appendInstantServiceToDefaultName: Boolean,
    onStreamSelected: (stream: StreamItem, resumePositionMs: Long?, resumeProgressFraction: Float?) -> Unit,
    onStreamLongPress: (StreamItem) -> Unit,
    resumePositionMs: Long?,
    resumeProgressFraction: Float?,
    modifier: Modifier = Modifier,
) {
    val filteredGroups = uiState.filteredGroups
    val hasGroups = filteredGroups.isNotEmpty()
    val hasAnyStreams = filteredGroups.any { it.streams.isNotEmpty() }
    val anyLoading = filteredGroups.any { it.isLoading }
    val torrentNotSupportedText = stringResource(Res.string.streams_torrent_not_supported)
    val fetchingText = stringResource(Res.string.streams_fetching)
    val findingStreamsText = stringResource(Res.string.streams_finding_streams)
    val checkingMoreAddonsText = stringResource(Res.string.streams_checking_more_addons)
    val formatStreamSize = rememberStreamSizeLabelFormat()
    val sharedStream = LocalSharedStream.current
    val streamBadgeSettings by remember {
        StreamBadgeSettingsRepository.ensureLoaded()
        StreamBadgeSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val pinnedSourceIds by rememberPinnedStreamSourceIds()
    val dubMode = remember { StreamLanguagePreferencesRepository.preferences }
        .collectAsStateWithLifecycle().value.dubMode
    val pinnedFirst = remember(filteredGroups, pinnedSourceIds, dubMode) {
        splitPinnedSources(filteredGroups.sortedByDubPreference(), pinnedSourceIds)
    }
    // Sources of each section, grouped and sorted once per change of the results instead of on
    // every pass of the list builder: the language detection behind the dubbed-first order reads
    // every link, and doing it while addons were still answering made the screen stutter.
    val sectionSources = remember(pinnedFirst, dubMode) {
        val dubFirst = dubMode != StreamDubMode.Any
        val providerLanguages = if (dubFirst) cloudStreamProviderLanguages() else emptyMap()
        StreamSectionSources(
            pinned = pinnedFirst.pinnedGroups.map { it.sourcesForDisplay(dubFirst, providerLanguages) },
            remaining = pinnedFirst.remainingGroups.map { it.sourcesForDisplay(dubFirst, providerLanguages) },
        )
    }

    // Sources with several links show their first one until their arrow is tapped.
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    CompositionLocalProvider(LocalStreamSizeLabelFormat provides formatStreamSize) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = 12.dp,
            vertical = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        sharedStream?.let { shared ->
            item(key = "shared_stream") {
                Column {
                    StreamSectionHeader(
                        group = AddonStreamGroup(
                            addonName = shared.label,
                            addonId = "party-shared-stream",
                            streams = emptyList(),
                        ),
                        fetchingText = fetchingText,
                    )
                    StreamCard(
                        stream = shared.stream,
                        enabled = true,
                        appendInstantServiceToDefaultName = false,
                        showFileSizeBadges = streamBadgeSettings.showFileSizeBadges,
                        showAddonLogo = streamBadgeSettings.showAddonLogo,
                        badgePlacement = streamBadgeSettings.badgePlacement,
                        onClick = { onStreamSelected(shared.stream, resumePositionMs, resumeProgressFraction) },
                        outlined = true,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
        when {
            hasGroups && anyLoading && !hasAnyStreams -> {
                item {
                    LoadingStateBlock(findingStreamsText = findingStreamsText)
                }
            }

            !hasAnyStreams && !uiState.isAnyLoading -> {
                if (sharedStream == null) {
                    item {
                        EmptyStateBlock(
                            reason = uiState.emptyStateReason,
                            // What each source answered, so a failure can be told apart from a missing title.
                            details = uiState.groups
                                .mapNotNull { group -> group.error?.takeIf(String::isNotBlank)?.let { "${group.addonName} : $it" } }
                                .distinct()
                                .take(EMPTY_STATE_MAX_ERRORS)
                                .joinToString("\n")
                                .takeIf(String::isNotEmpty),
                        )
                    }
                }
            }

            else -> {
                pinnedFirst.pinnedGroups.forEachIndexed { pinnedIndex, group ->
                    streamSection(
                        sectionKey = "pinned:" + streamSectionRenderKey(groupIndex = pinnedIndex, group = group),
                        group = group,
                        sources = sectionSources.pinned.getOrElse(pinnedIndex) { group.sourcesForDisplay(false, emptyMap()) },
                        showHeader = true,
                        expanded = expandedGroups["pinned:" + group.addonId] == true,
                        onToggleExpanded = {
                            val key = "pinned:" + group.addonId
                            expandedGroups[key] = expandedGroups[key] != true
                        },
                        debridEnabled = debridEnabled,
                        appendInstantServiceToDefaultName = appendInstantServiceToDefaultName,
                        showFileSizeBadges = streamBadgeSettings.showFileSizeBadges,
                        showAddonLogo = streamBadgeSettings.showAddonLogo,
                        badgePlacement = streamBadgeSettings.badgePlacement,
                        torrentNotSupportedText = torrentNotSupportedText,
                        fetchingText = fetchingText,
                        onStreamSelected = onStreamSelected,
                        onStreamLongPress = onStreamLongPress,
                        resumePositionMs = resumePositionMs,
                        resumeProgressFraction = resumeProgressFraction,
                    )
                }
                pinnedFirst.remainingGroups.forEachIndexed { groupIndex, group ->
                    streamSection(
                        sectionKey = streamSectionRenderKey(groupIndex = groupIndex, group = group),
                        group = group,
                        sources = sectionSources.remaining.getOrElse(groupIndex) { group.sourcesForDisplay(false, emptyMap()) },
                        showHeader = uiState.selectedFilter == null,
                        // A single source picked in "Afficher" shows all its links.
                        expanded = uiState.selectedFilter != null || expandedGroups[group.addonId] == true,
                        onToggleExpanded = { expandedGroups[group.addonId] = expandedGroups[group.addonId] != true },
                        debridEnabled = debridEnabled,
                        appendInstantServiceToDefaultName = appendInstantServiceToDefaultName,
                        showFileSizeBadges = streamBadgeSettings.showFileSizeBadges,
                        showAddonLogo = streamBadgeSettings.showAddonLogo,
                        badgePlacement = streamBadgeSettings.badgePlacement,
                        torrentNotSupportedText = torrentNotSupportedText,
                        fetchingText = fetchingText,
                        onStreamSelected = onStreamSelected,
                        onStreamLongPress = onStreamLongPress,
                        resumePositionMs = resumePositionMs,
                        resumeProgressFraction = resumeProgressFraction,
                    )
                }
                if (anyLoading) {
                    item {
                        FooterLoadingBlock(checkingMoreAddonsText = checkingMoreAddonsText)
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(nuvioSafeBottomPadding(80.dp)))
                }
            }
        }
    }
    }
}

/** Links of a section by source name, in display order. */
private class StreamSectionSources(
    val pinned: List<List<Pair<String, List<StreamItem>>>>,
    val remaining: List<List<Pair<String, List<StreamItem>>>>,
)

/**
 * Links of [this] by source name; with [dubFirst], sources with a dubbed or multi-language link
 * come first. Each source is ranked once (the comparator used to rank it on every comparison).
 */
private fun AddonStreamGroup.sourcesForDisplay(
    dubFirst: Boolean,
    providerLanguages: Map<String, String>,
): List<Pair<String, List<StreamItem>>> {
    val streamsBySource = streams.groupBy { stream ->
        stream.sourceName?.takeIf { it.isNotBlank() } ?: stream.addonName
    }
    val rankBySource = if (dubFirst) {
        streamsBySource.mapValues { (_, sourceStreams) -> sourceStreams.bestDubRank(providerLanguages) }
    } else {
        emptyMap()
    }
    return streamsBySource.keys
        .sortedWith(compareBy<String> { source -> rankBySource[source] ?: 0 }.thenBy { it.lowercase() })
        .map { source -> source to streamsBySource.getValue(source) }
}

private fun LazyListScope.streamSection(
    sectionKey: String,
    group: AddonStreamGroup,
    sources: List<Pair<String, List<StreamItem>>>,
    showHeader: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    debridEnabled: Boolean,
    appendInstantServiceToDefaultName: Boolean,
    showFileSizeBadges: Boolean,
    showAddonLogo: Boolean,
    badgePlacement: StreamBadgePlacement,
    torrentNotSupportedText: String,
    fetchingText: String,
    onStreamSelected: (stream: StreamItem, resumePositionMs: Long?, resumeProgressFraction: Float?) -> Unit,
    onStreamLongPress: (StreamItem) -> Unit,
    resumePositionMs: Long?,
    resumeProgressFraction: Float?,
) {
    if (group.streams.isEmpty() && !group.isLoading) return

    if (showHeader) {
        item(key = "header_$sectionKey") {
            StreamSectionHeader(
                group = group,
                fetchingText = fetchingText,
            )
        }
    }

    val totalLinks = sources.sumOf { it.second.size }
    val collapsible = totalLinks > COLLAPSED_LINK_COUNT
    // Collapsed: only the first links, without the per-server headers.
    val shownSources = if (collapsible && !expanded) {
        var left = COLLAPSED_LINK_COUNT
        sources.mapNotNull { (name, links) ->
            if (left <= 0) return@mapNotNull null
            val kept = links.take(left)
            left -= kept.size
            name to kept
        }
    } else {
        sources
    }
    val showSourceHeaders = sources.size > 1 && (expanded || !collapsible)

    shownSources.forEachIndexed { sourceIndex, (sourceName, sourceStreams) ->
        // A source with a single link needs no header: its card already shows the name. The
        // favorite star is only on the group header, for the whole source.
        if (showSourceHeaders && sourceStreams.size > 1) {
            item(key = "source_${sectionKey}_$sourceIndex") {
                StreamSourceHeader(sourceName = sourceName)
            }
        }

        itemsIndexed(
            items = sourceStreams,
            key = { index, stream ->
                streamCardRenderKey(
                    sectionKey = sectionKey,
                    sourceIndex = sourceIndex,
                    itemIndex = index,
                    stream = stream,
                )
            },
        ) { _, stream ->
            val isSelectable = stream.isSelectableForPlayback(debridEnabled)
            val isUnsupportedTorrentStream =
                stream.needsLocalDebridResolve &&
                    !AppFeaturePolicy.p2pEnabled &&
                    !(debridEnabled && stream.isAddonDebridCandidate)
            StreamCard(
                stream = stream,
                enabled = isSelectable || isUnsupportedTorrentStream,
                appendInstantServiceToDefaultName = appendInstantServiceToDefaultName,
                showFileSizeBadges = showFileSizeBadges,
                showAddonLogo = showAddonLogo,
                badgePlacement = badgePlacement,
                onClick = {
                    if (isSelectable) {
                        onStreamSelected(stream, resumePositionMs, resumeProgressFraction)
                    } else if (isUnsupportedTorrentStream) {
                        NuvioToastController.show(torrentNotSupportedText)
                    }
                },
                onLongClick = {
                    if (stream.playableDirectUrl != null || stream.shouldOpenExternally || stream.isAddonDebridCandidate) {
                        onStreamLongPress(stream)
                    }
                },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    if (collapsible) {
        item(key = "more_$sectionKey") {
            StreamMoreLinksRow(
                hiddenCount = totalLinks - COLLAPSED_LINK_COUNT,
                expanded = expanded,
                onClick = onToggleExpanded,
            )
        }
    }
}

/** Links shown for a source before its arrow is tapped. */
private const val COLLAPSED_LINK_COUNT = 1

@Composable
private fun StreamMoreLinksRow(
    hiddenCount: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (expanded) {
                stringResource(Res.string.streams_show_fewer_links)
            } else {
                pluralStringResource(Res.plurals.streams_show_more_links, hiddenCount, hiddenCount)
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
        )
        Icon(
            imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

internal data class PinnedFirstStreamGroups(
    val pinnedGroups: List<AddonStreamGroup>,
    val remainingGroups: List<AddonStreamGroup>,
)

internal fun splitPinnedSources(
    groups: List<AddonStreamGroup>,
    pinnedSourceIds: List<String>,
): PinnedFirstStreamGroups {
    if (pinnedSourceIds.isEmpty()) return PinnedFirstStreamGroups(emptyList(), groups)

    val pinnedRank = HashMap<String, Int>(pinnedSourceIds.size)
    pinnedSourceIds.forEachIndexed { index, key ->
        if (key !in pinnedRank) pinnedRank[key] = index
    }

    val pinned = mutableListOf<Pair<Int, AddonStreamGroup>>()
    val remaining = mutableListOf<AddonStreamGroup>()

    groups.forEach { group ->
        // A favorite source (its star, or the CloudStream settings pin) lifts the whole group.
        val providerRank = pinnedRank[group.addonId]
        if (providerRank != null) {
            pinned += providerRank to group
            return@forEach
        }
        val isDebridGroup = group.addonId.startsWith("debrid:")
        val pinnedStreams = if (isDebridGroup) {
            emptyMap()
        } else {
            group.streams
                .filterNot { stream -> stream.isAddonDebridCandidate && stream.isDirectDebridStream }
                .groupBy { stream ->
                    PinnedStreamSourcesRepository.sourceKeyFor(
                        addonId = group.addonId,
                        sourceName = stream.sourceName,
                    )
                }
                .filterKeys { key -> key in pinnedRank }
        }

        if (pinnedStreams.isEmpty()) {
            remaining += group
            return@forEach
        }

        val pinnedUrls = pinnedStreams.values.flatten().toSet()
        pinnedStreams.forEach { (key, streams) ->
            pinned += pinnedRank.getValue(key) to group.copy(
                addonName = streams.firstOrNull()?.sourceName?.takeIf { it.isNotBlank() }
                    ?: group.addonName,
                streams = streams,
                isLoading = false,
                error = null,
            )
        }

        val leftovers = group.streams.filterNot { it in pinnedUrls }
        if (leftovers.isNotEmpty() || group.isLoading) {
            remaining += group.copy(streams = leftovers)
        }
    }

    return PinnedFirstStreamGroups(
        pinnedGroups = pinned.sortedBy { it.first }.map { it.second },
        remainingGroups = remaining,
    )
}

internal fun streamSectionRenderKey(
    groupIndex: Int,
    group: AddonStreamGroup,
): String = "$groupIndex:${group.addonId}"

internal fun streamCardRenderKey(
    sectionKey: String,
    sourceIndex: Int,
    itemIndex: Int,
    stream: StreamItem,
): String = buildString {
    append(sectionKey)
    append(':')
    append(sourceIndex)
    append(':')
    append(itemIndex)
    append(':')
    append(stream.url ?: stream.infoHash ?: stream.clientResolve?.infoHash ?: stream.streamLabel)
    stream.externalUrl?.let {
        append(':')
        append(it)
    }
}

// ---------------------------------------------------------------------------
// Stream Section Header
// ---------------------------------------------------------------------------

@Composable
private fun StreamSectionHeader(
    group: AddonStreamGroup,
    fetchingText: String,
    modifier: Modifier = Modifier,
) {
    val isLoading = group.isLoading
    val pinnedSourceIds by rememberPinnedStreamSourceIds()
    val pinTarget = remember(group) { group.favoriteTarget() }
    val isPinned = pinTarget?.key in pinnedSourceIds
    var pinSheetVisible by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { if (pinTarget != null) pinSheetVisible = true },
            )
            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = group.addonName,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = fetchingText,
                    modifier = Modifier.shimmer(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (pinTarget != null) {
                FavoriteSourceStar(target = pinTarget, isFavorite = isPinned, size = 26.dp)
            }
        }
    }

    StreamSourcePinSheet(
        target = pinTarget?.takeIf { pinSheetVisible },
        onDismiss = { pinSheetVisible = false },
    )
}

@Composable
private fun StreamSourceHeader(
    sourceName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = sourceName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamActionsSheet(
    stream: StreamItem?,
    externalPlayerEnabled: Boolean,
    onDismiss: () -> Unit,
    onCopyLink: (StreamItem) -> Unit,
    onDownload: (StreamItem) -> Unit,
    onOpen: (StreamItem, openExternally: Boolean) -> Unit,
) {
    if (stream == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    NuvioModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch {
                dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
            }
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = nuvioSafeBottomPadding(16.dp)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stream.streamLabel,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                stream.streamSubtitle
                    ?.takeIf { it.isNotBlank() }
                    ?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
            }

            if (stream.addonId.isNotBlank() && !stream.isDirectDebridStream) {
                val pinnedSourceIds by rememberPinnedStreamSourceIds()
                val sourceKey = stream.addonId
                val isPinned = sourceKey in pinnedSourceIds
                NuvioBottomSheetDivider()
                NuvioBottomSheetActionRow(
                    icon = Icons.Rounded.Star,
                    title = stringResource(
                        if (isPinned) Res.string.streams_unpin_source else Res.string.streams_pin_source,
                    ),
                    onClick = {
                        PinnedStreamSourcesRepository.setPinned(sourceKey, !isPinned)
                        coroutineScope.launch {
                            dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                        }
                    },
                )
            }

            NuvioBottomSheetDivider()
            NuvioBottomSheetActionRow(
                icon = Icons.Rounded.ContentCopy,
                title = stringResource(Res.string.streams_copy_link),
                onClick = {
                    onCopyLink(stream)
                    coroutineScope.launch {
                        dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                    }
                },
            )
            NuvioBottomSheetActionRow(
                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                title = stringResource(
                    if (externalPlayerEnabled) {
                        Res.string.streams_open_internal_player
                    } else {
                        Res.string.streams_open_external_player
                    },
                ),
                onClick = {
                    onOpen(stream, !externalPlayerEnabled)
                    coroutineScope.launch {
                        dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                    }
                },
            )
            NuvioBottomSheetActionRow(
                icon = Icons.Rounded.Download,
                title = stringResource(Res.string.streams_download_file),
                onClick = {
                    onDownload(stream)
                    coroutineScope.launch {
                        dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                    }
                },
            )
        }
    }
}

internal fun Long.toPlaybackClock(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        buildString {
            append(hours)
            append(':')
            append(minutes.toString().padStart(2, '0'))
            append(':')
            append(seconds.toString().padStart(2, '0'))
        }
    } else {
        buildString {
            append(minutes)
            append(':')
            append(seconds.toString().padStart(2, '0'))
        }
    }
}

// ---------------------------------------------------------------------------
// State blocks
// ---------------------------------------------------------------------------

@Composable
internal fun LoadingStateBlock(
    findingStreamsText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = findingStreamsText,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .shimmer(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.primary,
        )
        repeat(4) {
            StreamCardSkeleton()
        }
    }
}

@Composable
internal fun EmptyStateBlock(
    reason: StreamsEmptyStateReason?,
    details: String? = null,
    modifier: Modifier = Modifier,
) {
    val title: String
    val message: String

    when (reason) {
        StreamsEmptyStateReason.NoAddonsInstalled,
        StreamsEmptyStateReason.NoCompatibleAddons -> {
            title = stringResource(Res.string.playback_unavailable)
            message = stringResource(Res.string.playback_unavailable_message)
        }

        StreamsEmptyStateReason.StreamFetchFailed -> {
            title = stringResource(Res.string.streams_empty_load_failed_title)
            message = stringResource(Res.string.streams_empty_load_failed_message)
        }

        StreamsEmptyStateReason.NoStreamsFound, null -> {
            title = stringResource(Res.string.compose_player_no_streams_found)
            message = stringResource(Res.string.streams_empty_no_streams_message)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        if (details != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

internal const val EMPTY_STATE_MAX_ERRORS = 3

@Composable
internal fun FooterLoadingBlock(
    checkingMoreAddonsText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = checkingMoreAddonsText,
            modifier = Modifier.shimmer(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

internal data class StreamSearchResult(
    val uiState: StreamsUiState,
    val isEmptyBecauseOfSearch: Boolean,
)

@Composable
internal fun rememberStreamSearchResult(
    uiState: StreamsUiState,
    query: String,
    enabled: Boolean,
): StreamSearchResult = remember(uiState, query, enabled) {
    val terms = if (enabled) query.trim().lowercase().split(' ').filter { it.isNotEmpty() } else emptyList()
    if (terms.isEmpty()) return@remember StreamSearchResult(uiState, isEmptyBecauseOfSearch = false)
    val groups = uiState.groups.map { group ->
        group.copy(streams = group.streams.filter { it.matchesSearchTerms(terms) })
    }
    val filtered = uiState.copy(groups = groups)
    StreamSearchResult(
        uiState = filtered,
        isEmptyBecauseOfSearch = uiState.hasAnyStreams && !filtered.hasAnyStreams,
    )
}

private fun StreamItem.matchesSearchTerms(terms: List<String>): Boolean {
    val haystack = buildString {
        name?.let { append(it).append(' ') }
        title?.let { append(it).append(' ') }
        description?.let { append(it).append(' ') }
        sourceName?.let { append(it).append(' ') }
        append(addonName).append(' ')
        behaviorHints.filename?.let { append(it).append(' ') }
        badges.forEach { badge -> append(badge.name).append(' ') }
    }.lowercase()
    return terms.all { haystack.contains(it) }
}

@Composable
internal fun StreamSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trailingContent: (@Composable () -> Unit)? = if (query.isEmpty()) null else {
        {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(Res.string.streams_search_clear),
                    tint = MaterialTheme.nuvio.colors.textMuted,
                )
            }
        }
    }
    if (!isAmbientLiquidGlassActive()) {
        NuvioInputField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = stringResource(Res.string.streams_search_placeholder),
            modifier = modifier,
            trailingContent = trailingContent,
        )
        return
    }
    val tokens = MaterialTheme.nuvio
    val fieldShape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .linksSurface(fieldShape),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = fieldShape,
            placeholder = {
                Text(
                    text = stringResource(Res.string.streams_search_placeholder),
                    color = tokens.colors.textMuted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = tokens.colors.textPrimary),
            trailingIcon = trailingContent,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = tokens.colors.borderFocus,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = tokens.colors.accent,
            ),
        )
    }
}

@Composable
internal fun StreamSearchEmptyBlock(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.streams_search_no_results),
            color = MaterialTheme.nuvio.colors.textMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}
