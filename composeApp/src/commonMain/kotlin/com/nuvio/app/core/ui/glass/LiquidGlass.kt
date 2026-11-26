package com.nuvio.app.core.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.opacity
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.Shadow

object LiquidGlassSettings {
    var enabled: Boolean by mutableStateOf(true)
}

/** Screens the Liquid glass settings can be applied to (« Appliquer à » in the settings). */
enum class GlassSurface(val key: String) {
    NAVIGATION_BAR("nav"),
    HOME("home"),
    SOURCES("sources"),
    PLAYER("player"),
    POPUPS("popups"),
    ;

    companion object {
        fun fromKey(key: String): GlassSurface? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Which screen the glass below belongs to. Screens provide it at their root; surfaces that are
 * not inside one of the listed screens (null) always follow the Liquid glass style.
 */
val LocalGlassSurface = staticCompositionLocalOf<GlassSurface?> { null }

/**
 * The one set of Liquid glass settings (material, edge, shadow, glass halo, blur, refraction,
 * tint) and the screens it applies to. A screen left unticked keeps the plain, opaque look.
 * Kept in sync by ThemeSettingsRepository.
 */
object GlassAppearance {
    var style: LiquidGlassStyle by mutableStateOf(LiquidGlassStyle.Default)
    var glow: Boolean by mutableStateOf(true)
    internal var effects: LiquidGlassEffectSettings by mutableStateOf(LiquidGlassEffectDefaults.floatingBar)

    /** Settings of each element other than the navigation bar (« Liquid glass » tabs). */
    internal var surfaceSettings: Map<GlassSurface, GlassSurfaceSettings> by mutableStateOf(emptyMap())

    internal fun settingsFor(surface: GlassSurface): GlassSurfaceSettings =
        surfaceSettings[surface] ?: GlassSurfaceSettings()

    fun isEnabledFor(surface: GlassSurface?): Boolean = surface == null || surface in style.surfaces
}

fun isLiquidGlassAvailable(): Boolean = isRenderEffectSupported()

fun isLiquidGlassSupported(): Boolean = LiquidGlassSettings.enabled && isLiquidGlassAvailable()

fun isLiquidRefractionSupported(): Boolean = isRuntimeShaderSupported()

interface LiquidGlassBackdropHost {
    val isReady: Boolean
    fun acquire() {}
    fun release() {}
}

val LocalLiquidGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Stable
class NuvioBackdrop internal constructor(
    internal val graphicsLayer: GraphicsLayer,
) : Backdrop, LiquidGlassBackdropHost {

    override val isCoordinatesDependent: Boolean = true

    internal var sourceCoordinates: LayoutCoordinates? by mutableStateOf(null)

    override val isReady: Boolean
        get() = sourceCoordinates != null

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        val target = coordinates ?: return
        val source = sourceCoordinates ?: return
        if (!target.isAttached || !source.isAttached) return
        val offset = offsetBetween(source, target)
        translate(-offset.x, -offset.y) {
            drawLayer(graphicsLayer)
        }
    }

    private fun offsetBetween(source: LayoutCoordinates, target: LayoutCoordinates): Offset {
        if (source.findRootCoordinates() === target.findRootCoordinates()) {
            try {
                return source.localPositionOf(target, Offset.Zero)
            } catch (_: Exception) {
            }
        }
        val targetOnScreen = target.positionOnScreen()
        val sourceOnScreen = source.positionOnScreen()
        return if (targetOnScreen.isSpecified && sourceOnScreen.isSpecified) {
            targetOnScreen - sourceOnScreen
        } else {
            target.positionInWindow() - source.positionInWindow()
        }
    }
}

@Composable
fun rememberNuvioBackdrop(): NuvioBackdrop {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(graphicsLayer) { NuvioBackdrop(graphicsLayer) }
}

fun Modifier.nuvioBackdropSource(backdrop: NuvioBackdrop): Modifier =
    this then NuvioBackdropSourceElement(backdrop)

private class NuvioBackdropSourceElement(
    val backdrop: NuvioBackdrop,
) : ModifierNodeElement<NuvioBackdropSourceNode>() {

    override fun create(): NuvioBackdropSourceNode = NuvioBackdropSourceNode(backdrop)

    override fun update(node: NuvioBackdropSourceNode) {
        if (node.backdrop != backdrop) {
            node.backdrop.sourceCoordinates = null
            node.backdrop = backdrop
        }
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "nuvioBackdropSource"
        properties["backdrop"] = backdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NuvioBackdropSourceElement) return false
        return backdrop == other.backdrop
    }

    override fun hashCode(): Int = backdrop.hashCode()
}

private class NuvioBackdropSourceNode(
    var backdrop: NuvioBackdrop,
) : DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {

    override fun ContentDrawScope.draw() {
        drawContent()
        val nodeDensity = requireDensity()
        val contentScope = this
        backdrop.graphicsLayer.record {
            val previousDensity = drawContext.density
            drawContext.density = nodeDensity
            try {
                contentScope.drawContent()
            } finally {
                drawContext.density = previousDensity
            }
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            backdrop.sourceCoordinates = coordinates
        }
    }

    override fun onDetach() {
        backdrop.sourceCoordinates = null
    }
}

