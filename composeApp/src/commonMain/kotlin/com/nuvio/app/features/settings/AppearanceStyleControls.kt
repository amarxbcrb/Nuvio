package com.nuvio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.AppTheme
import com.nuvio.app.core.ui.ThemeColors
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.labelRes
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.membership.MemberAccessRepository
import com.nuvio.app.features.membership.availableAppThemes
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.appearance_background
import nuvio.composeapp.generated.resources.appearance_background_amoled
import nuvio.composeapp.generated.resources.appearance_background_description
import nuvio.composeapp.generated.resources.appearance_background_dynamic
import nuvio.composeapp.generated.resources.appearance_style_glass
import nuvio.composeapp.generated.resources.appearance_style_glass_hint
import nuvio.composeapp.generated.resources.appearance_style_opaque
import nuvio.composeapp.generated.resources.appearance_style_opaque_hint
import nuvio.composeapp.generated.resources.settings_interface_style_glass_unsupported
import nuvio.composeapp.generated.resources.theme_custom
import org.jetbrains.compose.resources.stringResource

/** Style axis: two preview cards side by side, Liquid glass or Opaque. */
@Composable
internal fun AppearanceStyleCards(
    glass: Boolean,
    glassAvailable: Boolean,
    onSelect: (glass: Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StyleCard(
            isGlass = true,
            selected = glass,
            enabled = glassAvailable,
            title = stringResource(Res.string.appearance_style_glass),
            hint = stringResource(
                if (glassAvailable) Res.string.appearance_style_glass_hint
                else Res.string.settings_interface_style_glass_unsupported,
            ),
            onClick = { onSelect(true) },
            modifier = Modifier.weight(1f),
        )
        StyleCard(
            isGlass = false,
            selected = !glass,
            enabled = true,
            title = stringResource(Res.string.appearance_style_opaque),
            hint = stringResource(Res.string.appearance_style_opaque_hint),
            onClick = { onSelect(false) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StyleCard(
    isGlass: Boolean,
    selected: Boolean,
    enabled: Boolean,
    title: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val accent = tokens.colors.accent
    val outer = RoundedCornerShape(20.dp)
    val inner = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .clip(outer)
                .background(Color(0xFF141213))
                .border(2.dp, if (selected) accent else Color.Transparent, outer)
                .padding(10.dp)
                .clip(inner)
                .background(
                    if (isGlass) {
                        Brush.verticalGradient(listOf(accent.copy(alpha = 0.55f), Color(0xFF3A1F24)))
                    } else {
                        Brush.verticalGradient(listOf(Color.Black, Color.Black))
                    },
                )
                .padding(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1f, 1f, 0.7f).forEachIndexed { index, widthFraction ->
                    val pill = RoundedCornerShape(50)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(widthFraction)
                            .height(if (index == 0) 26.dp else 18.dp)
                            .clip(pill)
                            .background(
                                when {
                                    isGlass -> Color.White.copy(alpha = 0.16f)
                                    index == 0 -> accent.copy(alpha = 0.22f)
                                    else -> Color(0xFF1C1A1B)
                                },
                            )
                            .then(
                                if (isGlass) Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), pill)
                                else Modifier,
                            ),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) tokens.colors.textPrimary else tokens.colors.textMuted,
            )
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = tokens.colors.textMuted,
        )
    }
}

/** Accent axis: one round swatch per available theme, "+" opens the custom colour editor. */
@Composable
internal fun AccentSwatchRow(
    isTablet: Boolean,
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val memberAccess by remember {
        MemberAccessRepository.ensureStarted()
        MemberAccessRepository.access
    }.collectAsStateWithLifecycle()
    val customColors by ThemeSettingsRepository.customThemeColors.collectAsStateWithLifecycle()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    val themes = availableAppThemes(memberAccess.entitlements)
    val swatch = if (isTablet) 40.dp else 36.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            themes.forEach { theme ->
                // The custom theme is shown as a swatch only once chosen; "+" edits it.
                if (theme == AppTheme.CUSTOM && selectedTheme != AppTheme.CUSTOM) return@forEach
                val palette = remember(theme, customColors) { ThemeColors.getColorPalette(theme, customColors) }
                val selected = theme == selectedTheme
                val label = stringResource(theme.labelRes)
                Box(
                    modifier = Modifier
                        .size(swatch + 8.dp)
                        .then(if (selected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(palette.accentBrush())
                        .selectable(selected = selected, role = Role.RadioButton) {
                            if (theme == AppTheme.CUSTOM) showEditor = true else onThemeSelected(theme)
                        }
                        .semantics { contentDescription = label },
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        val customLabel = stringResource(Res.string.theme_custom)
        Box(
            modifier = Modifier
                .size(swatch + 4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .selectable(selected = false, role = Role.Button) { showEditor = true }
                .semantics { contentDescription = customLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = tokens.colors.textPrimary)
        }
    }
    if (showEditor) {
        CustomThemeEditor(
            initialColors = customColors,
            allowGradient = memberAccess.tier != null,
            onSave = ThemeSettingsRepository::setCustomTheme,
            onDismiss = { showEditor = false },
        )
    }
}

/** Fond axis: segmented control AMOLED black / Dynamic, independent of the style. */
@Composable
internal fun AppearanceBackgroundChoice(
    isTablet: Boolean,
    dynamic: Boolean,
    onSelect: (dynamic: Boolean) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = stringResource(Res.string.appearance_background),
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.colors.textPrimary,
        )
        Text(
            text = stringResource(Res.string.appearance_background_description),
            style = MaterialTheme.typography.bodySmall,
            color = tokens.colors.textMuted,
        )
        Spacer(Modifier.height(12.dp))
        val outer = RoundedCornerShape(14.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(outer)
                .background(Color.White.copy(alpha = 0.06f))
                .padding(4.dp),
        ) {
            listOf(
                false to Res.string.appearance_background_amoled,
                true to Res.string.appearance_background_dynamic,
            ).forEach { (value, label) ->
                val selected = value == dynamic
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) tokens.colors.accent else Color.Transparent)
                        .selectable(selected = selected, role = Role.RadioButton) { onSelect(value) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = if (selected) Color.White else tokens.colors.textMuted,
                    )
                }
            }
        }
    }
}
