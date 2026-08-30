package com.lillah.dhikr.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Motion
import androidx.compose.material3.MaterialTheme

/**
 * The app's recurring progress mark. Used at every size from a 28dp list glyph to the hero ring
 * on Home, so the sweep, cap and track all scale off the stroke width.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 64.dp,
    strokeWidth: Dp = 8.dp,
    colors: List<Color>? = null,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    startAngle: Float = -90f,
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val gradients = LocalAppGradients.current
    val stops = colors ?: gradients.hero
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = Motion.emphasized(Motion.Slow),
        label = "ringProgress",
    )
    val value = if (animate) animated else target

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (value > 0f) {
                drawArc(
                    brush = Brush.linearGradient(
                        colors = stops,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                    ),
                    startAngle = startAngle,
                    sweepAngle = 360f * value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/**
 * A ring divided into one segment per repetition. Below about sixty beads each repetition is
 * worth seeing individually — filling them one by one is the satisfying part — and above that the
 * segments would be thinner than the gaps, so [ProgressRing] takes over.
 */
@Composable
fun BeadRing(
    filled: Int,
    total: Int,
    modifier: Modifier = Modifier,
    diameter: Dp = 64.dp,
    strokeWidth: Dp = 8.dp,
    colors: List<Color>? = null,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val gradients = LocalAppGradients.current
    val stops = colors ?: gradients.hero
    val safeTotal = total.coerceAtLeast(1)
    val animatedFilled by animateFloatAsState(
        targetValue = filled.coerceIn(0, safeTotal).toFloat(),
        animationSpec = Motion.snappy(),
        label = "beadFill",
    )

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            val segment = 360f / safeTotal
            // Gap grows with segment count so dense rings stay legible instead of merging.
            val gap = (segment * 0.22f).coerceIn(1.6f, 7f)
            val sweep = (segment - gap).coerceAtLeast(1.2f)
            val brush = Brush.linearGradient(
                colors = stops,
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height),
            )

            for (index in 0 until safeTotal) {
                val start = -90f + index * segment + gap / 2f
                val fill = (animatedFilled - index).coerceIn(0f, 1f)
                drawArc(
                    color = trackColor,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                if (fill > 0f) {
                    drawArc(
                        brush = brush,
                        startAngle = start,
                        sweepAngle = sweep * fill,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        alpha = 0.35f + 0.65f * fill,
                    )
                }
            }
        }
        content()
    }
}

/** Threshold at which individual beads stop being worth drawing. */
const val MAX_BEADS = 60
