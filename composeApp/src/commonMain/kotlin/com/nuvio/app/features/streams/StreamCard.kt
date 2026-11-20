package com.nuvio.app.features.streams

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.nuvioPressScale
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.streams_pin_source
import nuvio.composeapp.generated.resources.streams_unpin_source
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.SkeletonBlock
import com.nuvio.app.features.debrid.DebridProviders
import androidx.compose.ui.graphics.compositeOver
import com.nuvio.app.core.ui.glass.ambientLiquidGlass
import com.nuvio.app.core.ui.glass.isAmbientLiquidGlassActive
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow

internal val LocalStreamHighlight = staticCompositionLocalOf<(StreamItem) -> String?> { { null } }

internal data class SharedStream(
    val label: String,
    val stream: StreamItem,
)

internal val LocalSharedStream = staticCompositionLocalOf<SharedStream?> { null }

@Composable
internal fun StreamCard(
    stream: StreamItem,
    enabled: Boolean,
    appendInstantServiceToDefaultName: Boolean,
    showFileSizeBadges: Boolean,
    showAddonLogo: Boolean,
    badgePlacement: StreamBadgePlacement,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    currentLabel: String? = null,
    outlined: Boolean = false,
) {
    val partyLabel = if (isCurrent) null else LocalStreamHighlight.current(stream)
    val cardShape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .shadow(
                elevation = 2.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f),
            )
            .clip(cardShape)
            .ambientLiquidGlass(
                shape = cardShape,
                tint = if (isCurrent) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.White.copy(alpha = 0.05f)
                }.compositeOver(Color.Black.copy(alpha = StreamGlassTintAlpha)),
                fallback = if (isCurrent) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.White.copy(alpha = 0.05f)
                },
                blurRadius = 18.dp,
            )
            .then(
                if (isCurrent || outlined) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.52f),
                        shape = cardShape,
                    )
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The same summary as every list of links: source ★, quality · language, origin, badges.
        LinkSummary(
            stream = stream,
            modifier = Modifier.weight(1f),
            tag = when {
                isCurrent && !currentLabel.isNullOrBlank() -> currentLabel
                partyLabel != null -> partyLabel
                appendInstantServiceToDefaultName -> stream.instantServiceLabel()?.removePrefix("- ")
                else -> null
            },
        )
        FavoriteHeartButton(addonId = stream.addonId, modifier = Modifier.padding(start = 8.dp))
    }
}

/** « ★ CNCVerse Bridge · Main »: the star adds the whole source to the profile's favorites. */
@Composable
private fun StreamSourceLine(stream: StreamItem, showLogo: Boolean) {
    val pinnedSourceIds by rememberPinnedStreamSourceIds()
    val activeProfile by StreamProfileRepository.active.collectAsStateWithLifecycle()
    val canStar = activeProfile.hasPreferences && stream.addonId.isNotBlank() && !stream.addonId.startsWith("debrid:")
    val favorite = pinnedSourceIds.any { it == stream.addonId || it.startsWith("${stream.addonId}|") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (canStar) {
            Icon(
                imageVector = if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = stringResource(if (favorite) Res.string.streams_unpin_source else Res.string.streams_pin_source),
                tint = if (favorite) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(30.dp)
                    .nuvioPressScale(pressedScale = 0.8f)
                    .clip(CircleShape)
                    .clickable { PinnedStreamSourcesRepository.setGroupFavorite(stream.addonId, !favorite) }
                    .padding(4.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (showLogo && !stream.addonLogo.isNullOrBlank()) {
            AsyncImage(
                model = stream.addonLogo,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = stream.sourceLabel(),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, shadow = streamGlassTextShadow()),
            color = Color.White.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** « NetNaija · CNCVerse Bridge »: the server when the addon names one, then the addon or extension. */
internal fun StreamItem.sourceLabel(): String {
    val source = sourceName?.trim()?.takeIf { it.isNotEmpty() && !it.equals(addonName, ignoreCase = true) }
    return if (source != null) "$source · $addonName" else addonName
}

/**
 * Name of a link without the decoration addons put in it: emoji and symbols are removed, only the
 * first line is kept ("🎬CastleTvProvider 😮‍💨 480p" becomes "CastleTvProvider 480p").
 */
internal fun StreamItem.cleanDisplayName(): String {
    val raw = (name?.takeIf { it.isNotBlank() } ?: title?.takeIf { it.isNotBlank() } ?: streamLabel)
        .lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
    val cleaned = buildString {
        raw.forEach { char ->
            when {
                char.isLetterOrDigit() -> append(char)
                char.isWhitespace() -> append(' ')
                char in "-–—_.,:;|/()[]+&'’!?#%•·" -> append(char)
                else -> append(' ')
            }
        }
    }.replace(Regex("\\s+"), " ").trim().trim('|', '•', '·', '-', ' ')
    return cleaned.ifBlank { streamLabel }
}

@Composable
internal fun StreamCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.55f), height = 14.dp, cornerRadius = 4.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.85f), height = 10.dp, cornerRadius = 4.dp)
    }
}

@Composable
private fun StreamCardBadgeRow(
    badgeImages: List<StreamBadge>,
    stream: StreamItem,
    showFileSizeBadges: Boolean,
    languageLabel: String?,
    qualityLabel: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (qualityLabel != null) {
            StreamTextBadge(text = qualityLabel)
        }
        if (languageLabel != null) {
            StreamTextBadge(text = languageLabel)
        }
        badgeImages.forEach { badge ->
            StreamBadgeImage(badge = badge)
        }
        if (showFileSizeBadges) {
            StreamFileSizeBadge(stream = stream)
        }
    }
}

@Composable
private fun StreamNameWithInstantService(
    stream: StreamItem,
    appendInstantServiceToDefaultName: Boolean,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val nameStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        shadow = streamGlassTextShadow(),
    )
    val instantLabel = if (appendInstantServiceToDefaultName) {
        stream.instantServiceLabel()
    } else {
        null
    }
    val showInstantLabel = instantLabel != null
    val visibleState = remember(stream.streamLabel) {
        MutableTransitionState(showInstantLabel)
    }
    visibleState.targetState = showInstantLabel

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stream.cleanDisplayName(),
            modifier = Modifier.weight(1f, fill = false),
            style = nameStyle,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(durationMillis = 260)) +
                expandHorizontally(
                    animationSpec = tween(durationMillis = 260),
                    expandFrom = Alignment.Start,
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
                shrinkHorizontally(
                    animationSpec = tween(durationMillis = 120),
                    shrinkTowards = Alignment.Start,
                ),
            label = "streamNameInstantService",
        ) {
            Text(
                text = " ${instantLabel.orEmpty()}",
                style = nameStyle,
                color = Color.White,
            )
        }
        trailingContent()
    }
}

@Composable
private fun CurrentStreamBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal const val StreamGlassTintAlpha = 0.55f

private val StreamGlassTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.6f),
    offset = Offset(0f, 1f),
    blurRadius = 4f,
)

@Composable
internal fun streamGlassTextShadow(): Shadow? =
    if (isAmbientLiquidGlassActive()) StreamGlassTextShadow else null

private fun StreamItem.instantServiceLabel(): String? {
    val status = debridCacheStatus ?: return null
    if (status.state != StreamDebridCacheState.CACHED) return null
    val providerLabel = DebridProviders.shortName(status.providerId)
        .ifBlank { status.providerName.trim() }
        .ifBlank { DebridProviders.displayName(status.providerId) }
    return "- $providerLabel Instant"
}
