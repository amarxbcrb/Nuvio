package com.nuvio.app.features.details.components

import com.nuvio.app.core.ui.nuvioPressScale
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import com.nuvio.app.features.ratings.UserRatingStars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.rounded.Download
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.core.ui.appIconPainter
import nuvio.composeapp.generated.resources.Res
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Brush
import com.nuvio.app.features.streams.linksSurface
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import nuvio.composeapp.generated.resources.details_play_source
import nuvio.composeapp.generated.resources.details_trailer_action
import nuvio.composeapp.generated.resources.action_play
import nuvio.composeapp.generated.resources.details_download_action
import nuvio.composeapp.generated.resources.details_actions_menu_label
import nuvio.composeapp.generated.resources.hero_add_to_library
import nuvio.composeapp.generated.resources.hero_mark_unwatched
import nuvio.composeapp.generated.resources.hero_mark_watched
import nuvio.composeapp.generated.resources.hero_remove_from_library
import nuvio.composeapp.generated.resources.details_choose_source
import nuvio.composeapp.generated.resources.playback_unavailable
import nuvio.composeapp.generated.resources.random_episode_title
import nuvio.composeapp.generated.resources.shuffle_stop
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.Replay
import nuvio.composeapp.generated.resources.details_action_start_from_beginning
import nuvio.composeapp.generated.resources.sidebar_library
import nuvio.composeapp.generated.resources.streams_open_external_player
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.ui.glass.ambientLiquidGlass
import com.nuvio.app.core.ui.glass.isAmbientLiquidGlassActive

/**
 * Opens the picker of the user's Library categories for the title on screen.
 * Null (the default) hides the « Catégories » action.
 */
val LocalDetailCategoriesAction = staticCompositionLocalOf<(() -> Unit)?> { null }

