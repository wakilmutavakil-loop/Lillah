package com.lillah.dhikr.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.lillah.dhikr.domain.model.CoverArt
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.mix
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Built-in collection covers, drawn rather than shipped as images.
 *
 * A sunrise should still look like a sunrise in the Ocean theme, so each piece keeps its own
 * intrinsic palette and is only pulled part-way toward the active one. Vectors also mean covers
 * stay sharp at any card size and cost the APK nothing.
 */
@Composable
fun CoverArtworkCanvas(
    artwork: CoverArt,
    modifier: Modifier = Modifier,
    seed: Long = 0L,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val gradients = LocalAppGradients.current
    val theme = gradients.hero
    val dark = gradients.isDark

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            when (artwork) {
                CoverArt.Sunrise -> drawSunrise(theme, dark)
                CoverArt.Night -> drawNight(theme, dark, seed)
                CoverArt.Beads -> drawBeads(theme, dark, seed)
                CoverArt.Arch -> drawArch(theme, dark)
                CoverArt.Bloom -> drawBloom(theme, dark, seed)
                CoverArt.Waves -> drawWaves(theme, dark)
            }
        }
        content()
    }
}

private fun blend(base: Color, theme: Color, amount: Float = 0.34f) = base.mix(theme, amount)

// ------------------------------------------------------------------------- Morning

private fun DrawScope.drawSunrise(theme: List<Color>, dark: Boolean) {
    val top = blend(if (dark) Color(0xFF2B1F4A) else Color(0xFF6B5BC4), theme[0], 0.42f)
    val mid = blend(if (dark) Color(0xFFB4562F) else Color(0xFFFF9A5B), theme[1], 0.26f)
    val low = blend(if (dark) Color(0xFFD98B3C) else Color(0xFFFFD08A), theme[2], 0.2f)

    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(0f to top, 0.52f to mid, 1f to low),
        )
    )

    // Sun, sitting low with a wide halo bleeding into the sky.
    val sunCenter = Offset(size.width * 0.5f, size.height * 0.70f)
    val sunRadius = size.minDimension * 0.17f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0f)),
            center = sunCenter,
            radius = sunRadius * 3.6f,
        ),
        radius = sunRadius * 3.6f,
        center = sunCenter,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, blend(Color(0xFFFFE7A8), theme[2], 0.3f)),
            center = sunCenter,
            radius = sunRadius,
        ),
        radius = sunRadius,
        center = sunCenter,
    )

    // Three ridgelines, each darker and closer than the last.
    ridge(0.78f, 0.16f, blend(Color(0xFFB06A4A), theme[1], 0.4f).copy(alpha = 0.5f))
    ridge(0.87f, 0.11f, blend(Color(0xFF7A4436), theme[0], 0.4f).copy(alpha = 0.65f))
    ridge(0.95f, 0.08f, blend(Color(0xFF432840), theme[0], 0.35f).copy(alpha = 0.85f))

    // A few birds, small enough to read as texture.
    val birdColor = Color.White.copy(alpha = 0.45f)
    listOf(0.24f to 0.30f, 0.32f to 0.24f, 0.70f to 0.32f).forEach { (x, y) ->
        val p = Offset(size.width * x, size.height * y)
        val w = size.minDimension * 0.035f
        drawArc(
            color = birdColor,
            startAngle = 200f, sweepAngle = 110f, useCenter = false,
            topLeft = Offset(p.x - w, p.y - w * 0.5f),
            size = Size(w, w),
            style = Stroke(width = size.minDimension * 0.006f, cap = StrokeCap.Round),
        )
        drawArc(
            color = birdColor,
            startAngle = 230f, sweepAngle = 110f, useCenter = false,
            topLeft = Offset(p.x, p.y - w * 0.5f),
            size = Size(w, w),
            style = Stroke(width = size.minDimension * 0.006f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.ridge(baseline: Float, amplitude: Float, color: Color) {
    val path = Path()
    val y = size.height * baseline
    val a = size.height * amplitude
    path.moveTo(0f, y)
    path.cubicTo(
        size.width * 0.22f, y - a,
        size.width * 0.38f, y + a * 0.35f,
        size.width * 0.56f, y - a * 0.45f,
    )
    path.cubicTo(
        size.width * 0.74f, y - a * 1.1f,
        size.width * 0.88f, y + a * 0.2f,
        size.width, y - a * 0.3f,
    )
    path.lineTo(size.width, size.height)
    path.lineTo(0f, size.height)
    path.close()
    drawPath(path, color)
}

// ------------------------------------------------------------------------- Evening

private fun DrawScope.drawNight(theme: List<Color>, dark: Boolean, seed: Long) {
    val top = blend(Color(0xFF0E1136), theme[0], if (dark) 0.22f else 0.3f)
    val mid = blend(Color(0xFF2A1B54), theme[1], 0.28f)
    val low = blend(Color(0xFF4A2A63), theme[2], 0.26f)

    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(0f to top, 0.6f to mid, 1f to low),
        )
    )

    val random = Random(seed + 41)
    repeat(46) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height * 0.8f
        val r = size.minDimension * (0.0035f + random.nextFloat() * 0.005f)
        drawCircle(
            color = Color.White.copy(alpha = 0.25f + random.nextFloat() * 0.6f),
            radius = r,
            center = Offset(x, y),
        )
    }

    // Crescent built by subtracting one disc from another, so it sits cleanly on the gradient.
    val moonCenter = Offset(size.width * 0.72f, size.height * 0.28f)
    val moonRadius = size.minDimension * 0.16f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0f)),
            center = moonCenter,
            radius = moonRadius * 2.8f,
        ),
        radius = moonRadius * 2.8f,
        center = moonCenter,
    )
    val outer = Path().apply {
        addOval(
            Rect(
                moonCenter - Offset(moonRadius, moonRadius),
                Size(moonRadius * 2, moonRadius * 2),
            )
        )
    }
    val cut = Path().apply {
        val cutCenter = moonCenter + Offset(-moonRadius * 0.42f, -moonRadius * 0.20f)
        val cutRadius = moonRadius * 0.94f
        addOval(
            Rect(
                cutCenter - Offset(cutRadius, cutRadius),
                Size(cutRadius * 2, cutRadius * 2),
            )
        )
    }
    val crescent = Path().apply { op(outer, cut, PathOperation.Difference) }
    drawPath(crescent, Color(0xFFFFF6DC))

    ridge(0.86f, 0.10f, blend(Color(0xFF1B1440), theme[0], 0.3f).copy(alpha = 0.75f))
    ridge(0.96f, 0.07f, blend(Color(0xFF0B0A22), theme[0], 0.25f).copy(alpha = 0.92f))
}

