package com.nuvio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import com.nuvio.app.core.ui.glass.GlassSurface
import com.nuvio.app.core.ui.glass.LiquidGlassMaterial
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.nuvio.app.core.ui.glass.GlassSurfaceSettings
import com.nuvio.app.core.ui.glass.forNavigationBar
import com.nuvio.app.core.ui.glass.liquidGlassSurface
import com.nuvio.app.core.ui.glass.nuvioBackdropSource
import com.nuvio.app.core.ui.glass.rememberNuvioBackdrop
import com.nuvio.app.core.ui.glass.LiquidGlassStyle
import com.nuvio.app.core.ui.glass.isLiquidGlassSupported
import com.nuvio.app.core.ui.glass.isLiquidRefractionSupported
import org.jetbrains.compose.resources.StringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioBottomSheetActionRow
import com.nuvio.app.core.ui.NuvioBottomSheetDivider
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.formatHexColor
import com.nuvio.app.core.ui.parseHexColor
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.LocalNuvioSheetBackdrop
import com.nuvio.app.core.ui.nuvioSheetSurface
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.core.ui.floatingNavigationGlowSupported
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.glass.LiquidGlassEffectDefaults
import com.nuvio.app.core.ui.glass.LiquidGlassEffectLimits
import com.nuvio.app.core.ui.glass.LiquidGlassEffectSettings
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Keep high-frequency slider draft subscriptions inside the open settings-sheet subtree.
 * Updating a glass effect should not recompose the Appearance settings item behind the modal.
 */
