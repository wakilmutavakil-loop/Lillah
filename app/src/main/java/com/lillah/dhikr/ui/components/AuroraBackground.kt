package com.lillah.dhikr.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.lillah.dhikr.ui.theme.LocalAppGradients
import androidx.compose.material3.MaterialTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * The ambient wash every screen sits on: three oversized colour blooms drifting on long, offset
 * cycles. Slow enough to read as light rather than motion, and cheap — three radial gradients per
 * frame, no layers or blurs.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    animated: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val gradients = LocalAppGradients.current
    val background = MaterialTheme.colorScheme.background

    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(38_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "auroraPhase",
    )
    val breath by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(11_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "auroraBreath",
    )

    val t = if (animated) phase else 0.2f
    val scale = if (animated) breath else 1f
    val alpha = (if (gradients.isDark) 0.42f else 0.30f) * intensity.coerceIn(0f, 1.4f)

    Box(modifier = modifier.background(background)) {
        Canvas(Modifier.fillMaxSize()) {
            val stops = gradients.aurora
            bloom(stops.getOrElse(0) { Color.Transparent }, 0.22f, 0.16f, t, 0f, alpha, scale)
            bloom(stops.getOrElse(1) { Color.Transparent }, 0.86f, 0.30f, t, 0.42f, alpha, scale)
            bloom(stops.getOrElse(2) { Color.Transparent }, 0.48f, 0.92f, t, 0.78f, alpha * 0.9f, scale)
        }
        content()
    }
}

private fun DrawScope.bloom(
    color: Color,
    anchorX: Float,
    anchorY: Float,
    phase: Float,
    offset: Float,
    alpha: Float,
    scale: Float,
) {
    val angle = (phase + offset) * 2f * Math.PI.toFloat()
    val driftX = cos(angle) * size.width * 0.10f
    val driftY = sin(angle * 0.8f) * size.height * 0.06f
    val center = Offset(size.width * anchorX + driftX, size.height * anchorY + driftY)
    val radius = size.minDimension * 0.78f * scale

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/** A flatter variant for detail screens, where content should carry the colour instead. */
@Composable
fun QuietBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) = AuroraBackground(modifier = modifier, intensity = 0.55f, content = content)
