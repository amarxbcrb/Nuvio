package com.nuvio.app.features.settings

import com.nuvio.app.core.ui.AppTheme
import com.nuvio.app.core.ui.CustomThemeColors
import com.nuvio.app.core.ui.NativeTabBridge
import com.nuvio.app.core.ui.ThemeColors
import com.nuvio.app.core.ui.glass.LiquidGlassSettings
import com.nuvio.app.core.ui.glass.LiquidGlassStyle
import com.nuvio.app.core.ui.glass.LiquidGlassEffectDefaults
import com.nuvio.app.core.ui.glass.LiquidGlassEffectSettings
import com.nuvio.app.core.ui.glass.decodeLiquidGlassEffectSettings
import com.nuvio.app.core.ui.glass.encodeLiquidGlassEffectSettings
import com.nuvio.app.core.ui.glass.migrateLegacyNavBarGlassEffects
import com.nuvio.app.core.ui.formatHexColor
import com.nuvio.app.core.ui.parseHexColor
import com.nuvio.app.core.ui.nativeAccentGradientHex
import com.nuvio.app.features.membership.MemberAccessRepository
import com.nuvio.app.features.membership.resolveCustomThemeColors
import com.nuvio.app.features.membership.resolveAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal const val DEFAULT_NAV_BAR_ICON_COLOR = 0x969CA3

object ThemeSettingsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _selectedThemePreference = MutableStateFlow<AppTheme?>(null)
    val selectedThemePreference: StateFlow<AppTheme?> = _selectedThemePreference.asStateFlow()
    private val _selectedTheme = MutableStateFlow(AppTheme.WHITE)
    val selectedTheme: StateFlow<AppTheme> = _selectedTheme.asStateFlow()

    private val _customThemePreference = MutableStateFlow(CustomThemeColors.Default)
    val customThemePreference: StateFlow<CustomThemeColors> = _customThemePreference.asStateFlow()
    private val _customThemeColors = MutableStateFlow(CustomThemeColors.solid(CustomThemeColors.Default.second))
    val customThemeColors: StateFlow<CustomThemeColors> = _customThemeColors.asStateFlow()

    private val _amoledEnabled = MutableStateFlow(false)
    val amoledEnabled: StateFlow<Boolean> = _amoledEnabled.asStateFlow()

    private val _tabBarBehavior = MutableStateFlow(NuvioTabBarBehavior.Default)
    val tabBarBehavior: StateFlow<NuvioTabBarBehavior> = _tabBarBehavior.asStateFlow()

    private val _liquidGlassNativeTabBarEnabled = MutableStateFlow(NuvioTabBarBehavior.Default.isEnabled)
    val liquidGlassNativeTabBarEnabled: StateFlow<Boolean> = _liquidGlassNativeTabBarEnabled.asStateFlow()

    private val _dynamicArtworkBackgroundEnabled = MutableStateFlow(false)
    val dynamicArtworkBackgroundEnabled: StateFlow<Boolean> = _dynamicArtworkBackgroundEnabled.asStateFlow()

    private val _showCatalogAccentEnabled = MutableStateFlow(false)
    val showCatalogAccentEnabled: StateFlow<Boolean> = _showCatalogAccentEnabled.asStateFlow()

    private val _selectedAppLanguage = MutableStateFlow(AppLanguage.DEVICE)
    val selectedAppLanguage: StateFlow<AppLanguage> = _selectedAppLanguage.asStateFlow()

    private val _navBarStyle = MutableStateFlow(NavBarStyle.ADAPTIVE)
    val navBarStyle: StateFlow<NavBarStyle> = _navBarStyle.asStateFlow()

    private val _navBarPosition = MutableStateFlow(NavBarPosition.BOTTOM)
    val navBarPosition: StateFlow<NavBarPosition> = _navBarPosition.asStateFlow()

    private val _navBarGlowEnabled = MutableStateFlow(true)
    val navBarGlowEnabled: StateFlow<Boolean> = _navBarGlowEnabled.asStateFlow()

    private val _navBarDefaultIconColorEnabled = MutableStateFlow(false)
    val navBarDefaultIconColorEnabled: StateFlow<Boolean> = _navBarDefaultIconColorEnabled.asStateFlow()

    private val _navBarDefaultIconColor = MutableStateFlow(DEFAULT_NAV_BAR_ICON_COLOR)
    val navBarDefaultIconColor: StateFlow<Int> = _navBarDefaultIconColor.asStateFlow()

    private val _navBarDefaultIconColorRevision = MutableStateFlow(0L)
    internal val navBarDefaultIconColorRevision: StateFlow<Long> = _navBarDefaultIconColorRevision.asStateFlow()
    private var navBarDefaultIconColorDraftDirty = false

    private val _navBarLiquidGlassBarEffects = MutableStateFlow(LiquidGlassEffectDefaults.floatingBar)
    internal val navBarLiquidGlassBarEffects: StateFlow<LiquidGlassEffectSettings> = _navBarLiquidGlassBarEffects.asStateFlow()

    private val _navBarLiquidGlassSelectionEffects = MutableStateFlow(LiquidGlassEffectDefaults.selectionLens)
    internal val navBarLiquidGlassSelectionEffects: StateFlow<LiquidGlassEffectSettings> = _navBarLiquidGlassSelectionEffects.asStateFlow()

    private val _navBarLiquidGlassEffectsRevision = MutableStateFlow(0L)
    internal val navBarLiquidGlassEffectsRevision: StateFlow<Long> = _navBarLiquidGlassEffectsRevision.asStateFlow()

    private val _navBarGlass = MutableStateFlow(LiquidGlassStyle.Default)
    val navBarGlass: StateFlow<LiquidGlassStyle> = _navBarGlass.asStateFlow()

    /** Liquid glass settings of each element other than the navigation bar. */
    private val _glassSurfaceSettings =
        MutableStateFlow<Map<com.nuvio.app.core.ui.glass.GlassSurface, com.nuvio.app.core.ui.glass.GlassSurfaceSettings>>(emptyMap())
    internal val glassSurfaceSettings: StateFlow<Map<com.nuvio.app.core.ui.glass.GlassSurface, com.nuvio.app.core.ui.glass.GlassSurfaceSettings>> =
        _glassSurfaceSettings.asStateFlow()

    internal fun updateGlassSurfaceSettings(
        surface: com.nuvio.app.core.ui.glass.GlassSurface,
        transform: (com.nuvio.app.core.ui.glass.GlassSurfaceSettings) -> com.nuvio.app.core.ui.glass.GlassSurfaceSettings,
    ) {
        ensureLoaded()
        val current = _glassSurfaceSettings.value[surface] ?: com.nuvio.app.core.ui.glass.GlassSurfaceSettings()
        val updated = _glassSurfaceSettings.value + (surface to transform(current))
        if (updated == _glassSurfaceSettings.value) return
        _glassSurfaceSettings.value = updated
        com.nuvio.app.core.ui.glass.GlassAppearance.surfaceSettings = updated
        ThemeSettingsStorage.saveGlassSurfaces(com.nuvio.app.core.ui.glass.encodeGlassSurfaceSettings(updated))
    }

    private val _liquidGlassEffectsEnabled = MutableStateFlow(true)
    val liquidGlassEffectsEnabled: StateFlow<Boolean> = _liquidGlassEffectsEnabled.asStateFlow()

    private var hasLoaded = false
    private var observesMembership = false

    fun ensureLoaded() {
        observeMembership()
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        _selectedThemePreference.value = null
        _selectedTheme.value = AppTheme.WHITE
        _customThemePreference.value = CustomThemeColors.Default
        _customThemeColors.value = CustomThemeColors.solid(CustomThemeColors.Default.second)
        _amoledEnabled.value = true
        _tabBarBehavior.value = NuvioTabBarBehavior.Default
        _liquidGlassNativeTabBarEnabled.value = NuvioTabBarBehavior.Default.isEnabled
        _dynamicArtworkBackgroundEnabled.value = false
        _showCatalogAccentEnabled.value = false
        NativeTabBridge.publishAccentColor(
            hexColor = AppTheme.WHITE.nativeTabAccentHex(),
            gradientHexColors = ThemeColors.getColorPalette(AppTheme.WHITE).nativeAccentGradientHex(),
        )
        NativeTabBridge.publishTabBarBehavior(NuvioTabBarBehavior.Default)
        NativeTabBridge.publishLiquidGlassEnabled(NuvioTabBarBehavior.Default.isEnabled)
        _selectedAppLanguage.value = AppLanguage.DEVICE
        _navBarGlowEnabled.value = true
        com.nuvio.app.core.ui.glass.GlassAppearance.glow = _navBarGlowEnabled.value
        _navBarDefaultIconColorEnabled.value = false
        _navBarDefaultIconColor.value = DEFAULT_NAV_BAR_ICON_COLOR
        navBarDefaultIconColorDraftDirty = false
        _navBarLiquidGlassBarEffects.value = LiquidGlassEffectDefaults.floatingBar
        _navBarLiquidGlassSelectionEffects.value = LiquidGlassEffectDefaults.selectionLens
        _navBarStyle.value = NavBarStyle.ADAPTIVE
        _navBarPosition.value = NavBarPosition.BOTTOM
        _navBarGlass.value = LiquidGlassStyle.Default
        com.nuvio.app.core.ui.glass.GlassAppearance.style = _navBarGlass.value
        _glassSurfaceSettings.value = emptyMap()
        com.nuvio.app.core.ui.glass.GlassAppearance.surfaceSettings = emptyMap()
        applyLiquidGlassEffectsEnabled(true)
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val stored = ThemeSettingsStorage.loadSelectedTheme()
        val theme = if (stored != null) {
            try {
                AppTheme.valueOf(stored)
            } catch (_: IllegalArgumentException) {
                null
            }
        } else {
            null
        }
        _selectedThemePreference.value = theme
        _customThemePreference.value = CustomThemeColors.decode(ThemeSettingsStorage.loadCustomThemeColors())
        applyEffectiveTheme()
        migrateAppearanceAxes()
        val behavior = NuvioTabBarBehavior.fromKey(ThemeSettingsStorage.loadTabBarBehavior())
            ?: NuvioTabBarBehavior.fromLegacyEnabled(
                ThemeSettingsStorage.loadLiquidGlassNativeTabBarEnabled(),
            )
        applyTabBarBehavior(behavior)
        _showCatalogAccentEnabled.value =
            ThemeSettingsStorage.loadShowCatalogAccentEnabled() ?: false
        val appLanguage = AppLanguage.fromCode(ThemeSettingsStorage.loadSelectedAppLanguage())
        ThemeSettingsStorage.applySelectedAppLanguage(appLanguage.code)
        _selectedAppLanguage.value = appLanguage
        _navBarGlowEnabled.value = ThemeSettingsStorage.loadNavBarGlowEnabled() ?: true
        com.nuvio.app.core.ui.glass.GlassAppearance.glow = _navBarGlowEnabled.value
        _navBarStyle.value = NavBarStyle.fromKey(ThemeSettingsStorage.loadNavBarStyle())
        _navBarPosition.value = NavBarPosition.fromKey(ThemeSettingsStorage.loadNavBarPosition())
        _navBarDefaultIconColorEnabled.value = ThemeSettingsStorage.loadNavBarDefaultIconColorEnabled() ?: false
        _navBarDefaultIconColor.value = ThemeSettingsStorage.loadNavBarDefaultIconColor()
            ?.let(::parseHexColor) ?: DEFAULT_NAV_BAR_ICON_COLOR
        navBarDefaultIconColorDraftDirty = false
        _navBarLiquidGlassBarEffects.value = decodeLiquidGlassEffectSettings(
            ThemeSettingsStorage.loadNavBarLiquidGlassBarEffects(),
            LiquidGlassEffectDefaults.floatingBar,
            LiquidGlassEffectDefaults.floatingBarLimits,
        )
        _navBarLiquidGlassSelectionEffects.value = decodeLiquidGlassEffectSettings(
            ThemeSettingsStorage.loadNavBarLiquidGlassSelectionEffects(),
            LiquidGlassEffectDefaults.selectionLens,
            LiquidGlassEffectDefaults.selectionLensLimits,
        )
        val storedNavBarGlass = ThemeSettingsStorage.loadNavBarGlass()
        _navBarGlass.value = LiquidGlassStyle.decode(storedNavBarGlass)
        com.nuvio.app.core.ui.glass.GlassAppearance.style = _navBarGlass.value
        _glassSurfaceSettings.value = com.nuvio.app.core.ui.glass.decodeGlassSurfaceSettings(ThemeSettingsStorage.loadGlassSurfaces())
        com.nuvio.app.core.ui.glass.GlassAppearance.surfaceSettings = _glassSurfaceSettings.value
        migrateLegacyNavBarGlass(storedNavBarGlass)
        applyLiquidGlassEffectsEnabled(ThemeSettingsStorage.loadLiquidGlassEffectsEnabled() ?: true)
    }

    fun setCustomTheme(colors: CustomThemeColors) {
        ensureLoaded()
        val selectedColors = resolveCustomThemeColors(
            colors,
            MemberAccessRepository.access.value.tier,
        )
        ThemeSettingsStorage.saveCustomThemeColors(selectedColors.encode())
        ThemeSettingsStorage.saveSelectedTheme(AppTheme.CUSTOM.name)
        _customThemePreference.value = selectedColors
        _selectedThemePreference.value = AppTheme.CUSTOM
        applyEffectiveTheme()
    }

    fun setTheme(theme: AppTheme) {
        ensureLoaded()
        if (_selectedThemePreference.value == theme) return
        _selectedThemePreference.value = theme
        ThemeSettingsStorage.saveSelectedTheme(theme.name)
        applyEffectiveTheme()
    }

    /**
     * Appearance is two independent axes, all four combinations valid:
     *  - Style: liquid glass or opaque  -> [liquidGlassEffectsEnabled] ([setLiquidGlassEffectsEnabled]).
     *  - Fond:  AMOLED black or dynamic -> [setBackgroundMode].
     * AMOLED black: pure #000 background, no artwork tint. Dynamic: the theme background tinted by
     * the artwork, as before the redesign. The old switches are gone; their stored keys are still
     * read and written so backups, sync and older builds keep working.
     */
    fun setBackgroundMode(dynamic: Boolean) {
        setDynamicArtworkBackground(dynamic)
        setAmoledInternal(!dynamic)
    }

    private fun setAmoledInternal(enabled: Boolean) {
        if (_amoledEnabled.value == enabled) return
        _amoledEnabled.value = enabled
        ThemeSettingsStorage.saveAmoledEnabled(enabled)
    }

    /**
     * Settings saved by older builds could mix both switches. The dynamic flag decides the Fond
     * axis and the AMOLED flag follows it, so a dynamic user keeps the tinted theme background
     * and an AMOLED user keeps pure black.
     */
    private fun migrateAppearanceAxes() {
        val dynamic = ThemeSettingsStorage.loadDynamicArtworkBackgroundEnabled() ?: false
        _dynamicArtworkBackgroundEnabled.value = dynamic
        val amoled = !dynamic
        if (ThemeSettingsStorage.loadAmoledEnabled() != amoled) ThemeSettingsStorage.saveAmoledEnabled(amoled)
        _amoledEnabled.value = amoled
    }

    fun setTabBarBehavior(behavior: NuvioTabBarBehavior) {
        ensureLoaded()
        if (_tabBarBehavior.value == behavior) return
        applyTabBarBehavior(behavior)
        ThemeSettingsStorage.saveTabBarBehavior(behavior.key)
        if (!ThemeSettingsStorage.isTabBarBehaviorDeviceLocal()) {
            ThemeSettingsStorage.saveLiquidGlassNativeTabBarEnabled(behavior.isEnabled)
        }
    }

    private fun applyTabBarBehavior(behavior: NuvioTabBarBehavior) {
        _tabBarBehavior.value = behavior
        _liquidGlassNativeTabBarEnabled.value = behavior.isEnabled
        NativeTabBridge.publishTabBarBehavior(behavior)
        NativeTabBridge.publishLiquidGlassEnabled(behavior.isEnabled)
    }

    fun setDynamicArtworkBackground(enabled: Boolean) {
        ensureLoaded()
        if (_dynamicArtworkBackgroundEnabled.value == enabled) return
        _dynamicArtworkBackgroundEnabled.value = enabled
        ThemeSettingsStorage.saveDynamicArtworkBackgroundEnabled(enabled)
    }

    fun setShowCatalogAccent(enabled: Boolean) {
        ensureLoaded()
        if (_showCatalogAccentEnabled.value == enabled) return
        _showCatalogAccentEnabled.value = enabled
        ThemeSettingsStorage.saveShowCatalogAccentEnabled(enabled)
    }

    fun setAppLanguage(language: AppLanguage) {
        ensureLoaded()
        if (_selectedAppLanguage.value == language) return
        ThemeSettingsStorage.saveSelectedAppLanguage(language.code)
        ThemeSettingsStorage.applySelectedAppLanguage(language.code)
        _selectedAppLanguage.value = language
    }

    fun setNavBarStyle(style: NavBarStyle) {
        ensureLoaded()
        if (_navBarStyle.value == style) return
        _navBarStyle.value = style
        ThemeSettingsStorage.saveNavBarStyle(style.key)
    }

    fun setNavBarPosition(position: NavBarPosition) {
        ensureLoaded()
        if (_navBarPosition.value == position) return
        _navBarPosition.value = position
        ThemeSettingsStorage.saveNavBarPosition(position.key)
    }

    fun setNavBarGlowEnabled(enabled: Boolean) {
        ensureLoaded()
        if (_navBarGlowEnabled.value == enabled) return
        _navBarGlowEnabled.value = enabled
        com.nuvio.app.core.ui.glass.GlassAppearance.glow = _navBarGlowEnabled.value
        ThemeSettingsStorage.saveNavBarGlowEnabled(enabled)
    }

    fun setNavBarDefaultIconColorEnabled(enabled: Boolean) {
        ensureLoaded()
        if (_navBarDefaultIconColorEnabled.value == enabled) return
        ThemeSettingsStorage.saveNavBarDefaultIconColorEnabled(enabled)
        _navBarDefaultIconColorEnabled.value = enabled
    }

    internal fun updateNavBarDefaultIconColor(color: Int) {
        ensureLoaded()
        val safeColor = color.coerceIn(0, 0xFFFFFF)
        if (_navBarDefaultIconColor.value == safeColor) return
        _navBarDefaultIconColor.value = safeColor
        navBarDefaultIconColorDraftDirty = true
    }

    internal fun saveNavBarDefaultIconColorSettings() {
        ensureLoaded()
        if (!navBarDefaultIconColorDraftDirty) return
        ThemeSettingsStorage.saveNavBarDefaultIconColor(formatHexColor(_navBarDefaultIconColor.value))
        navBarDefaultIconColorDraftDirty = false
        _navBarDefaultIconColorRevision.value += 1L
    }

    private fun migrateLegacyNavBarGlass(raw: String?) {
        val (bar, lens) = migrateLegacyNavBarGlassEffects(raw) ?: return
        if (ThemeSettingsStorage.loadNavBarLiquidGlassBarEffects() == null) {
            _navBarLiquidGlassBarEffects.value = bar
            ThemeSettingsStorage.saveNavBarLiquidGlassBarEffects(
                encodeLiquidGlassEffectSettings(bar, LiquidGlassEffectDefaults.floatingBar, LiquidGlassEffectDefaults.floatingBarLimits),
            )
        }
        if (ThemeSettingsStorage.loadNavBarLiquidGlassSelectionEffects() == null) {
            _navBarLiquidGlassSelectionEffects.value = lens
            ThemeSettingsStorage.saveNavBarLiquidGlassSelectionEffects(
                encodeLiquidGlassEffectSettings(lens, LiquidGlassEffectDefaults.selectionLens, LiquidGlassEffectDefaults.selectionLensLimits),
            )
        }
        ThemeSettingsStorage.saveNavBarGlass(_navBarGlass.value.encode())
    }

    internal fun updateNavBarLiquidGlassBarEffects(settings: LiquidGlassEffectSettings) {
        ensureLoaded()
        _navBarLiquidGlassBarEffects.value = settings.sanitized(
            LiquidGlassEffectDefaults.floatingBar,
            LiquidGlassEffectDefaults.floatingBarLimits,
        )
    }

    internal fun updateNavBarLiquidGlassSelectionEffects(settings: LiquidGlassEffectSettings) {
        ensureLoaded()
        _navBarLiquidGlassSelectionEffects.value = settings.sanitized(
            LiquidGlassEffectDefaults.selectionLens,
            LiquidGlassEffectDefaults.selectionLensLimits,
        )
    }

    internal fun saveNavBarLiquidGlassEffectSettings() {
        ensureLoaded()
        val barEffects = _navBarLiquidGlassBarEffects.value.sanitized(
            LiquidGlassEffectDefaults.floatingBar,
            LiquidGlassEffectDefaults.floatingBarLimits,
        )
        val selectionEffects = _navBarLiquidGlassSelectionEffects.value.sanitized(
            LiquidGlassEffectDefaults.selectionLens,
            LiquidGlassEffectDefaults.selectionLensLimits,
        )
        val persistedBarEffects = decodeLiquidGlassEffectSettings(
            ThemeSettingsStorage.loadNavBarLiquidGlassBarEffects(),
            LiquidGlassEffectDefaults.floatingBar,
            LiquidGlassEffectDefaults.floatingBarLimits,
        )
        val persistedSelectionEffects = decodeLiquidGlassEffectSettings(
            ThemeSettingsStorage.loadNavBarLiquidGlassSelectionEffects(),
            LiquidGlassEffectDefaults.selectionLens,
            LiquidGlassEffectDefaults.selectionLensLimits,
        )
        var changed = false

        if (barEffects != persistedBarEffects) {
            ThemeSettingsStorage.saveNavBarLiquidGlassBarEffects(
                encodeLiquidGlassEffectSettings(
                    barEffects,
                    LiquidGlassEffectDefaults.floatingBar,
                    LiquidGlassEffectDefaults.floatingBarLimits,
                ),
            )
            changed = true
        }
        if (selectionEffects != persistedSelectionEffects) {
            ThemeSettingsStorage.saveNavBarLiquidGlassSelectionEffects(
                encodeLiquidGlassEffectSettings(
                    selectionEffects,
                    LiquidGlassEffectDefaults.selectionLens,
                    LiquidGlassEffectDefaults.selectionLensLimits,
                ),
            )
            changed = true
        }

        if (changed) _navBarLiquidGlassEffectsRevision.value += 1L
    }

    fun setNavBarGlass(style: LiquidGlassStyle) {
        ensureLoaded()
        if (_navBarGlass.value == style) return
        _navBarGlass.value = style
        com.nuvio.app.core.ui.glass.GlassAppearance.style = _navBarGlass.value
        ThemeSettingsStorage.saveNavBarGlass(style.encode())
    }

    fun setLiquidGlassEffectsEnabled(enabled: Boolean) {
        ensureLoaded()
        if (_liquidGlassEffectsEnabled.value == enabled) return
        applyLiquidGlassEffectsEnabled(enabled)
        ThemeSettingsStorage.saveLiquidGlassEffectsEnabled(enabled)
    }

    private fun applyLiquidGlassEffectsEnabled(enabled: Boolean) {
        _liquidGlassEffectsEnabled.value = enabled
        LiquidGlassSettings.enabled = enabled
    }

    private fun observeMembership() {
        if (observesMembership) return
        observesMembership = true
        MemberAccessRepository.ensureStarted()
        scope.launch {
            MemberAccessRepository.access.collect {
                if (hasLoaded) applyEffectiveTheme()
            }
        }
    }

    private fun applyEffectiveTheme() {
        val access = MemberAccessRepository.access.value
        val effective = resolveAppTheme(
            selectedTheme = _selectedThemePreference.value,
            entitlements = access.entitlements,
        )
        _customThemeColors.value = resolveCustomThemeColors(_customThemePreference.value, access.tier)
        _selectedTheme.value = effective
        val palette = ThemeColors.getColorPalette(effective, _customThemeColors.value)
        NativeTabBridge.publishAccentColor(
            hexColor = palette.nativeAccentHex,
            gradientHexColors = palette.nativeAccentGradientHex(),
        )
    }
}

private fun AppTheme.nativeTabAccentHex(): String =
    ThemeColors.getColorPalette(this).nativeAccentHex
