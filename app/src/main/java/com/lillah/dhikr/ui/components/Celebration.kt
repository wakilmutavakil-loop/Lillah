package com.lillah.dhikr.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.domain.gamification.AchievementDef
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Motion
import com.lillah.dhikr.ui.theme.Radii
import com.lillah.dhikr.ui.theme.Spacing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Shown once, when a milestone is first reached. It interrupts deliberately — this is the one
 * moment the app asks for attention — and says what was reached without congratulating the user
 * on their worship.
 */
@Composable
fun MilestoneCelebration(
    achievement: AchievementDef?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradients = LocalAppGradients.current
    val visible = achievement != null
    val burst = remember { Animatable(0f) }

    LaunchedEffect(achievement?.key) {
        if (achievement != null) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(1_400, easing = Motion.Decelerate))
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(Motion.Medium)),
        exit = fadeOut(tween(Motion.Quick)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val accent = gradients.accent(achievement?.accentIndex ?: 0)

            Canvas(Modifier.fillMaxSize()) {
                val progress = burst.value
                if (progress <= 0f) return@Canvas
                val random = Random(achievement?.key?.hashCode() ?: 0)
                val origin = Offset(size.width / 2f, size.height / 2f)
                repeat(34) { index ->
                    val angle = (index / 34f) * 2f * PI.toFloat() +
                        random.nextFloat() * 0.28f
                    val distance = size.minDimension *
                        (0.16f + random.nextFloat() * 0.42f) * easeOut(progress)
                    val position = Offset(
                        origin.x + cos(angle) * distance,
                        origin.y + sin(angle) * distance * 0.9f,
                    )
                    val radius = (2.5f + random.nextFloat() * 4.5f) * (1f - progress * 0.55f)
                    drawCircle(
                        color = accent[index % accent.size]
                            .copy(alpha = (1f - progress).coerceIn(0f, 1f) * 0.85f),
                        radius = radius.dp.toPx() / 2.4f,
                        center = position,
                    )
                }
            }

            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(Motion.bouncy(), initialScale = 0.75f) + fadeIn(),
                exit = scaleOut(targetScale = 0.9f) + fadeOut(),
            ) {
                SoftCard(
                    modifier = Modifier
                        .padding(Spacing.xxl)
                        .fillMaxWidth(),
                    shape = Radii.cardLarge,
                    contentPadding = Spacing.xxl,
                    elevation = 24.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(accent)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (achievement != null) {
                            Icon(
                                imageVector = achievement.icon.vector(),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(Spacing.l))
                    Text(
                        text = "Milestone reached",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = achievement?.title.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        text = achievement?.note.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.xl))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = Radii.chip,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Continue", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

private fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t)