@Composable
internal fun NavigationBarLiquidGlassEffectDrafts(
    content: @Composable (LiquidGlassEffectSettings, LiquidGlassEffectSettings) -> Unit,
) {
    val floatingBarEffects by ThemeSettingsRepository.navBarLiquidGlassBarEffects.collectAsStateWithLifecycle()
    val selectionLensEffects by ThemeSettingsRepository.navBarLiquidGlassSelectionEffects.collectAsStateWithLifecycle()
    content(floatingBarEffects, selectionLensEffects)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun NavigationBarSettingsSheet(
    isTablet: Boolean,
    tabletLayout: Boolean = false,
    selectedStyle: NavBarStyle,
    onStyleSelected: (NavBarStyle) -> Unit,
    glowEnabled: Boolean,
    onGlowChanged: (Boolean) -> Unit,
    defaultIconColorEnabled: Boolean,
    onDefaultIconColorEnabledChanged: (Boolean) -> Unit,
    defaultIconColor: Int,
    onDefaultIconColorChanged: (Int) -> Unit,
    onSaveDefaultIconColor: () -> Unit,
    floatingBarEffects: LiquidGlassEffectSettings,
    onFloatingBarEffectsChanged: (LiquidGlassEffectSettings) -> Unit,
    selectionLensEffects: LiquidGlassEffectSettings,
    onSelectionLensEffectsChanged: (LiquidGlassEffectSettings) -> Unit,
    onSaveLiquidGlassEffectSettings: () -> Unit,
    glassStyle: LiquidGlassStyle = LiquidGlassStyle.Default,
    onGlassStyleChanged: (LiquidGlassStyle) -> Unit = {},
    selectedPosition: NavBarPosition,
    onPositionSelected: (NavBarPosition) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = {
        // Persist synchronously before beginning any suspend/animation work. This covers both
        // explicit Done and platform dismissal callbacks that may immediately dispose the sheet.
        onSaveLiquidGlassEffectSettings()
        onSaveDefaultIconColor()
        scope.launch {
            dismissNuvioBottomSheet(sheetState, onDismiss)
        }
    }
    var defaultIconColorExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    NuvioModalBottomSheet(onDismissRequest = dismiss, sheetState = sheetState) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
        ) {
            stickyHeader(key = "navigation-bar-preview") {
                NavigationBarPreviewStickyHeader(
                    listState = listState,
                    selectedStyle = selectedStyle,
                    isTablet = isTablet,
                    glowEnabled = glowEnabled,
                    selectedPosition = selectedPosition,
                    tabletLayout = tabletLayout,
                    floatingBarEffects = floatingBarEffects,
                    selectionLensEffects = selectionLensEffects,
                    defaultIconColorEnabled = defaultIconColorEnabled,
                    defaultIconColor = defaultIconColor,
                    glassStyle = glassStyle,
                )
            }
            if (!isTablet) {
                NavBarStyle.entries.forEach { style ->
                    item(key = style.key) {
                        NuvioBottomSheetActionRow(
                            title = stringResource(style.labelRes),
                            onClick = { onStyleSelected(style) },
                            selected = style == selectedStyle,
                            modifier = Modifier.semantics {
                                role = Role.RadioButton
                                selected = style == selectedStyle
                            },
                            trailingContent = {
                                if (style == selectedStyle) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                        )
                    }
                }
            }
            item {
                // Only the floating pill can move; CLASSIC keeps its fixed place.
                AnimatedVisibility(selectedStyle != NavBarStyle.CLASSIC) {
                    Column {
                        NuvioBottomSheetDivider()
                        Text(
                            text = stringResource(Res.string.settings_nav_bar_position),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                        )
                        NavBarPosition.entries.forEach { position ->
                            NuvioBottomSheetActionRow(
                                title = stringResource(position.labelRes),
                                onClick = { onPositionSelected(position) },
                                modifier = Modifier.semantics {
                                    role = Role.RadioButton
                                    selected = position == selectedPosition
                                },
                                trailingContent = {
                                    if (position == selectedPosition) {
                                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                            )
                        }
                    }
                }
            }
            item {
                AnimatedVisibility(selectedStyle != NavBarStyle.CLASSIC) {
                    Column {
                        NuvioBottomSheetDivider()
                        SettingsSwitchRow(
                            title = stringResource(Res.string.settings_nav_bar_default_icon_color),
                            description = stringResource(Res.string.settings_nav_bar_default_icon_color_description),
                            checked = defaultIconColorEnabled,
                            isTablet = isTablet,
                            onCheckedChange = { enabled ->
                                if (!enabled) defaultIconColorExpanded = false
                                onDefaultIconColorEnabledChanged(enabled)
                            },
                        )
                    }
                }
            }
            item {
                AnimatedVisibility(
                    visible = selectedStyle != NavBarStyle.CLASSIC && defaultIconColorEnabled,
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) {
                                    defaultIconColorExpanded = !defaultIconColorExpanded
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.settings_nav_bar_default_icon_color_change),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(
                                        if (defaultIconColorExpanded) {
                                            Res.string.settings_nav_bar_default_icon_color_collapse
                                        } else {
                                            Res.string.settings_nav_bar_default_icon_color_expand
                                        },
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        AnimatedVisibility(visible = defaultIconColorExpanded) {
                            NavigationBarIconColorEditor(
                                color = defaultIconColor,
                                isTablet = isTablet,
                                onColorChanged = onDefaultIconColorChanged,
                            )
                        }
                        NuvioBottomSheetDivider()
                    }
                }
            }
            // The glass options (halo, material, effects) moved to the Liquid glass settings sheet.
            item {
                NuvioPrimaryButton(
                    text = stringResource(Res.string.action_done),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    onClick = dismiss,
                )
            }
        }
    }
}


private val LiquidGlassMaterial.labelRes: StringResource
    get() = when (this) {
        LiquidGlassMaterial.LIQUID -> Res.string.settings_nav_bar_glass_material_liquid
        LiquidGlassMaterial.FROSTED -> Res.string.settings_nav_bar_glass_material_frosted
        LiquidGlassMaterial.CLEAR -> Res.string.settings_nav_bar_glass_material_clear
        LiquidGlassMaterial.SOLID -> Res.string.settings_nav_bar_glass_material_solid
        LiquidGlassMaterial.AMOLED -> Res.string.settings_liquid_glass_material_amoled
    }

internal fun navBarGlassShowsLensControls(style: LiquidGlassStyle, refractionSupported: Boolean): Boolean =
    style.usesLens && refractionSupported

@Composable
private fun NavigationBarGlassSettings(
    style: LiquidGlassStyle,
    onStyleChanged: (LiquidGlassStyle) -> Unit,
    floatingBarEffects: LiquidGlassEffectSettings,
    onFloatingBarEffectsChanged: (LiquidGlassEffectSettings) -> Unit,
    selectionLensEffects: LiquidGlassEffectSettings,
    onSelectionLensEffectsChanged: (LiquidGlassEffectSettings) -> Unit,
    isTablet: Boolean,
    /** Title of the effects group; the floating bar's by default. */
    effectsTitle: String? = null,
    /** The selection lens only exists on the navigation bar. */
    showSelectionLens: Boolean = true,
) {
    val refractionSupported = isLiquidRefractionSupported()
    val materials = LiquidGlassMaterial.entries.filter {
        refractionSupported || it != LiquidGlassMaterial.CLEAR
    }
    Column {
        NuvioBottomSheetDivider()
        Text(
            text = stringResource(Res.string.settings_nav_bar_glass),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        )
        materials.forEach { material ->
            NuvioBottomSheetActionRow(
                title = stringResource(material.labelRes),
                onClick = { onStyleChanged(style.copy(material = material)) },
                modifier = Modifier.semantics {
                    role = Role.RadioButton
                    selected = material == style.material
                },
                trailingContent = {
                    if (material == style.material) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        }
        SettingsSwitchRow(
            title = stringResource(Res.string.settings_nav_bar_glass_highlight),
            description = stringResource(Res.string.settings_nav_bar_glass_highlight_description),
            checked = style.edgeHighlight,
            isTablet = isTablet,
            onCheckedChange = { onStyleChanged(style.copy(edgeHighlight = it)) },
        )
        AnimatedVisibility(style.usesBackdrop) {
            Column {
                NuvioBottomSheetDivider()
                LiquidGlassEffectGroupHeader(
                    title = effectsTitle ?: stringResource(Res.string.settings_nav_bar_liquid_glass_floating_bar),
                    resetDescription = stringResource(Res.string.settings_nav_bar_liquid_glass_reset_bar),
                    isTablet = isTablet,
                    onReset = {
                        onFloatingBarEffectsChanged(LiquidGlassEffectDefaults.floatingBar)
                        onStyleChanged(
                            style.copy(
                                vibrancy = LiquidGlassStyle.Default.vibrancy,
                                shadow = LiquidGlassStyle.Default.shadow,
                            ),
                        )
                    },
                )
                LiquidGlassEffectControls(
                    effects = floatingBarEffects,
                    onEffectsChanged = onFloatingBarEffectsChanged,
                    limits = LiquidGlassEffectDefaults.floatingBarLimits,
                    showBlur = style.usesBlur,
                    showLens = navBarGlassShowsLensControls(style, refractionSupported),
                    isTablet = isTablet,
                )
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_nav_bar_glass_vibrancy),
                    description = stringResource(Res.string.settings_nav_bar_glass_vibrancy_description),
                    checked = style.vibrancy,
                    isTablet = isTablet,
                    onCheckedChange = { onStyleChanged(style.copy(vibrancy = it)) },
                )
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_nav_bar_glass_shadow),
                    description = stringResource(Res.string.settings_nav_bar_glass_shadow_description),
                    checked = style.shadow,
                    isTablet = isTablet,
                    onCheckedChange = { onStyleChanged(style.copy(shadow = it)) },
                )
                if (refractionSupported && showSelectionLens) {
                    NuvioBottomSheetDivider()
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_nav_bar_glass_selection_lens),
                        description = stringResource(Res.string.settings_nav_bar_glass_selection_lens_description),
                        checked = style.selectionLens,
                        isTablet = isTablet,
                        onCheckedChange = { onStyleChanged(style.copy(selectionLens = it)) },
                    )
                    AnimatedVisibility(style.selectionLens) {
                        Column {
                            LiquidGlassEffectGroupHeader(
                                title = stringResource(Res.string.settings_nav_bar_liquid_glass_selection_lens),
                                resetDescription = stringResource(Res.string.settings_nav_bar_liquid_glass_reset_lens),
                                isTablet = isTablet,
                                onReset = { onSelectionLensEffectsChanged(LiquidGlassEffectDefaults.selectionLens) },
                            )
                            LiquidGlassEffectControls(
                                effects = selectionLensEffects,
                                onEffectsChanged = onSelectionLensEffectsChanged,
                                limits = LiquidGlassEffectDefaults.selectionLensLimits,
                                showBlur = true,
                                showLens = true,
                                isTablet = isTablet,
                            )
                            SettingsSwitchRow(
                                title = stringResource(Res.string.settings_nav_bar_glass_lens_affect_tab_icons),
                                description = stringResource(Res.string.settings_nav_bar_glass_lens_affect_tab_icons_description),
                                checked = selectionLensEffects.affectTabIcons,
                                isTablet = isTablet,
                                onCheckedChange = {
                                    onSelectionLensEffectsChanged(selectionLensEffects.copy(affectTabIcons = it))
                                },
                            )
                            SettingsSwitchRow(
                                title = stringResource(Res.string.settings_nav_bar_glass_lens_affect_floating_bar),
                                description = stringResource(Res.string.settings_nav_bar_glass_lens_affect_floating_bar_description),
                                checked = selectionLensEffects.affectFloatingBar,
                                isTablet = isTablet,
                                onCheckedChange = {
                                    onSelectionLensEffectsChanged(selectionLensEffects.copy(affectFloatingBar = it))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassEffectControls(
    effects: LiquidGlassEffectSettings,
    onEffectsChanged: (LiquidGlassEffectSettings) -> Unit,
    limits: LiquidGlassEffectLimits,
    showBlur: Boolean,
    showLens: Boolean,
    isTablet: Boolean,
) {
    Column {
        LiquidGlassEffectSlider(
            title = stringResource(Res.string.settings_nav_bar_glass_corner_radius),
            value = effects.cornerRadiusPercent,
            valueRange = 0f..50f,
            steps = 49,
            valueLabel = "${effects.cornerRadiusPercent.roundToInt()}%",
            isTablet = isTablet,
            onValueChange = { onEffectsChanged(effects.copy(cornerRadiusPercent = it)) },
        )
        if (showBlur) {
            LiquidGlassEffectSlider(
                title = stringResource(Res.string.settings_nav_bar_glass_blur_radius),
                value = effects.blurRadiusDp,
                valueRange = 0f..limits.maxBlurRadiusDp,
                steps = limits.maxBlurRadiusDp.roundToInt() - 1,
                valueLabel = "${effects.blurRadiusDp.roundToInt()} dp",
                isTablet = isTablet,
                onValueChange = { onEffectsChanged(effects.copy(blurRadiusDp = it)) },
            )
        }
        if (showLens) {
            LiquidGlassEffectSlider(
                title = stringResource(Res.string.settings_nav_bar_glass_refraction_height),
                value = effects.refractionHeightDp,
                valueRange = 0f..limits.maxRefractionHeightDp,
                steps = limits.maxRefractionHeightDp.roundToInt() - 1,
                valueLabel = "${effects.refractionHeightDp.roundToInt()} dp",
                isTablet = isTablet,
                onValueChange = { onEffectsChanged(effects.copy(refractionHeightDp = it)) },
            )
            LiquidGlassEffectSlider(
                title = stringResource(Res.string.settings_nav_bar_glass_refraction_amount),
                value = effects.refractionAmountDp,
                valueRange = 0f..limits.maxRefractionAmountDp,
                steps = limits.maxRefractionAmountDp.roundToInt() - 1,
                valueLabel = "${effects.refractionAmountDp.roundToInt()} dp",
                isTablet = isTablet,
                onValueChange = { onEffectsChanged(effects.copy(refractionAmountDp = it)) },
            )
        }
        LiquidGlassEffectSlider(
            title = stringResource(Res.string.settings_nav_bar_glass_tint_opacity),
            value = effects.tintOpacity,
            valueRange = 0f..1f,
            steps = 19,
            valueLabel = "${(effects.tintOpacity * 100f).roundToInt()}%",
            isTablet = isTablet,
            onValueChange = { onEffectsChanged(effects.copy(tintOpacity = it)) },
        )
        if (showLens) {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_nav_bar_glass_chromatic_aberration),
                description = stringResource(Res.string.settings_nav_bar_glass_chromatic_aberration_description),
                checked = effects.chromaticAberration,
                isTablet = isTablet,
                onCheckedChange = { onEffectsChanged(effects.copy(chromaticAberration = it)) },
            )
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_nav_bar_glass_depth_effect),
                description = stringResource(Res.string.settings_nav_bar_glass_depth_effect_description),
                checked = effects.depthEffect,
                isTablet = isTablet,
                onCheckedChange = { onEffectsChanged(effects.copy(depthEffect = it)) },
            )
        }
    }
}


@Composable
private fun LiquidGlassEffectGroupHeader(
    title: String,
    resetDescription: String,
    isTablet: Boolean,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isTablet) 20.dp else 16.dp,
                end = 8.dp,
                top = 12.dp,
                bottom = 2.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onReset) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = resetDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


@Composable
private fun NavigationBarPreviewStickyHeader(
    listState: LazyListState,
    selectedStyle: NavBarStyle,
    isTablet: Boolean,
    glowEnabled: Boolean,
    selectedPosition: NavBarPosition,
    tabletLayout: Boolean,
    floatingBarEffects: LiquidGlassEffectSettings,
    selectionLensEffects: LiquidGlassEffectSettings,
    defaultIconColorEnabled: Boolean,
    defaultIconColor: Int,
    glassStyle: LiquidGlassStyle,
    title: String? = null,
) {
    var bounceTrigger by remember { mutableIntStateOf(0) }
    var bounceDirection by remember { mutableStateOf(0f) }

    LaunchedEffect(listState) {
        val gate = NavigationPreviewScrollBounceGate()
        snapshotFlow { listState.canScrollBackward }
            .distinctUntilChanged()
            .collectLatest { canScrollBackward ->
                val direction = gate.onScrollabilityChanged(canScrollBackward)
                    ?: return@collectLatest
                bounceDirection = direction
                bounceTrigger = if (bounceTrigger == Int.MAX_VALUE) 1 else bounceTrigger + 1
            }
    }

    val sheetBackdrop = LocalNuvioSheetBackdrop.current?.backdrop
    val sheetContainerColor = MaterialTheme.nuvio.colors.surfaceSheet

    // Keep the pinned preview opaque during scrolling while matching the shared sheet blur and tint.
    // The gate emits a one-shot request at each top-boundary crossing for the preview bar's JellyMotion springs.
    // Opaque so the rows that scroll under the pinned preview never show through it (they were
    // read cut in half, « Changer la couleur des icônes » under « Touchez la barre… »).
    Column(
        Modifier
            .fillMaxWidth()
            .background(sheetContainerColor.copy(alpha = 1f))
            .drawBehind {
                drawRect(
                    color = Color.White.copy(alpha = 0.10f),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 1.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()),
                )
            }
            .padding(bottom = 8.dp),
    ) {
        Text(
            text = title ?: stringResource(Res.string.settings_appearance_nav_bar_style),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        NavigationBarPreview(
            style = selectedStyle,
            isTablet = isTablet,
            glowEnabled = glowEnabled,
            position = selectedPosition,
            tabletLayout = tabletLayout,
            floatingBarEffects = floatingBarEffects,
            selectionLensEffects = selectionLensEffects,
            scrollBounceTrigger = bounceTrigger,
            scrollBounceDirection = bounceDirection,
            glassStyle = glassStyle,
            defaultIconColorOverride = if (defaultIconColorEnabled) {
                Color(defaultIconColor or 0xFF000000.toInt())
            } else {
                null
            },
        )
    }
}

@Composable
private fun NavigationBarIconColorEditor(
    color: Int,
    isTablet: Boolean,
    onColorChanged: (Int) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    var hexCode by remember(color) { mutableStateOf(formatHexColor(color)) }
    LaunchedEffect(color) {
        hexCode = formatHexColor(color)
    }
    val hexLabel = stringResource(Res.string.custom_theme_hex_title)
    val validHex = parseHexColor(hexCode) != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Reuse the Custom Theme picker so the color channels and swatches stay consistent.
        ThemeColorPicker(
            color = color,
            colorIndex = 0,
            onColorChanged = { updated ->
                onColorChanged(updated)
                hexCode = formatHexColor(updated)
            },
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = hexLabel,
                style = MaterialTheme.typography.labelLarge,
                color = tokens.colors.textPrimary,
            )
            NuvioInputField(
                value = hexCode,
                onValueChange = { value ->
                    hexCode = value
                    parseHexColor(value)?.let(onColorChanged)
                },
                placeholder = "#969CA3",
                modifier = Modifier.semantics { contentDescription = hexLabel },
            )
            if (!validHex) {
                Text(
                    text = stringResource(Res.string.custom_theme_hex_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.danger,
                )
            }
        }
    }
}