enum class LiquidGlassMaterial(val key: String) {
    LIQUID("liquid"),
    FROSTED("frosted"),
    CLEAR("clear"),
    SOLID("solid"),
    ;

    companion object {
        fun fromKey(key: String?): LiquidGlassMaterial =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: LIQUID
    }
}

@Immutable
data class LiquidGlassStyle(
    val material: LiquidGlassMaterial = LiquidGlassMaterial.LIQUID,
    val vibrancy: Boolean = true,
    val edgeHighlight: Boolean = true,
    val shadow: Boolean = true,
    val selectionLens: Boolean = true,
    val surfaces: Set<GlassSurface> = GlassSurface.entries.toSet(),
) {
    val usesBackdrop: Boolean
        get() = material != LiquidGlassMaterial.SOLID

    val usesBlur: Boolean
        get() = material == LiquidGlassMaterial.LIQUID || material == LiquidGlassMaterial.FROSTED

    val usesLens: Boolean
        get() = material == LiquidGlassMaterial.LIQUID || material == LiquidGlassMaterial.CLEAR

    fun encode(): String = listOf(
        EncodingVersion,
        material.key,
        vibrancy.encodeFlag(),
        edgeHighlight.encodeFlag(),
        shadow.encodeFlag(),
        selectionLens.encodeFlag(),
        surfaces.joinToString(SurfaceSeparator) { it.key }.ifEmpty { NoSurface },
    ).joinToString(Separator)

    companion object {
        val Default = LiquidGlassStyle()

        private const val EncodingVersion = "3"
        private const val SurfacesEncodingVersion = "2"
        private const val SurfaceSeparator = ","
        private const val NoSurface = "-"
        private const val LegacyEncodingVersion = "1"
        internal const val Separator = ";"

        fun decode(raw: String?): LiquidGlassStyle {
            if (raw.isNullOrBlank()) return Default
            val parts = raw.split(Separator)
            val (vibrancyAt, highlightAt, shadowAt, lensAt) = when (parts.firstOrNull()) {
                EncodingVersion, SurfacesEncodingVersion -> listOf(2, 3, 4, 5)
                LegacyEncodingVersion -> listOf(5, 7, 8, 9)
                else -> return Default
            }
            fun flag(index: Int, fallback: Boolean): Boolean =
                when (parts.getOrNull(index)) {
                    "1" -> true
                    "0" -> false
                    else -> fallback
                }
            return LiquidGlassStyle(
                material = LiquidGlassMaterial.fromKey(parts.getOrNull(1)),
                vibrancy = flag(vibrancyAt, Default.vibrancy),
                edgeHighlight = flag(highlightAt, Default.edgeHighlight),
                shadow = flag(shadowAt, Default.shadow),
                selectionLens = flag(lensAt, Default.selectionLens),
                surfaces = if (parts.firstOrNull() == EncodingVersion) {
                    parts.getOrNull(6)
                        ?.split(SurfaceSeparator)
                        ?.mapNotNull(GlassSurface::fromKey)
                        ?.toSet()
                        ?: Default.surfaces
                } else {
                    Default.surfaces
                },
            )
        }

        internal fun isLegacyEncoding(raw: String?): Boolean =
            raw?.split(Separator)?.firstOrNull() == LegacyEncodingVersion

        private fun Boolean.encodeFlag(): String = if (this) "1" else "0"
    }
}

internal val DefaultGlassHighlight: () -> Highlight? = { Highlight.Default }

internal val DefaultGlassShadow: () -> Shadow? = { Shadow.Default }

