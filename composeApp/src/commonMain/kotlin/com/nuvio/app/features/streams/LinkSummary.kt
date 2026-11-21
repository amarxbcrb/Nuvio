package com.nuvio.app.features.streams

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.nuvioPressScale
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.links_film_source
import nuvio.composeapp.generated.resources.links_original
import nuvio.composeapp.generated.resources.streams_pin_source
import nuvio.composeapp.generated.resources.streams_unpin_source
import org.jetbrains.compose.resources.stringResource

private val FavoriteRed = Color(0xFFFF4D5E)
private val FilmSourceGreen = Color(0xFF8BD39B)

/**
 * One link, as in every list of links:
 * « CNCVerse Bridge ★  Source du film » / « 1080p · Français VF » / « ▤ NetNaija » / badges.
 * The star adds the whole source to the active profile's favorites in one tap.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LinkSummary(
    stream: StreamItem,
    modifier: Modifier = Modifier,
    isFilmSource: Boolean = false,
    tag: String? = null,
    showSource: Boolean = true,
) {
    val info = remember(stream) { StreamLanguageDetector.detect(stream, cloudStreamProviderLanguages()[stream.addonId]) }
    val badges = remember(stream) { StreamTechBadges.detect(stream) }
    val resolution = badges.firstOrNull { it.kind == TechBadgeKind.Resolution }?.text
    val language = linkLanguageText(info)
    val origin = stream.sourceName?.trim()?.takeIf { it.isNotEmpty() && !it.equals(stream.addonName, ignoreCase = true) }
        ?: stream.cleanDisplayName().takeIf { !it.equals(stream.addonName, ignoreCase = true) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // In a source's card the source is its header: the first line is then where the link comes from.
            Text(
                text = if (showSource) stream.addonName else origin ?: stream.addonName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (isFilmSource) {
                Text(
                    text = stringResource(Res.string.links_film_source),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = FilmSourceGreen,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(FilmSourceGreen.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            if (tag != null) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            text = buildAnnotatedString {
                if (resolution != null) {
                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) { append(resolution) }
                    append(" · ")
                }
                append(language)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.62f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showSource && origin != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Dns, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                Text(
                    text = origin,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val badgeImages = stream.badges.filter { it.imageURL.isNotBlank() }
        if (badges.isNotEmpty() || badgeImages.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                badges.forEach { TechBadgeChip(it) }
                badgeImages.forEach { StreamBadgeImage(badge = it) }
            }
        }
    }
}

/**
 * ♥ in a round glass button, the size of the arrows: adds the whole source to the active profile's
 * favorites, or removes it. Nothing is shown for « Tout », which has no favorites.
 */
@Composable
internal fun FavoriteHeartButton(addonId: String, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 38.dp) {
    val pinnedSourceIds by rememberPinnedStreamSourceIds()
    val activeProfile by StreamProfileRepository.active.collectAsStateWithLifecycle()
    if (!activeProfile.hasPreferences || addonId.isBlank() || addonId.startsWith("debrid:")) return
    val favorite = pinnedSourceIds.any { it == addonId || it.startsWith("$addonId|") }
    Box(
        modifier = modifier
            .size(size)
            .nuvioPressScale(pressedScale = 0.85f)
            .clip(CircleShape)
            .background(if (favorite) FavoriteRed.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f))
            .border(1.dp, if (favorite) FavoriteRed.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable { PinnedStreamSourcesRepository.setGroupFavorite(addonId, !favorite) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = stringResource(if (favorite) Res.string.streams_unpin_source else Res.string.streams_pin_source),
            tint = if (favorite) FavoriteRed else Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** « Français VF », « VOSTFR », « VOST anglais », « Multi », « Anglais », « VO ». */
@Composable
internal fun linkLanguageText(info: StreamLanguageInfo): String {
    val version = info.versionKey()
    return when {
        version == "VF" -> localizedLanguageName("fr") + " VF"
        version == "MULTI" -> "Multi"
        version == "VO" -> stringResource(Res.string.links_original)
        version.startsWith("VOST") -> versionDisplayName(version)
        else -> localizedLanguageName(version)
    }
}

@Composable
private fun TechBadgeChip(badge: TechBadge) {
    val color = when (badge.kind) {
        TechBadgeKind.Resolution -> Color(0xFFE8C66A)
        TechBadgeKind.Release -> Color(0xFF7DB3FF)
        TechBadgeKind.Codec -> Color(0xFFB9A2FF)
        TechBadgeKind.Hdr -> Color(0xFFFFB25E)
        TechBadgeKind.Audio -> Color(0xFFE6E6E6)
    }
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = badge.text,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.55f), shape)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}