// ------------------------------------------------------------------------- Everyday tasbih

private fun DrawScope.drawBeads(theme: List<Color>, dark: Boolean, seed: Long) {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                theme[0].mix(if (dark) Color.Black else Color.White, if (dark) 0.35f else 0.12f),
                theme[2].mix(if (dark) Color.Black else Color.White, if (dark) 0.42f else 0.06f),
            )
        )
    )

    val center = Offset(size.width * 0.5f, size.height * 0.52f)
    val radius = size.minDimension * 0.34f
    val beadCount = 21
    val random = Random(seed + 7)

    // The strand itself.
    drawCircle(
        color = Color.White.copy(alpha = 0.16f),
        radius = radius,
        center = center,
        style = Stroke(width = size.minDimension * 0.008f),
    )

    for (i in 0 until beadCount) {
        val angle = (i.toFloat() / beadCount) * 2f * PI.toFloat() - PI.toFloat() / 2f
        val position = Offset(
            center.x + cos(angle) * radius,
            center.y + sin(angle) * radius,
        )
        val beadRadius = size.minDimension * (0.026f + random.nextFloat() * 0.008f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.42f)),
                center = position - Offset(beadRadius * 0.3f, beadRadius * 0.3f),
                radius = beadRadius * 1.8f,
            ),
            radius = beadRadius,
            center = position,
        )
    }

    // The leader bead at the top of the strand.
    val leader = Offset(center.x, center.y - radius)
    val leaderRadius = size.minDimension * 0.055f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, Color.White.copy(alpha = 0.6f)),
            center = leader - Offset(leaderRadius * 0.3f, leaderRadius * 0.35f),
            radius = leaderRadius * 2f,
        ),
        radius = leaderRadius,
        center = leader,
    )
}