@Composable
internal fun LiquidGlassEffectSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    isTablet: Boolean,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
) {
    val safeValue = value.takeIf { it.isFinite() }?.coerceIn(valueRange) ?: valueRange.start
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isTablet) 20.dp else 16.dp,
                vertical = 2.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = safeValue,
            onValueChange = { onValueChange(it.coerceIn(valueRange)) },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}


/**
 * « Liquid glass »: the one set of glass settings of the app (glass halo, material, edge, blur,
 * refraction, tint, shadow, selection lens) and, at the top, the screens it applies to. A screen
 * left unticked keeps its plain look. The pinned preview shows the navigation bar live.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun LiquidGlassSettingsSheet(
    isTablet: Boolean,
    tabletLayout: Boolean,
    selectedStyle: NavBarStyle,
    selectedPosition: NavBarPosition,
    glowEnabled: Boolean,
    onGlowChanged: (Boolean) -> Unit,
    defaultIconColorEnabled: Boolean,
    defaultIconColor: Int,
    floatingBarEffects: LiquidGlassEffectSettings,
    onFloatingBarEffectsChanged: (LiquidGlassEffectSettings) -> Unit,
    selectionLensEffects: LiquidGlassEffectSettings,
    onSelectionLensEffectsChanged: (LiquidGlassEffectSettings) -> Unit,
    onSaveLiquidGlassEffectSettings: () -> Unit,
    glassStyle: LiquidGlassStyle,
    onGlassStyleChanged: (LiquidGlassStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = {
        onSaveLiquidGlassEffectSettings()
        scope.launch { dismissNuvioBottomSheet(sheetState, onDismiss) }
    }
    val listState = rememberLazyListState()
    NuvioModalBottomSheet(onDismissRequest = dismiss, sheetState = sheetState) {
        val surfaceSettings by ThemeSettingsRepository.glassSurfaceSettings.collectAsStateWithLifecycle()
        var tabKey by rememberSaveable { mutableStateOf(GlassSurface.NAVIGATION_BAR.key) }
        val tab = GlassSurface.fromKey(tabKey) ?: GlassSurface.NAVIGATION_BAR
        val tabLabel = stringResource(tab.labelRes)
        LazyColumn(modifier = Modifier.fillMaxWidth(), state = listState) {
            // Title, the element tabs and a live preview of the element chosen, pinned on top.
            stickyHeader(key = "liquid-glass-header") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.nuvio.colors.surfaceSheet.copy(alpha = 1f))
                        .padding(bottom = 10.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.settings_liquid_glass_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                    GlassSurfaceTabs(selected = tab, onSelect = { tabKey = it.key })
                    Spacer(Modifier.height(10.dp))
                    if (tab == GlassSurface.NAVIGATION_BAR) {
                        NavigationBarPreview(
                            style = selectedStyle,
                            isTablet = isTablet,
                            glowEnabled = glowEnabled,
                            position = selectedPosition,
                            tabletLayout = tabletLayout,
                            floatingBarEffects = floatingBarEffects,
                            selectionLensEffects = selectionLensEffects,
                            glassStyle = glassStyle.forNavigationBar(),
                            defaultIconColorOverride = if (defaultIconColorEnabled) {
                                Color(defaultIconColor or 0xFF000000.toInt())
                            } else {
                                null
                            },
                        )
                    } else {
                        GlassSurfacePreview(surface = tab, label = tabLabel)
                    }
                }
            }
            if (!isLiquidGlassSupported()) {
                item(key = "inactive") {
                    Text(
                        text = stringResource(Res.string.settings_liquid_glass_inactive),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
            item(key = "enabled-$tabKey") {
                val enabled = tab in glassStyle.surfaces
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_liquid_glass_enable, tabLabel),
                    description = stringResource(Res.string.settings_liquid_glass_apply_to_hint),
                    checked = enabled,
                    isTablet = isTablet,
                    onCheckedChange = { on ->
                        onGlassStyleChanged(
                            glassStyle.copy(surfaces = if (on) glassStyle.surfaces + tab else glassStyle.surfaces - tab),
                        )
                    },
                )
            }
            if (tab == GlassSurface.NAVIGATION_BAR) {
                if (floatingNavigationGlowSupported) {
                    item(key = "glow-nav") {
                        Column {
                            NuvioBottomSheetDivider()
                            SettingsSwitchRow(
                                title = stringResource(Res.string.settings_nav_bar_glow),
                                description = stringResource(Res.string.settings_nav_bar_glow_description),
                                checked = glowEnabled,
                                isTablet = isTablet,
                                onCheckedChange = onGlowChanged,
                            )
                        }
                    }
                }
                item(key = "glass-nav") {
                    NavigationBarGlassSettings(
                        style = glassStyle,
                        onStyleChanged = onGlassStyleChanged,
                        floatingBarEffects = floatingBarEffects,
                        onFloatingBarEffectsChanged = onFloatingBarEffectsChanged,
                        selectionLensEffects = selectionLensEffects,
                        onSelectionLensEffectsChanged = onSelectionLensEffectsChanged,
                        isTablet = isTablet,
                    )
                }
            } else {
                val current = surfaceSettings[tab] ?: GlassSurfaceSettings()
                item(key = "glow-$tabKey") {
                    Column {
                        NuvioBottomSheetDivider()
                        SettingsSwitchRow(
                            title = stringResource(Res.string.settings_nav_bar_glow),
                            description = stringResource(Res.string.settings_liquid_glass_glow_description),
                            checked = current.glow,
                            isTablet = isTablet,
                            onCheckedChange = { on -> ThemeSettingsRepository.updateGlassSurfaceSettings(tab) { it.copy(glow = on) } },
                        )
                    }
                }
                item(key = "glass-$tabKey") {
                    NavigationBarGlassSettings(
                        style = current.asStyle(glassStyle),
                        onStyleChanged = { style ->
                            ThemeSettingsRepository.updateGlassSurfaceSettings(tab) { it.withStyle(style) }
                        },
                        floatingBarEffects = current.effects,
                        onFloatingBarEffectsChanged = { effects ->
                            ThemeSettingsRepository.updateGlassSurfaceSettings(tab) {
                                it.copy(
                                    effects = effects.sanitized(
                                        LiquidGlassEffectDefaults.floatingBar,
                                        LiquidGlassEffectDefaults.floatingBarLimits,
                                    ),
                                )
                            }
                        },
                        selectionLensEffects = LiquidGlassEffectDefaults.selectionLens,
                        onSelectionLensEffectsChanged = {},
                        isTablet = isTablet,
                        effectsTitle = tabLabel,
                        showSelectionLens = false,
                    )
                }
            }
            item(key = "done") {
                NuvioPrimaryButton(
                    text = stringResource(Res.string.action_done),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    onClick = dismiss,
                )
            }
        }
    }
}

private val GlassSurface.labelRes: StringResource
    get() = when (this) {
        GlassSurface.NAVIGATION_BAR -> Res.string.settings_liquid_glass_surface_nav
        GlassSurface.HOME -> Res.string.settings_liquid_glass_surface_home
        GlassSurface.RESUME_CARD -> Res.string.settings_liquid_glass_surface_resume
        GlassSurface.DETAILS -> Res.string.settings_liquid_glass_surface_details
        GlassSurface.SOURCES -> Res.string.settings_liquid_glass_surface_sources
        GlassSurface.PLAYER -> Res.string.settings_liquid_glass_surface_player
        GlassSurface.PLAYER_PANELS -> Res.string.settings_liquid_glass_surface_player_panels
        GlassSurface.PLAYER_CARDS -> Res.string.settings_liquid_glass_surface_player_cards
        GlassSurface.CONTEXT_MENU -> Res.string.settings_liquid_glass_surface_menu
        GlassSurface.PROFILE_MENU -> Res.string.settings_liquid_glass_surface_profile
        GlassSurface.POPUPS -> Res.string.settings_liquid_glass_surface_popups
    }


/** « Barre · Accueil · Sources · Lecteur · Fenêtres »: which element the settings below change. */
@Composable
private fun GlassSurfaceTabs(selected: GlassSurface, onSelect: (GlassSurface) -> Unit) {
    val pill = RoundedCornerShape(percent = 50)
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(GlassSurface.entries.size) { index ->
            val surface = GlassSurface.entries[index]
            val isSelected = surface == selected
            Text(
                text = stringResource(surface.shortLabelRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(pill)
                    .background(if (isSelected) Color(0xFFF2F2F7) else Color.White.copy(alpha = 0.08f))
                    .clickable { onSelect(surface) }
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

/** A glass card over coloured tiles, drawn with the settings of [surface]. */
@Composable
private fun GlassSurfacePreview(surface: GlassSurface, label: String) {
    val tokens = MaterialTheme.nuvio
    val backdrop = rememberNuvioBackdrop()
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(156.dp)
            .clip(tokens.shapes.card),
    ) {
        Column(
            Modifier
                .matchParentSize()
                .nuvioBackdropSource(backdrop)
                .background(tokens.colors.surfaceCard)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(3) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) { column ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(tokens.shapes.compactCard)
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(
                                            tokens.colors.accent.copy(alpha = 0.25f + (row + column) * 0.12f),
                                            Color(0xFF2F4F9E).copy(alpha = 0.6f),
                                        ),
                                    ),
                                ),
                        )
                    }
                }
            }
        }
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .liquidGlassSurface(
                    backdrop = backdrop,
                    shape = shape,
                    tint = Color(0xFF0E0E12).copy(alpha = 0.5f),
                    fallback = Color(0xFF16161A),
                    blurRadius = 16.dp,
                    refractionHeight = 10.dp,
                    refractionAmount = 16.dp,
                    surface = surface,
                )
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                stringResource(Res.string.settings_liquid_glass_preview_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
            )
        }
    }
}

private val GlassSurface.shortLabelRes: StringResource
    get() = when (this) {
        GlassSurface.NAVIGATION_BAR -> Res.string.settings_liquid_glass_tab_nav
        GlassSurface.HOME -> Res.string.settings_liquid_glass_surface_home
        GlassSurface.RESUME_CARD -> Res.string.settings_liquid_glass_tab_resume
        GlassSurface.DETAILS -> Res.string.settings_liquid_glass_tab_details
        GlassSurface.SOURCES -> Res.string.settings_liquid_glass_tab_sources
        GlassSurface.PLAYER -> Res.string.settings_liquid_glass_tab_player
        GlassSurface.PLAYER_PANELS -> Res.string.settings_liquid_glass_tab_player_panels
        GlassSurface.PLAYER_CARDS -> Res.string.settings_liquid_glass_tab_player_cards
        GlassSurface.CONTEXT_MENU -> Res.string.settings_liquid_glass_tab_menu
        GlassSurface.PROFILE_MENU -> Res.string.settings_liquid_glass_tab_profile
        GlassSurface.POPUPS -> Res.string.settings_liquid_glass_tab_popups
    }