data class DetailSecondaryAction(
    val label: String,
    val icon: ImageVector,
    val drawable: DrawableResource? = null,
    val isActive: Boolean = false,
    val onClick: () -> Unit = {},
    val onLongClick: (() -> Unit)? = null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailActionButtons(
    modifier: Modifier = Modifier,
    playLabel: String = stringResource(Res.string.action_play),
    playEnabled: Boolean = true,
    pinnedAction: DetailSecondaryAction? = null,
    secondaryActions: List<DetailSecondaryAction> = emptyList(),
    actionsMenuLabel: String = stringResource(Res.string.details_actions_menu_label),
    isTablet: Boolean = false,
    onPlayClick: () -> Unit = {},
    onPlayLongClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    iconActionRow: Boolean = false,
    iconActions: List<DetailSecondaryAction> = emptyList(),
    userRating: Int? = null,
    onRateClick: (() -> Unit)? = null,
    onSourceSegmentClick: (() -> Unit)? = null,
    playSource: DetailPlaySource? = null,
) {
    val playPainter = appIconPainter(AppIconResource.PlayerPlay)
    val buttonHeight = if (isTablet) 56.dp else 52.dp
    val iconButtonSize = buttonHeight
    val playShape = RoundedCornerShape(40.dp)
    val hapticFeedback = LocalHapticFeedback.current
    var actionsExpanded by remember { mutableStateOf(false) }
    val menuProgress by animateFloatAsState(
        targetValue = if (actionsExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "detail_action_menu_progress",
    )
    val hasSecondaryActions = secondaryActions.isNotEmpty()

    Column(
        modifier = modifier
            .widthIn(max = if (isTablet) 520.dp else 420.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (iconActionRow) {
            PlayButton(
                playLabel = playLabel,
                playEnabled = playEnabled,
                playPainter = playPainter,
                playShape = playShape,
                buttonHeight = buttonHeight,
                isTablet = isTablet,
                onPlayClick = onPlayClick,
                onPlayLongClick = onPlayLongClick,
                modifier = Modifier.fillMaxWidth(),
                onSourceSegmentClick = onSourceSegmentClick,
                sourceSegmentLabel = stringResource(Res.string.details_choose_source),
            )
            playSource?.let { source ->
                PlaySourceLine(source = source, modifier = Modifier.fillMaxWidth())
            }
            if (iconActions.isNotEmpty()) {
                DetailIconActionRow(
                    actions = iconActions,
                    preferredSize = iconButtonSize,
                    preferredSpacing = if (isTablet) 20.dp else 16.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            onRateClick?.let { rate ->
                UserRatingStars(
                    rating = userRating,
                    onClick = rate,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            return@Column
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight),
        ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayButton(
                playLabel = playLabel,
                playEnabled = playEnabled,
                playPainter = playPainter,
                playShape = playShape,
                buttonHeight = buttonHeight,
                isTablet = isTablet,
                onPlayClick = onPlayClick,
                onPlayLongClick = onPlayLongClick,
                modifier = Modifier.weight(1f),
            )

            if (pinnedAction != null) {
                Spacer(modifier = Modifier.width(12.dp))
                DetailIconAction(
                    label = pinnedAction.label,
                    icon = pinnedAction.icon,
                    active = pinnedAction.isActive,
                    progress = 1f,
                    size = iconButtonSize,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        pinnedAction.onClick()
                    },
                )
            }

            if (hasSecondaryActions) {
                Spacer(modifier = Modifier.width(12.dp))
                secondaryActions.forEachIndexed { index, action ->
                    Box(
                        modifier = Modifier
                            .width(iconButtonSize * menuProgress)
                            .height(iconButtonSize)
                            .graphicsLayer {
                                clip = true
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (actionsExpanded || menuProgress > 0.01f) {
                            DetailIconAction(
                                label = action.label,
                                icon = action.icon,
                                drawable = action.drawable,
                                active = action.isActive,
                                progress = menuProgress,
                                size = iconButtonSize,
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    action.onClick()
                                },
                                onLongClick = action.onLongClick?.let { longClick ->
                                    {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        longClick()
                                    }
                                },
                            )
                        }
                    }

                    if (index != secondaryActions.lastIndex) {
                        Spacer(modifier = Modifier.width(12.dp * menuProgress))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp * menuProgress))
            }

            if (hasSecondaryActions) {
                val moreGlassActive = !actionsExpanded && isAmbientLiquidGlassActive()
                Surface(
                    modifier = Modifier
                        .size(iconButtonSize)
                        .then(
                            if (moreGlassActive) {
                                Modifier.ambientLiquidGlass(
                                    shape = CircleShape,
                                    tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    fallback = Color.Transparent,
                                )
                            } else {
                                Modifier
                            },
                        ),
                    shape = CircleShape,
                    color = if (actionsExpanded) {
                        MaterialTheme.colorScheme.onBackground
                    } else if (moreGlassActive) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
                    },
                    contentColor = if (actionsExpanded) {
                        MaterialTheme.colorScheme.background
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconButtonSize)
                            .clickable(role = Role.Button) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                actionsExpanded = !actionsExpanded
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = actionsMenuLabel,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    rotationZ = 90f * menuProgress
                                },
                        )
                    }
                }
            }
        }
    }

        if (onDownloadClick != null) {
            val downloadGlassActive = isAmbientLiquidGlassActive()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight)
                    .then(
                        if (downloadGlassActive) {
                            Modifier.ambientLiquidGlass(
                                shape = playShape,
                                tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                fallback = Color.Transparent,
                            )
                        } else {
                            Modifier
                        },
                    ),
                shape = playShape,
                color = if (downloadGlassActive) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onDownloadClick)
                        .height(buttonHeight),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(if (isTablet) 20.dp else 18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.details_download_action),
                        style = if (isTablet) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleSmall
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        onRateClick?.let { rate ->
            UserRatingStars(
                rating = userRating,
                onClick = rate,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

// Upstream's single entry point for the detail actions (phone sections and the tablet hero).
// Fork additions: download / play from start / external player in the icon row, the icon-row
// layout itself, the user rating stars, and the fork's watched/library icons.
@Composable
fun DetailActions(
    playLabel: String,
    playEnabled: Boolean,
    isSaved: Boolean,
    isWatched: Boolean,
    isTablet: Boolean,
    shuffleEnabled: Boolean,
    onPlayClick: () -> Unit,
    onPlayLongClick: (() -> Unit)?,
    onShuffleClick: (() -> Unit)?,
    onWatchedClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveLongClick: (() -> Unit)?,
    onDownloadClick: (() -> Unit)? = null,
    onPlayFromStartClick: (() -> Unit)? = null,
    onPlayExternallyClick: (() -> Unit)? = null,
    iconActionRow: Boolean = false,
    userRating: Int? = null,
    onRateClick: (() -> Unit)? = null,
    onTrailerClick: (() -> Unit)? = null,
    playSource: DetailPlaySource? = null,
    showSourceSegment: Boolean = true,
) {
    val actionSet = detailActionSet(
        isSaved = isSaved,
        isWatched = isWatched,
        shuffleEnabled = shuffleEnabled,
        onShuffleClick = onShuffleClick,
        onWatchedClick = onWatchedClick,
        onSaveClick = onSaveClick,
        onSaveLongClick = onSaveLongClick,
        onDownloadClick = onDownloadClick,
        onPlayFromStartClick = onPlayFromStartClick,
        onPlayExternallyClick = onPlayExternallyClick,
        // The long press on Play stays; the split button's right segment does the same.
        onChooseSourceClick = onPlayLongClick,
        onTrailerClick = onTrailerClick,
    )
    val shuffleAction = actionSet.shuffle
    val watchedAction = actionSet.watched
    val saveAction = actionSet.save
    val chooseSourceAction = actionSet.chooseSource
    val iconActions = actionSet.icons
    DetailActionButtons(
        playLabel = if (playEnabled) playLabel else stringResource(Res.string.playback_unavailable),
        playEnabled = playEnabled,
        pinnedAction = shuffleAction?.takeIf { shuffleEnabled },
        iconActionRow = iconActionRow,
        iconActions = iconActions,
        secondaryActions = buildList {
            chooseSourceAction?.let(::add)
            if (!shuffleEnabled) shuffleAction?.let(::add)
            add(watchedAction)
            add(saveAction)
        },
        isTablet = isTablet,
        onPlayClick = onPlayClick,
        onDownloadClick = onDownloadClick.takeIf { !iconActionRow },
        onPlayLongClick = onPlayLongClick,
        userRating = userRating,
        onRateClick = onRateClick,
        onSourceSegmentClick = onPlayLongClick.takeIf { iconActionRow && showSourceSegment },
        playSource = playSource.takeIf { iconActionRow },
    )
}

private class DetailActionSet(
    val shuffle: DetailSecondaryAction?,
    val watched: DetailSecondaryAction,
    val save: DetailSecondaryAction,
    val chooseSource: DetailSecondaryAction?,
    val icons: List<DetailSecondaryAction>,
)

@Composable
private fun detailActionSet(
    isSaved: Boolean,
    isWatched: Boolean,
    shuffleEnabled: Boolean,
    onShuffleClick: (() -> Unit)?,
    onWatchedClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveLongClick: (() -> Unit)?,
    onDownloadClick: (() -> Unit)?,
    onPlayFromStartClick: (() -> Unit)?,
    onPlayExternallyClick: (() -> Unit)?,
    onChooseSourceClick: (() -> Unit)?,
    onTrailerClick: (() -> Unit)?,
): DetailActionSet {
    val shuffleAction = onShuffleClick?.let { onClick ->
        DetailSecondaryAction(
            label = stringResource(if (shuffleEnabled) Res.string.shuffle_stop else Res.string.random_episode_title),
            icon = Icons.Default.Shuffle,
            isActive = shuffleEnabled,
            onClick = onClick,
        )
    }
    val watchedAction = DetailSecondaryAction(
        label = stringResource(if (isWatched) Res.string.hero_mark_unwatched else Res.string.hero_mark_watched),
        icon = if (isWatched) Icons.Default.Visibility else Icons.Default.VisibilityOff,
        isActive = isWatched,
        onClick = onWatchedClick,
    )
    val saveAction = DetailSecondaryAction(
        label = stringResource(if (isSaved) Res.string.hero_remove_from_library else Res.string.hero_add_to_library),
        icon = Icons.Default.Add,
        drawable = Res.drawable.sidebar_library.takeIf { isSaved },
        isActive = isSaved,
        onClick = onSaveClick,
        onLongClick = onSaveLongClick,
    )
    // One button for the Library and its categories: the categories sheet also saves or removes
    // the title, so « + » opens it when it exists instead of a separate « Catégories » button.
    val openCategories = LocalDetailCategoriesAction.current
    val libraryAction = if (openCategories != null) saveAction.copy(onClick = openCategories) else saveAction
    val chooseSourceLabel = stringResource(Res.string.details_choose_source)
    val chooseSourceAction = onChooseSourceClick?.let { chooseSource ->
        DetailSecondaryAction(label = chooseSourceLabel, icon = Icons.Rounded.VideoLibrary, onClick = chooseSource)
    }
    val downloadLabel = stringResource(Res.string.details_download_action)
    val playFromStartLabel = stringResource(Res.string.details_action_start_from_beginning)
    val externalLabel = stringResource(Res.string.streams_open_external_player)
    val trailerLabel = stringResource(Res.string.details_trailer_action)
    val iconActions = buildList {
        // First round button: the trailer (hidden when there is none). Choosing the source moved
        // to the right segment of the split « Lire » button.
        onTrailerClick?.let { trailer ->
            add(DetailSecondaryAction(label = trailerLabel, icon = Icons.Rounded.Videocam, onClick = trailer))
        }
        onDownloadClick?.let { download ->
            add(DetailSecondaryAction(label = downloadLabel, icon = Icons.Rounded.Download, onClick = download))
        }
        onPlayFromStartClick?.let { playFromStart ->
            add(DetailSecondaryAction(label = playFromStartLabel, icon = Icons.Rounded.Replay, onClick = playFromStart))
        }
        shuffleAction?.let(::add)
        onPlayExternallyClick?.let { playExternally ->
            add(
                DetailSecondaryAction(
                    label = externalLabel,
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = playExternally,
                ),
            )
        }
        add(watchedAction)
        add(libraryAction)
    }
    return DetailActionSet(shuffleAction, watchedAction, libraryAction, chooseSourceAction, iconActions)
}

@Composable
fun TabletDetailHeroActions(
    playLabel: String,
    playEnabled: Boolean,
    isSaved: Boolean,
    isWatched: Boolean,
    shuffleEnabled: Boolean,
    landscape: Boolean,
    onPlayClick: () -> Unit,
    onPlayLongClick: (() -> Unit)?,
    onShuffleClick: (() -> Unit)?,
    onWatchedClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onDownloadClick: (() -> Unit)? = null,
    onPlayFromStartClick: (() -> Unit)? = null,
    onPlayExternallyClick: (() -> Unit)? = null,
    userRating: Int? = null,
    onRateClick: (() -> Unit)? = null,
    onTrailerClick: (() -> Unit)? = null,
    playSource: DetailPlaySource? = null,
    showSourceSegment: Boolean = true,
) {
    val actionSet = detailActionSet(
        isSaved = isSaved,
        isWatched = isWatched,
        shuffleEnabled = shuffleEnabled,
        onShuffleClick = onShuffleClick,
        onWatchedClick = onWatchedClick,
        onSaveClick = onSaveClick,
        onSaveLongClick = onSaveLongClick,
        onDownloadClick = onDownloadClick,
        onPlayFromStartClick = onPlayFromStartClick,
        onPlayExternallyClick = onPlayExternallyClick,
        // The long press on Play stays; the split button's right segment does the same.
        onChooseSourceClick = onPlayLongClick,
        onTrailerClick = onTrailerClick,
    )
    val sourceSegmentClick = onPlayLongClick.takeIf { showSourceSegment }
    val sourceSegmentLabel = stringResource(Res.string.details_choose_source)
    val playPainter = appIconPainter(AppIconResource.PlayerPlay)
    val buttonHeight = 56.dp
    val playShape = RoundedCornerShape(40.dp)
    val resolvedPlayLabel = if (playEnabled) playLabel else stringResource(Res.string.playback_unavailable)

    if (landscape) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TabletHeroActionGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayButton(
                playLabel = resolvedPlayLabel,
                playEnabled = playEnabled,
                playPainter = playPainter,
                playShape = playShape,
                buttonHeight = buttonHeight,
                isTablet = true,
                onPlayClick = onPlayClick,
                onPlayLongClick = onPlayLongClick,
                modifier = Modifier.weight(1f),
                onSourceSegmentClick = sourceSegmentClick,
                sourceSegmentLabel = sourceSegmentLabel,
            )
            if (onRateClick != null) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    UserRatingStars(rating = userRating, onClick = onRateClick, starSize = TabletRatingStarSize)
                }
                DetailIconActionRow(
                    actions = actionSet.icons,
                    preferredSize = buttonHeight,
                    preferredSpacing = 16.dp,
                    modifier = Modifier.weight(1f),
                )
            } else {
                DetailIconActionRow(
                    actions = actionSet.icons,
                    preferredSize = buttonHeight,
                    preferredSpacing = 16.dp,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(2f),
                )
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PlayButton(
                playLabel = resolvedPlayLabel,
                playEnabled = playEnabled,
                playPainter = playPainter,
                playShape = playShape,
                buttonHeight = buttonHeight,
                isTablet = true,
                onPlayClick = onPlayClick,
                onPlayLongClick = onPlayLongClick,
                modifier = Modifier
                    .widthIn(max = TabletPortraitActionsMaxWidth)
                    .fillMaxWidth(),
                onSourceSegmentClick = sourceSegmentClick,
                sourceSegmentLabel = sourceSegmentLabel,
            )
            playSource?.let { source ->
                PlaySourceLine(
                    source = source,
                    modifier = Modifier.widthIn(max = TabletPortraitActionsMaxWidth).fillMaxWidth(),
                )
            }
            DetailIconActionRow(
                actions = actionSet.icons,
                preferredSize = buttonHeight,
                preferredSpacing = 20.dp,
                modifier = Modifier
                    .widthIn(max = TabletPortraitActionsMaxWidth)
                    .fillMaxWidth(),
            )
            onRateClick?.let { rate ->
                UserRatingStars(rating = userRating, onClick = rate, starSize = TabletRatingStarSize)
            }
        }
    }
}

