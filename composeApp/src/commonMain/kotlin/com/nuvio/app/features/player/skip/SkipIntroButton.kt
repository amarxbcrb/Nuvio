package com.nuvio.app.features.player.skip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import nuvio.composeapp.generated.resources.action_close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.player_skip
import nuvio.composeapp.generated.resources.player_skip_intro
import nuvio.composeapp.generated.resources.player_skip_outro
import nuvio.composeapp.generated.resources.player_skip_recap
import nuvio.composeapp.generated.resources.player_skip_movie_credits
import nuvio.composeapp.generated.resources.player_skip_to_post_credits
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.ui.glass.ambientLiquidGlass

@Composable
fun SkipIntroButton(
    interval: SkipInterval?,
    dismissed: Boolean,
    controlsVisible: Boolean,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    skipsToPostCredits: Boolean = false,
) {
    var lastType by remember { mutableStateOf(interval?.type) }
    var lastSkipsToPostCredits by remember { mutableStateOf(skipsToPostCredits) }
    val canSkip = interval?.internalSkipAction(emptyList()) != null
    if (canSkip) {
        lastType = interval?.type
        lastSkipsToPostCredits = skipsToPostCredits
    }
    val shouldShow = canSkip && (!dismissed || controlsVisible)

    var autoHidden by remember { mutableStateOf(false) }
    var manuallyDismissed by remember { mutableStateOf(false) }
    // Closed with the × or a swipe: stays hidden for this interval, even when the controls show.
    var closedByUser by remember { mutableStateOf(false) }
    val swipeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }

    LaunchedEffect(interval?.startTime, interval?.type) {
        autoHidden = false
        manuallyDismissed = false
        closedByUser = false
        swipeOffset.snapTo(0f)
        progress.snapTo(0f)
    }

    LaunchedEffect(dismissed) {
        if (!dismissed) {
            if (!manuallyDismissed) {
                autoHidden = false
                progress.snapTo(0f)
            }
        } else {
            manuallyDismissed = true
        }
    }

    LaunchedEffect(shouldShow, autoHidden, controlsVisible) {
        if (shouldShow && !autoHidden && !controlsVisible) {
            progress.animateTo(
                1f,
                animationSpec = tween(
                    durationMillis = ((1f - progress.value) * 10000).toInt().coerceAtLeast(1),
                    easing = LinearEasing,
                ),
            )
            autoHidden = true
        }
    }

    val isVisible = shouldShow && !closedByUser && (!autoHidden || controlsVisible)
    val closeButton = {
        closedByUser = true
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f),
        exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.8f),
        modifier = modifier,
    ) {
        val shape = RoundedCornerShape(16.dp)
        val dismissDistance = with(LocalDensity.current) { 72.dp.toPx() }
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
                .graphicsLayer { alpha = 1f - (abs(swipeOffset.value) / (dismissDistance * 2f)).coerceIn(0f, 0.7f) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta -> scope.launch { swipeOffset.snapTo(swipeOffset.value + delta) } },
                    onDragStopped = {
                        if (abs(swipeOffset.value) > dismissDistance) closeButton()
                        else swipeOffset.animateTo(0f, tween(180))
                    },
                )
                .clip(shape)
                .ambientLiquidGlass(
                    surface = com.nuvio.app.core.ui.glass.GlassSurface.PLAYER_CARDS,
                    shape = shape,
                    tint = Color(0xFF1E1E1E).copy(alpha = 0.6f),
                    fallback = Color(0xFF1E1E1E).copy(alpha = 0.85f),
                    blurRadius = 18.dp,
                    refractionHeight = 10.dp,
                    refractionAmount = 14.dp,
                )
                .clickable(enabled = canSkip) { onSkip() },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = skipLabel(lastType, lastSkipsToPostCredits),
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Box(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(onClick = closeButton),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.action_close),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(Color.White.copy(alpha = if (controlsVisible || autoHidden || dismissed) 0f else 0.15f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value)
                        .height(3.dp)
                        .background(
                            Color(0xFF1E1E1E).copy(
                                alpha = if (controlsVisible || autoHidden || dismissed) 0f else 0.85f,
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun skipLabel(type: String?, skipsToPostCredits: Boolean): String =
    when (type?.lowercase()) {
        "intro", "op", "mixed-op" -> stringResource(Res.string.player_skip_intro)
        "outro", "ed", "mixed-ed", "credits" -> stringResource(
            if (skipsToPostCredits) Res.string.player_skip_to_post_credits else Res.string.player_skip_outro,
        )
        "recap" -> stringResource(Res.string.player_skip_recap)
        "movie-credits" -> stringResource(
            if (skipsToPostCredits) Res.string.player_skip_to_post_credits else Res.string.player_skip_movie_credits,
        )
        else -> stringResource(Res.string.player_skip)
    }
