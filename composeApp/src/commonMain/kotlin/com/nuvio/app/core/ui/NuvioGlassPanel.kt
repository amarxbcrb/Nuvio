package com.nuvio.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.glass.ambientLiquidGlass
import com.nuvio.app.core.ui.glass.isAmbientLiquidGlassActive

/** Opaque card colour of the redesign (#141213), used whenever glass is off or unavailable. */
val NuvioCardColor = Color(0xFF141213)

/**
 * Card surface shared by the redesigned screens: liquid glass over the page backdrop when the
 * Liquid glass style is on and a backdrop exists (artwork behind the streams list), otherwise the
 * opaque #141213 card. Same tint and edge as the detail page buttons, so both styles stay coherent.
 */
@Composable
fun Modifier.nuvioGlassPanel(shape: Shape, tintAlpha: Float = 0.42f): Modifier {
    val glass = isAmbientLiquidGlassActive()
    return if (glass) {
        this
            .ambientLiquidGlass(
                shape = shape,
                tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = tintAlpha),
                fallback = NuvioCardColor,
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
    } else {
        this.clip(shape).background(NuvioCardColor)
    }
}

/** Small control surface (round buttons, chips) on top of a panel. */
@Composable
fun Modifier.nuvioGlassControl(shape: Shape): Modifier {
    val glass = isAmbientLiquidGlassActive()
    return if (glass) {
        this
            .ambientLiquidGlass(
                shape = shape,
                tint = Color.White.copy(alpha = 0.10f),
                fallback = Color.White.copy(alpha = 0.08f),
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
    } else {
        this.clip(shape).background(Color.White.copy(alpha = 0.08f))
    }
}

@Composable
fun NuvioGlassBox(shape: Shape, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.nuvioGlassPanel(shape), content = content)
}
