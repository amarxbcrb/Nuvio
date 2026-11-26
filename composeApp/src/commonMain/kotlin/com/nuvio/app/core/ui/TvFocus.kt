package com.nuvio.app.core.ui

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** What kind of device the app runs on. Set by the platform at start-up. */
object DeviceForm {
    /** Android TV / Google TV: driven with a remote, no touch screen. */
    var isTv: Boolean by mutableStateOf(false)
}

/**
 * On TV, every element that can be clicked shows where the remote is: a white outline and a
 * light fill around the focused element (the default ripple is barely visible from a sofa).
 */
internal object TvFocusIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = TvFocusNode(interactionSource)
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = 7_331
}

private class TvFocusNode(private val source: InteractionSource) : Modifier.Node(), DrawModifierNode {
    private var focused = false
    private var pressed = false

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is FocusInteraction.Focus -> focused = true
                    is FocusInteraction.Unfocus -> focused = false
                    is PressInteraction.Press -> pressed = true
                    is PressInteraction.Release, is PressInteraction.Cancel -> pressed = false
                }
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (!focused && !pressed) return
        val radius = minOf(16.dp.toPx(), size.minDimension / 2f)
        drawRoundRect(
            color = Color.White.copy(alpha = if (pressed) 0.22f else 0.14f),
            cornerRadius = CornerRadius(radius),
        )
        if (focused) {
            val stroke = 3.dp.toPx()
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(size.width - stroke, size.height - stroke),
                cornerRadius = CornerRadius(radius),
                style = Stroke(stroke),
            )
        }
    }
}
