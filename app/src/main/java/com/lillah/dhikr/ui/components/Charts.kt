package com.lillah.dhikr.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.core.time.DateFormats
import com.lillah.dhikr.domain.model.DayPoint
import com.lillah.dhikr.domain.model.MonthStats
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Motion
import com.lillah.dhikr.ui.theme.mix
import java.time.LocalDate

/**
 * Weekly bars. Bars grow from the baseline on first composition, the current day is marked rather
 * than recoloured, and the goal line is a reference — never a pass/fail mark.
 */
@Composable
fun WeeklyBars(
    days: List<DayPoint>,
    modifier: Modifier = Modifier,
    goal: Int? = null,
    today: LocalDate = LocalDate.now(),
    height: Dp = 168.dp,
) {
    val gradients = LocalAppGradients.current
    val peak = maxOf(days.maxOfOrNull { it.total } ?: 0, goal ?: 0, 1)

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(height),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { point ->
                val isToday = point.date == today
                val fraction by animateFloatAsState(
                    targetValue = (point.total.toFloat() / peak).coerceIn(0f, 1f),
                    animationSpec = Motion.emphasized(Motion.Slow),
                    label = "bar_${point.date}",
                )
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (point.total > 0) {
                        Text(
                            text = point.total.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        // Track, so empty days still read as part of the series.
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction.coerceAtLeast(if (point.total > 0) 0.06f else 0f))
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.verticalGradient(gradients.hero))
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            days.forEach { point ->
                val isToday = point.date == today
                Text(
                    text = DateFormats.weekdayInitial(point.date),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Month heatmap. Five intensity steps rather than a continuous ramp, so a quiet day and a busy one
 * are distinguishable at a glance without needing a legend.
 */
@Composable
fun MonthHeatmap(
    stats: MonthStats,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    weekStart: java.time.DayOfWeek = java.time.DayOfWeek.MONDAY,
) {
    val gradients = LocalAppGradients.current
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val peak = (stats.peak).coerceAtLeast(1)

    val leadingBlanks = stats.days.firstOrNull()?.let { first ->
        (first.date.dayOfWeek.value - weekStart.value + 7) % 7
    } ?: 0

    val cells: List<DayPoint?> = List(leadingBlanks) { null } + stats.days
    val weeks = cells.chunked(7)

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(7) { index ->
                val day = weekStart.plus(index.toLong())
                Text(
                    text = day.getDisplayName(
                        java.time.format.TextStyle.NARROW,
                        java.util.Locale.getDefault(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (index in 0 until 7) {
                    val point = week.getOrNull(index)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (point != null) {
                            val step = intensityStep(point.total, peak)
                            val fill = if (step == 0) {
                                base.copy(alpha = 0.55f)
                            } else {
                                gradients.hero[minOf(step - 1, gradients.hero.lastIndex)]
                                    .copy(alpha = 0.28f + 0.22f * step)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(fill)
                                    .then(
                                        if (point.date == today) {
                                            Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(9.dp),
                                            )
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = point.date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (step >= 3) {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun intensityStep(total: Int, peak: Int): Int = when {
    total <= 0 -> 0
    else -> ((total.toFloat() / peak) * 4f).toInt().coerceIn(0, 3) + 1
}

/** Compact trend line for the home screen: a smoothed path with a soft fill beneath it. */
@Composable
fun Sparkline(
    days: List<DayPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
) {
    val gradients = LocalAppGradients.current
    val peak = (days.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)
    // Animatable rather than animateFloatAsState: the target equals the initial value, so
    // animateFloatAsState would settle immediately and the line would never draw itself in.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(days.size) {
        reveal.animateTo(1f, tween(Motion.Slow, easing = Motion.Emphasized))
    }

    Canvas(modifier.fillMaxWidth().height(height)) {
        if (days.size < 2) return@Canvas
        val stepX = size.width / (days.size - 1)
        val points = days.mapIndexed { index, point ->
            Offset(
                x = index * stepX,
                y = size.height * (1f - (point.total.toFloat() / peak) * 0.86f) - 2f,
            )
        }

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val previous = points[i - 1]
                val current = points[i]
                val midX = (previous.x + current.x) / 2f
                cubicTo(midX, previous.y, midX, current.y, current.x, current.y)
            }
        }

        val fill = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                listOf(
                    gradients.hero.first().copy(alpha = 0.28f * reveal.value),
                    gradients.hero.first().copy(alpha = 0f),
                )
            ),
        )
        drawPath(
            path = line,
            brush = Brush.horizontalGradient(gradients.hero),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            alpha = reveal.value,
        )
        drawCircle(
            color = gradients.hero.last(),
            radius = 4.dp.toPx(),
            center = points.last(),
        )
        drawCircle(
            color = Color.White.mix(gradients.hero.last(), 0.15f),
            radius = 2.dp.toPx(),
            center = points.last(),
        )
    }
}