// ------------------------------------------------------------------------- After prayer

private fun DrawScope.drawArch(theme: List<Color>, dark: Boolean) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                theme[1].mix(if (dark) Color.Black else Color.White, if (dark) 0.4f else 0.08f),
                theme[0].mix(if (dark) Color.Black else Color.White, if (dark) 0.55f else 0.22f),
            )
        )
    )

    // Three nested pointed arches, receding.
    listOf(0.86f to 0.10f, 0.68f to 0.18f, 0.50f to 0.30f).forEach { (widthFraction, alpha) ->
        drawPath(pointedArch(widthFraction), Color.White.copy(alpha = alpha))
    }
    drawPath(
        pointedArch(0.5f),
        Color.White.copy(alpha = 0.5f),
        style = Stroke(width = size.minDimension * 0.008f),
    )

    // An eight-point star, the simplest recognisable piece of the geometric vocabulary.
    val starCenter = Offset(size.width * 0.5f, size.height * 0.42f)
    val starRadius = size.minDimension * 0.10f
    repeat(4) { i ->
        val angle = (PI.toFloat() / 4f) * i
        val dx = cos(angle) * starRadius
        val dy = sin(angle) * starRadius
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = starCenter + Offset(-dx, -dy),
            end = starCenter + Offset(dx, dy),
            strokeWidth = size.minDimension * 0.006f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.pointedArch(widthFraction: Float): Path {
    val width = size.width * widthFraction
    val left = (size.width - width) / 2f
    val right = left + width
    val bottom = size.height * 0.98f
    val springLine = size.height * 0.62f
    val apex = size.height * 0.16f

    return Path().apply {
        moveTo(left, bottom)
        lineTo(left, springLine)
        quadraticTo(left + width * 0.14f, apex, size.width / 2f, apex)
        quadraticTo(right - width * 0.14f, apex, right, springLine)
        lineTo(right, bottom)
        close()
    }
}

// ------------------------------------------------------------------------- Custom collections

private fun DrawScope.drawBloom(theme: List<Color>, dark: Boolean, seed: Long) {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                theme[0].mix(if (dark) Color.Black else Color.White, if (dark) 0.32f else 0.05f),
                theme[2].mix(if (dark) Color.Black else Color.White, if (dark) 0.4f else 0.02f),
            )
        )
    )

    val random = Random(seed * 31 + 11)
    repeat(5) { index ->
        val center = Offset(
            size.width * (0.15f + random.nextFloat() * 0.7f),
            size.height * (0.15f + random.nextFloat() * 0.7f),
        )
        val radius = size.minDimension * (0.22f + random.nextFloat() * 0.3f)
        val color = theme[index % theme.size]
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.42f), color.copy(alpha = 0f)),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}

private fun DrawScope.drawWaves(theme: List<Color>, dark: Boolean) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                theme[2].mix(if (dark) Color.Black else Color.White, if (dark) 0.3f else 0.06f),
                theme[0].mix(if (dark) Color.Black else Color.White, if (dark) 0.5f else 0.16f),
            )
        )
    )
    for (layer in 0 until 4) {
        val path = Path()
        val baseline = size.height * (0.5f + layer * 0.13f)
        val amplitude = size.height * (0.09f - layer * 0.015f)
        path.moveTo(0f, baseline)
        var x = 0f
        while (x <= size.width) {
            val y = baseline + sin((x / size.width) * 2f * PI.toFloat() * 1.4f + layer) * amplitude
            path.lineTo(x, y)
            x += size.width / 48f
        }
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()
        drawPath(path, Color.White.copy(alpha = 0.10f + layer * 0.06f))
    }
}