internal fun Modifier.liquidGlassSurface(
    backdrop: Backdrop?,
    shape: Shape,
    tint: Color,
    fallback: Color,
    blurRadius: Dp,
    refractionHeight: Dp = 0.dp,
    refractionAmount: Dp = refractionHeight,
    withVibrancy: Boolean = true,
    withHighlight: Boolean = true,
    withShadow: Boolean = false,
    fallbackBrush: Brush? = null,
    backdropOpacity: Float = 1f,
    surface: GlassSurface? = null,
): Modifier {
    val isBar = surface == GlassSurface.NAVIGATION_BAR
    // Elements outside the listed screens (surface null) keep the default look.
    val own = if (isBar) null else surface?.let(GlassAppearance::settingsFor) ?: GlassSurfaceSettings()
    val style = own?.asStyle(GlassAppearance.style) ?: GlassAppearance.style
    if (backdrop == null || !isLiquidGlassSupported() || !style.usesBackdrop || !GlassAppearance.isEnabledFor(surface)) {
        return if (fallbackBrush != null) clip(shape).background(fallbackBrush) else clip(shape).background(fallback)
    }
    // The navigation bar draws its own effects; every other element keeps its proportions and
    // follows its own blur, refraction and tint sliders.
    val effects = own?.effects ?: LiquidGlassEffectDefaults.floatingBar
    val defaults = LiquidGlassEffectDefaults.floatingBar
    fun ratio(value: Float, default: Float): Float = if (!isBar && default > 0f) (value / default) else 1f
    val blurRadius = if (style.usesBlur) blurRadius * ratio(effects.blurRadiusDp, defaults.blurRadiusDp) else 0.dp
    val refractionHeight = if (style.usesLens) refractionHeight * ratio(effects.refractionHeightDp, defaults.refractionHeightDp) else 0.dp
    val refractionAmount = refractionAmount * ratio(effects.refractionAmountDp, defaults.refractionAmountDp)
    val scaledTint = (tint.alpha * ratio(effects.tintOpacity, defaults.tintOpacity)).coerceIn(0f, 1f)
    // Without blur (« Transparent ») text must stay readable: the tint never drops below 60 %.
    val tint = tint.copy(alpha = if (!isBar && blurRadius <= 0.dp) maxOf(scaledTint, 0.6f) else scaledTint)
    val withVibrancy = withVibrancy && style.vibrancy
    val withHighlight = withHighlight && style.edgeHighlight
    val withShadow = if (own != null) own.shadow else withShadow && style.shadow
    val depthEffect = own?.effects?.depthEffect ?: false
    val chromaticAberration = own?.effects?.chromaticAberration ?: false
    // « Halo de verre »: a soft light rim around the surface, like around the floating bar.
    val haloOn = own?.glow ?: GlassAppearance.glow
    val halo = if (haloOn) Modifier.border(1.dp, Color.White.copy(alpha = 0.16f), shape) else Modifier
    return drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (withVibrancy) vibrancy()
                if (blurRadius > 0.dp) blur(blurRadius.toPx())
                if (shape is CornerBasedShape && refractionHeight > 0.dp && refractionAmount > 0.dp) {
                    lens(
                        refractionHeight = refractionHeight.toPx().coerceAtMost(size.minDimension / 2f),
                        refractionAmount = refractionAmount.toPx(),
                        depthEffect = depthEffect,
                        chromaticAberration = chromaticAberration,
                    )
                }
                if (backdropOpacity < 1f) opacity(backdropOpacity.coerceIn(0f, 1f))
            },
            highlight = if (withHighlight) DefaultGlassHighlight else null,
            shadow = if (withShadow) DefaultGlassShadow else null,
            onDrawSurface = if (tint.alpha > 0f) {
                { drawRect(tint) }
            } else {
                null
            },
        ).then(halo)
}

internal fun Modifier.liquidGlassBlur(
    backdrop: Backdrop?,
    radius: Dp,
    alphaProvider: () -> Float = { 1f },
): Modifier =
    if (backdrop == null || !isLiquidGlassAvailable()) {
        this
    } else {
        graphicsLayer { alpha = alphaProvider() }
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = { blur(radius.toPx()) },
            )
    }

@Composable
internal fun isAmbientLiquidGlassActive(): Boolean {
    if (!GlassAppearance.isEnabledFor(LocalGlassSurface.current)) return false
    val backdrop = LocalLiquidGlassBackdrop.current ?: return false
    val host = backdrop as? LiquidGlassBackdropHost
    return isLiquidGlassSupported() && (host?.isReady ?: true)
}

@Composable
internal fun Modifier.ambientLiquidGlass(
    shape: Shape,
    tint: Color,
    fallback: Color,
    blurRadius: Dp = 16.dp,
    refractionHeight: Dp = 10.dp,
    refractionAmount: Dp = refractionHeight * 1.6f,
    withVibrancy: Boolean = true,
    withHighlight: Boolean = true,
    withShadow: Boolean = false,
    fallbackBrush: Brush? = null,
    backdropOpacity: Float = 1f,
): Modifier {
    val backdrop = LocalLiquidGlassBackdrop.current
    val host = backdrop as? LiquidGlassBackdropHost
    // Without glass (the AMOLED style, or older phones) the video is not copied frame by frame.
    if (host != null && isLiquidGlassSupported()) {
        DisposableEffect(host) {
            host.acquire()
            onDispose { host.release() }
        }
    }
    val ready = backdrop != null && (host?.isReady ?: true)
    return liquidGlassSurface(
        backdrop = if (ready) backdrop else null,
        shape = shape,
        tint = tint,
        fallback = fallback,
        blurRadius = blurRadius,
        refractionHeight = refractionHeight,
        refractionAmount = refractionAmount,
        withVibrancy = withVibrancy,
        withHighlight = withHighlight,
        withShadow = withShadow,
        fallbackBrush = fallbackBrush,
        backdropOpacity = backdropOpacity,
        surface = LocalGlassSurface.current,
    )
}

/** The bar's look: the Liquid glass settings when « Barre de navigation » is ticked, else plain. */
fun LiquidGlassStyle.forNavigationBar(): LiquidGlassStyle =
    if (GlassSurface.NAVIGATION_BAR in surfaces) this else copy(material = LiquidGlassMaterial.SOLID, selectionLens = false)

/** The glass halo of the bar, only while the Liquid glass settings apply to the bar. */
fun LiquidGlassStyle.navigationBarGlow(glowEnabled: Boolean): Boolean =
    glowEnabled && GlassSurface.NAVIGATION_BAR in surfaces
