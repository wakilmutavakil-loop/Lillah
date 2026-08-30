package com.lillah.dhikr.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Motion
import kotlinx.coroutines.launch

private class TapRipple(val id: Long) {
    val progress = Animatable(0f)
}

/**
 * The hero counter.
 *
 * Everything here exists to make one repetition feel like it landed: the ring fills a bead at a
 * time, the disc gives under the finger, a ring of light travels outward from the tap, and the
 * number arrives from below rather than swapping in place. A completed round blooms once and then
 * gets out of the way.
 */
@Composable
fun TasbihCounter(
    count: Int,
    target: Int,
    roundsToday: Int,
    onTap: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 300.dp,
    celebrationKey: Int = 0,
    enabled: Boolean = true,
) {
    val gradients = LocalAppGradients.current
    val scope = rememberCoroutineScope()
    val ripples = remember { mutableStateListOf<TapRipple>() }
    var nextRippleId by remember { mutableLongStateOf(0L) }

    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource, pressedScale = 0.955f)

    val bloom = remember { Animatable(0f) }
    LaunchedEffect(celebrationKey) {
        if (celebrationKey > 0) {
            bloom.snapTo(0f)
            bloom.animateTo(1f, tween(Motion.Celebration, easing = Motion.Decelerate))
        }
    }

    val complete = count >= target
    val glowStrength by animateFloatAsState(
        targetValue = if (complete) 1f else 0.55f,
        animationSpec = Motion.gentle(),
        label = "glow",
    )

    val strokeWidth = diameter * 0.045f
    val discInset = strokeWidth * 2.6f

    Box(
        modifier = modifier
            .size(diameter)
            .scale(pressScale)
            .semantics {
                contentDescription = "Tasbih counter, $count of $target. " +
                    "Tap to count, long press to undo."
            }
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    val ripple = TapRipple(nextRippleId++)
                    ripples.add(ripple)
                    scope.launch {
                        ripple.progress.animateTo(1f, tween(760, easing = Motion.Decelerate))
                        ripples.remove(ripple)
                    }
                    onTap()
                },
                onLongClick = onUndo,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Ambient halo and the outward-travelling tap rings, both behind the counter itself.
        Canvas(Modifier.fillMaxSize()) {
            val outerRadius = size.minDimension / 2f
            val haloColor = gradients.hero.first()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        haloColor.copy(alpha = 0.30f * glowStrength),
                        haloColor.copy(alpha = 0f),
                    ),
                    center = center,
                    radius = outerRadius * 1.28f,
                ),
                radius = outerRadius * 1.28f,
            )

            ripples.forEach { ripple ->
                val t = ripple.progress.value
                val radius = outerRadius * (0.62f + 0.52f * t)
                drawCircle(
                    color = gradients.hero.last().copy(alpha = (1f - t) * 0.38f),
                    radius = radius,
                    style = Stroke(width = (1f - t) * strokeWidth.toPx() * 0.8f + 1f),
                )
            }

            val bloomValue = bloom.value
            if (bloomValue > 0f && bloomValue < 1f) {
                val radius = outerRadius * (0.7f + 0.75f * bloomValue)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            gradients.hero[1].copy(alpha = 0f),
                            gradients.hero[1].copy(alpha = (1f - bloomValue) * 0.34f),
                            gradients.hero[1].copy(alpha = 0f),
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                )
            }
        }

        if (target in 1..MAX_BEADS) {
            BeadRing(
                filled = count,
                total = target,
                diameter = diameter,
                strokeWidth = strokeWidth,
            )
        } else {
            ProgressRing(
                progress = count.toFloat() / target.coerceAtLeast(1),
                diameter = diameter,
                strokeWidth = strokeWidth,
            )
        }

        // Inner disc.
        Box(
            modifier = Modifier
                .size(diameter - discInset * 2)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = gradients.hero,
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Soft specular highlight so the disc reads as a physical bead, not a flat circle.
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.34f),
                            Color.White.copy(alpha = 0f),
                        ),
                        center = Offset(size.width * 0.3f, size.height * 0.22f),
                        radius = size.minDimension * 0.62f,
                    )
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AnimatedContent(
                    targetState = count,
                    transitionSpec = {
                        if (targetState >= initialState) {
                            (slideInVertically { it / 3 } + fadeIn() + scaleIn(initialScale = 0.82f))
                                .togetherWith(slideOutVertically { -it / 3 } + fadeOut())
                        } else {
                            (slideInVertically { -it / 3 } + fadeIn())
                                .togetherWith(slideOutVertically { it / 3 } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "count",
                ) { value ->
                    Text(
                        text = "$value",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = (diameter.value * 0.24f).sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(18.dp)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.4f))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "of $target",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.86f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .width(18.dp)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.4f))
                    )
                }

                if (roundsToday > 0) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (roundsToday == 1) "1 round today" else "$roundsToday rounds today",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
