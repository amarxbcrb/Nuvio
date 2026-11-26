package com.nuvio.app.core.ui.glass

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The Liquid glass settings of one element (home, sources, player, windows): its own material,
 * light, shadow, glass halo, blur, refraction (« déformation ») and tint. The navigation bar keeps
 * its own settings (navBarGlass and its effects).
 */
@Serializable
internal data class GlassSurfaceSettings(
    val material: String = LiquidGlassMaterial.LIQUID.key,
    val vibrancy: Boolean = true,
    val edgeHighlight: Boolean = true,
    val shadow: Boolean = false,
    val glow: Boolean = false,
    val effects: LiquidGlassEffectSettings = LiquidGlassEffectDefaults.floatingBar,
) {
    val glassMaterial: LiquidGlassMaterial
        get() = LiquidGlassMaterial.fromKey(material)

    /** As a [LiquidGlassStyle], to reuse the navigation bar's controls. */
    fun asStyle(base: LiquidGlassStyle): LiquidGlassStyle = base.copy(
        material = glassMaterial,
        vibrancy = vibrancy,
        edgeHighlight = edgeHighlight,
        shadow = shadow,
    )

    fun withStyle(style: LiquidGlassStyle): GlassSurfaceSettings = copy(
        material = style.material.key,
        vibrancy = style.vibrancy,
        edgeHighlight = style.edgeHighlight,
        shadow = style.shadow,
    )
}

private val glassSurfacesJson = Json { ignoreUnknownKeys = true }

internal fun encodeGlassSurfaceSettings(settings: Map<GlassSurface, GlassSurfaceSettings>): String =
    glassSurfacesJson.encodeToString(settings.mapKeys { it.key.key })

internal fun decodeGlassSurfaceSettings(raw: String?): Map<GlassSurface, GlassSurfaceSettings> {
    if (raw.isNullOrBlank()) return emptyMap()
    val decoded = runCatching {
        glassSurfacesJson.decodeFromString<Map<String, GlassSurfaceSettings>>(raw)
    }.getOrNull() ?: return emptyMap()
    return decoded.mapNotNull { (key, value) ->
        GlassSurface.fromKey(key)?.let { surface ->
            surface to value.copy(
                effects = value.effects.sanitized(
                    LiquidGlassEffectDefaults.floatingBar,
                    LiquidGlassEffectDefaults.floatingBarLimits,
                ),
            )
        }
    }.toMap()
}