private val TabletHeroActionGap = 24.dp
private val TabletPortraitActionsMaxWidth = 520.dp
private val TabletRatingStarSize = 34.dp

@Composable
private fun DetailIconActionRow(
    actions: List<DetailSecondaryAction>,
    preferredSize: Dp,
    preferredSpacing: Dp,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    if (actions.isEmpty()) {
        Spacer(modifier = modifier)
        return
    }
    BoxWithConstraints(modifier = modifier) {
        val layout = fitIconActionRow(
            count = actions.size,
            availableWidth = maxWidth,
            preferredSize = preferredSize,
            preferredSpacing = preferredSpacing,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (layout.fits) Modifier else Modifier.horizontalScroll(rememberScrollState())),
            horizontalArrangement = Arrangement.spacedBy(
                space = layout.spacing,
                alignment = horizontalAlignment,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { action ->
                DetailIconAction(
                    label = action.label,
                    icon = action.icon,
                    drawable = action.drawable,
                    active = action.isActive,
                    progress = 1f,
                    size = layout.size,
                    onClick = action.onClick,
                    onLongClick = action.onLongClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DetailIconAction(
    label: String,
    icon: ImageVector,
    active: Boolean,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp,
    drawable: DrawableResource? = null,
    onLongClick: (() -> Unit)? = null,
) {
    // Same surface as the links screen cards and pills (dark frosted glass, or black with a
    // hairline in the opaque style), so the two screens look alike.
    Surface(
        modifier = modifier
            .nuvioPressScale(pressedScale = 0.88f)
            .graphicsLayer {
                alpha = progress
                scaleX = 0.86f + (0.14f * progress)
                scaleY = 0.86f + (0.14f * progress)
            }
            .then(if (active) Modifier else Modifier.linksSurface(CircleShape)),
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.onBackground else Color.Transparent,
        contentColor = if (active) {
            MaterialTheme.colorScheme.background
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    role = Role.Button,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (drawable != null) {
                Icon(
                    painter = painterResource(drawable),
                    contentDescription = label,
                    modifier = Modifier.size(21.dp),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayButton(
    playLabel: String,
    playEnabled: Boolean,
    playPainter: Painter,
    playShape: Shape,
    buttonHeight: Dp,
    isTablet: Boolean,
    onPlayClick: () -> Unit,
    onPlayLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onSourceSegmentClick: (() -> Unit)? = null,
    sourceSegmentLabel: String? = null,
) {
        Surface(
            modifier = modifier
                .nuvioPressScale(pressedScale = 0.97f, enabled = playEnabled)
                .height(buttonHeight)
                .then(
                    if (playEnabled) {
                        // Same as « Reprendre » on the links screen: light at the top, thin light edge.
                        val accent = MaterialTheme.nuvio.colors.accent
                        Modifier
                            .clip(playShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.18f).compositeOver(accent), accent),
                                ),
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.22f), playShape)
                    } else {
                        Modifier
                    },
                ),
            shape = playShape,
            color = if (playEnabled) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
            // White on any saturated accent (red, blue…), dark only on a very light one (white, gold).
            contentColor = if (playEnabled) {
                if (MaterialTheme.nuvio.colors.accent.luminance() > 0.6f) Color(0xFF111111) else Color.White
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        ) {
          Row(modifier = Modifier.fillMaxWidth().height(buttonHeight)) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        enabled = playEnabled,
                        onClick = {
                            onPlayClick()
                        },
                        onLongClick = onPlayLongClick,
                        role = Role.Button,
                    )
                    .height(buttonHeight),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = playPainter,
                    contentDescription = null,
                    modifier = Modifier.size(if (isTablet) 20.dp else 18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = playLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Split button: right segment (86dp) opens the source choice; Play keeps the left part.
            if (onSourceSegmentClick != null) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.35f)),
                )
                Row(
                    modifier = Modifier
                        .width(PlaySourceSegmentWidth)
                        .fillMaxHeight()
                        .clickable(enabled = playEnabled, role = Role.Button, onClick = onSourceSegmentClick)
                        .semantics { if (sourceSegmentLabel != null) contentDescription = sourceSegmentLabel },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Dns,
                        contentDescription = null,
                        modifier = Modifier.size(if (isTablet) 22.dp else 20.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
          }
        }
}

private val PlaySourceSegmentWidth = 86.dp

/** Source that « Lire » will use, shown under the button with its language/quality badges. */
data class DetailPlaySource(
    val name: String,
    val badges: List<String> = emptyList(),
)

@Composable
private fun PlaySourceLine(
    source: DetailPlaySource,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(Res.string.details_play_source, source.name)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.nuvio.colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        source.badges.forEach { badge ->
            Text(
                text = badge,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.nuvio.colors.textPrimary,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

private data class IconActionRowLayout(val size: Dp, val spacing: Dp, val fits: Boolean)

private fun fitIconActionRow(
    count: Int,
    availableWidth: Dp,
    preferredSize: Dp,
    preferredSpacing: Dp,
): IconActionRowLayout {
    if (count <= 1) return IconActionRowLayout(preferredSize, preferredSpacing, true)
    val gaps = count - 1
    val minSpacing = 8.dp
    val minSize = 44.dp
    if (preferredSize * count + preferredSpacing * gaps <= availableWidth) {
        return IconActionRowLayout(preferredSize, preferredSpacing, true)
    }
    val spacingAtPreferredSize = (availableWidth - preferredSize * count) / gaps
    if (spacingAtPreferredSize >= minSpacing) {
        return IconActionRowLayout(preferredSize, spacingAtPreferredSize, true)
    }
    val shrunkSize = (availableWidth - minSpacing * gaps) / count
    return if (shrunkSize >= minSize) {
        IconActionRowLayout(shrunkSize, minSpacing, true)
    } else {
        IconActionRowLayout(minSize, minSpacing, false)
    }
}
