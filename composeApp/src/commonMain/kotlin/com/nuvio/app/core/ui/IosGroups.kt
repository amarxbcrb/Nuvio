package com.nuvio.app.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * iOS grouped-list building blocks for sheets and screens: small caps section headers, filled cards
 * with large corners, rows with an inset divider, coloured icon tiles, green switches and a
 * segmented control whose selection fades from one segment to the other.
 */

internal val IosGroupShape = RoundedCornerShape(22.dp)
internal val IosGroupFill = Color.White.copy(alpha = 0.07f)
internal val IosGreen = Color(0xFF30D158)
internal val IosOrange = Color(0xFFFF9F0A)
internal val IosRed = Color(0xFFFF453A)
internal val IosGray = Color(0xFF8E8E93)

/** "PROFIL ............ Mémorisé": the title above a group, with an optional note on the right. */
@Composable
internal fun IosSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    trailingColor: Color = MaterialTheme.nuvio.colors.accent,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.nuvio.colors.textMuted,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = trailingColor,
            )
        }
    }
}

/** A filled card holding rows. */
@Composable
internal fun IosGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().clip(IosGroupShape).background(IosGroupFill),
        content = content,
    )
}

/** Hairline between two rows, starting under the text like iOS. */
@Composable
internal fun IosGroupDivider(startInset: Dp = 16.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startInset),
        thickness = 0.5.dp,
        color = Color.White.copy(alpha = 0.10f),
    )
}

/** A row of a group: optional leading content, a title, then [trailing]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun IosRow(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.nuvio.colors.textPrimary,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        enabled = enabled,
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke(this)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) titleColor else titleColor.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.nuvio.colors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke(this)
    }
}

/** Coloured square with a white glyph, as in the iOS settings. */
@Composable
internal fun IosIconTile(icon: ImageVector, color: Color, modifier: Modifier = Modifier, size: Dp = 30.dp) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(8.dp)).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
    }
}

/** Accent check mark shown on the selected row of a choice list. */
@Composable
internal fun IosCheckMark(visible: Boolean) {
    if (!visible) return
    Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = null,
        tint = MaterialTheme.nuvio.colors.accent,
        modifier = Modifier.size(22.dp),
    )
}

/** iOS switch: green track when on, white thumb, no outline. */
@Composable
internal fun IosSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        thumbContent = {},
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = IosGreen,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFF39393D),
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** One segment of [IosSegmentedControl]: a label and an optional count in grey. */
internal data class IosSegment(val key: String, val label: String, val count: Int? = null)

/**
 * Segmented control in a pill track. With [scrollable] the segments keep their width and the
 * track scrolls (any number of profiles); otherwise they share the width equally.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun IosSegmentedControl(
    segments: List<IosSegment>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    onLongPress: ((String) -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White.copy(alpha = 0.08f))
            .then(if (scrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segments.forEach { segment ->
            val selected = segment.key == selectedKey
            val fill by animateColorAsState(
                targetValue = if (selected) Color.White.copy(alpha = 0.20f) else Color.Transparent,
                animationSpec = tween(220),
                label = "ios_segment_fill",
            )
            Row(
                modifier = Modifier
                    .then(if (scrollable) Modifier else Modifier.weight(1f))
                    .nuvioPressScale(pressedScale = 0.94f)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(fill)
                    .combinedClickable(
                        onClick = { onSelect(segment.key) },
                        onLongClick = onLongPress?.let { longPress -> { longPress(segment.key) } },
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = segment.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) tokens.colors.textPrimary else tokens.colors.textPrimary.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (segment.count != null) {
                    Text(
                        text = " ${segment.count}",
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                    )
                }
            }
        }
        trailing?.invoke(this)
    }
}
