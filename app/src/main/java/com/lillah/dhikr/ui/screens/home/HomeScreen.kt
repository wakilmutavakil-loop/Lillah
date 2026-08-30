package com.lillah.dhikr.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.core.time.DateFormats
import com.lillah.dhikr.core.time.grouped
import com.lillah.dhikr.domain.model.CollectionProgress
import com.lillah.dhikr.domain.model.Dhikr
import com.lillah.dhikr.ui.components.MilestoneCelebration
import com.lillah.dhikr.ui.components.ProgressRing
import com.lillah.dhikr.ui.components.SoftCard
import com.lillah.dhikr.ui.components.Sparkline
import com.lillah.dhikr.ui.components.TargetSheet
import com.lillah.dhikr.ui.components.TasbihCounter
import com.lillah.dhikr.ui.components.softShadow
import com.lillah.dhikr.ui.theme.ArabicText
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Radii
import com.lillah.dhikr.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    state: HomeUiState,
    onCount: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onSelectDhikr: (Long) -> Unit,
    onSetTarget: (Int) -> Unit,
    onAddDhikr: () -> Unit,
    onOpenCollection: (Long) -> Unit,
    onOpenProgress: () -> Unit,
    onDismissMilestone: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    now: LocalTime = LocalTime.now(),
    today: LocalDate = LocalDate.now(),
) {
    var targetSheetVisible by remember { mutableStateOf(false) }

    Box(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.s))

            GreetingRow(
                greeting = greetingFor(now),
                date = DateFormats.full(today),
                streak = state.streak.current,
                streakActiveToday = state.streak.activeToday,
                onStreakClick = onOpenProgress,
            )

            Spacer(Modifier.height(Spacing.l))

            TodayStrip(
                total = state.todayTotal,
                goal = state.settings.dailyGoal,
                fraction = state.goalFraction,
                goalMet = state.goalMet,
                recentDays = state.recentDays,
                onClick = onOpenProgress,
            )

            AnimatedVisibility(
                visible = state.timeSuggestion != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val suggestion = state.timeSuggestion
                if (suggestion != null) {
                    Column {
                        Spacer(Modifier.height(Spacing.m))
                        SuggestionCard(
                            suggestion = suggestion,
                            onClick = { onOpenCollection(suggestion.collection.id) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            DhikrHeadline(
                dhikr = state.activeDhikr,
                showArabic = state.settings.showArabic,
                showTransliteration = state.settings.showTransliteration,
                showMeaning = state.settings.showMeaning,
            )

            Spacer(Modifier.height(Spacing.xl))

            TasbihCounter(
                count = state.displayCount,
                target = state.activeDhikr?.safeTarget ?: 33,
                roundsToday = state.activeDhikr?.let {
                    if (it.roundsEpochDay == today.toEpochDay()) it.roundsToday else 0
                } ?: 0,
                onTap = onCount,
                onUndo = onUndo,
                celebrationKey = state.celebrationKey,
                enabled = state.activeDhikr != null,
            )

            Spacer(Modifier.height(Spacing.l))

            Text(
                text = if (state.activeDhikrToday > 0) {
                    "${state.activeDhikrToday.grouped()} today for this dhikr"
                } else {
                    "Tap the circle to begin"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.l))

            CounterActions(
                onUndo = onUndo,
                onReset = onReset,
                onAdjustTarget = { targetSheetVisible = true },
            )

            Spacer(Modifier.height(Spacing.xxl))

            QuickPicks(
                dhikr = state.quickPicks,
                activeId = state.activeDhikr?.id,
                onSelect = onSelectDhikr,
                onAdd = onAddDhikr,
            )

            Spacer(Modifier.height(Spacing.xl))
        }

        MilestoneCelebration(
            achievement = state.pendingMilestone,
            onDismiss = onDismissMilestone,
        )
    }

    if (targetSheetVisible) {
        TargetSheet(
            current = state.activeDhikr?.safeTarget ?: 33,
            onDismiss = { targetSheetVisible = false },
            onConfirm = {
                onSetTarget(it)
                targetSheetVisible = false
            },
        )
    }
}

@Composable
private fun GreetingRow(
    greeting: String,
    date: String,
    streak: Int,
    streakActiveToday: Boolean,
    onStreakClick: () -> Unit,
) {
    val gradients = LocalAppGradients.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier
                .clip(Radii.chip)
                .background(
                    if (streak > 0) {
                        Brush.linearGradient(gradients.accent(3))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                            )
                        )
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onStreakClick,
                )
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (streak > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (streak > 0) "$streak day${if (streak == 1) "" else "s"}" else "Start today",
                style = MaterialTheme.typography.labelMedium,
                color = if (streak > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TodayStrip(
    total: Int,
    goal: Int,
    fraction: Float,
    goalMet: Boolean,
    recentDays: List<com.lillah.dhikr.domain.model.DayPoint>,
    onClick: () -> Unit,
) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = Spacing.l,
        elevation = 8.dp,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = fraction,
                diameter = 54.dp,
                strokeWidth = 6.dp,
            ) {
                Text(
                    text = "${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.width(Spacing.l))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${total.grouped()} of ${goal.grouped()} today",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when {
                        goalMet -> "Today's goal is met. Anything more is a gift."
                        total == 0 -> "Nothing counted yet today."
                        else -> "${(goal - total).grouped()} to go."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (recentDays.size >= 2 && recentDays.any { it.total > 0 }) {
            Spacer(Modifier.height(Spacing.m))
            Sparkline(days = recentDays, height = 44.dp)
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: CollectionProgress,
    onClick: () -> Unit,
) {
    val gradients = LocalAppGradients.current
    val accent = gradients.accent(suggestion.collection.accentIndex)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(12.dp, Radii.card, accent.first(), alpha = 0.3f)
            .clip(Radii.card)
            .background(Brush.linearGradient(accent))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressRing(
            progress = suggestion.fraction,
            diameter = 44.dp,
            strokeWidth = 5.dp,
            colors = listOf(Color.White, Color.White),
            trackColor = Color.White.copy(alpha = 0.28f),
        ) {
            Text(
                text = "${suggestion.completedToday}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Time for ${suggestion.collection.name}",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
            Text(
                text = "${suggestion.completedToday} of ${suggestion.itemCount} done",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Text(
            text = "Open",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@Composable
private fun DhikrHeadline(
    dhikr: Dhikr?,
    showArabic: Boolean,
    showTransliteration: Boolean,
    showMeaning: Boolean,
) {
    if (dhikr == null) {
        Text(
            text = "Choose a dhikr to begin",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showArabic && !dhikr.arabic.isNullOrBlank()) {
            Text(
                text = dhikr.arabic,
                style = ArabicText.Title,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.s))
        }
        Text(
            text = dhikr.displayTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        if (showTransliteration && !dhikr.transliteration.isNullOrBlank()) {
            Text(
                text = dhikr.transliteration,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showMeaning && !dhikr.meaning.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = dhikr.meaning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CounterActions(
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onAdjustTarget: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        CircleAction(Icons.AutoMirrored.Rounded.Undo, "Undo one", onUndo)
        CircleAction(Icons.Rounded.RestartAlt, "Reset round", onReset)
        CircleAction(Icons.Rounded.Tune, "Change target", onAdjustTarget)
    }
}

@Composable
private fun CircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun QuickPicks(
    dhikr: List<Dhikr>,
    activeId: Long?,
    onSelect: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    val gradients = LocalAppGradients.current

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Switch dhikr",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.s),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            contentPadding = PaddingValues(end = Spacing.s),
        ) {
            items(dhikr, key = { it.id }) { item ->
                val selected = item.id == activeId
                val accent = gradients.accent(item.accentIndex)
                Column(
                    modifier = Modifier
                        .width(126.dp)
                        .clip(Radii.tile)
                        .then(
                            if (selected) {
                                Modifier.background(Brush.linearGradient(accent))
                            } else {
                                Modifier
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                        Radii.tile,
                                    )
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(item.id) }
                        .padding(Spacing.m),
                ) {
                    Text(
                        text = item.displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "×${item.targetCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) {
                            Color.White.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 68.dp)
                        .clip(Radii.tile)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onAdd,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add a dhikr",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

internal fun greetingFor(time: LocalTime): String = when (time.hour) {
    in 0..3 -> "Peaceful night"
    in 4..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..20 -> "Good evening"
    else -> "Peaceful night"
}
