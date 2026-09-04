package com.lillah.dhikr.ui.screens.progress

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.core.time.DateFormats
import com.lillah.dhikr.core.time.grouped
import com.lillah.dhikr.domain.model.BreakdownItem
import com.lillah.dhikr.ui.components.AchievementBadge
import com.lillah.dhikr.ui.components.MonthHeatmap
import com.lillah.dhikr.ui.components.ProgressRing
import com.lillah.dhikr.ui.components.SectionHeader
import com.lillah.dhikr.ui.components.SegmentedSelector
import com.lillah.dhikr.ui.components.SoftCard
import com.lillah.dhikr.ui.components.sheenOverlay
import com.lillah.dhikr.ui.components.StatTile
import com.lillah.dhikr.ui.components.WeeklyBars
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Radii
import com.lillah.dhikr.ui.theme.Spacing

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    range: ProgressRange,
    onSelectRange: (ProgressRange) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
    ) {
        item {
            Column(Modifier.padding(horizontal = Spacing.screen)) {
                Spacer(Modifier.height(Spacing.s))
                SectionHeader(
                    title = "Progress",
                    subtitle = "Only ever measured against yourself",
                )
                Spacer(Modifier.height(Spacing.l))
                SegmentedSelector(
                    options = ProgressRange.entries.map { it.label },
                    selectedIndex = ProgressRange.entries.indexOf(range),
                    onSelect = { onSelectRange(ProgressRange.entries[it]) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.l))
            }
        }

        item {
            Box(Modifier.padding(horizontal = Spacing.screen)) {
                AnimatedContent(
                    targetState = range,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "range",
                ) { selected ->
                    when (selected) {
                        ProgressRange.Day -> DaySection(state)
                        ProgressRange.Week -> WeekSection(state)
                        ProgressRange.Month -> MonthSection(state)
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = Spacing.screen)) {
                Spacer(Modifier.height(Spacing.xl))
                GardenCard(state)
                Spacer(Modifier.height(Spacing.m))
                StreakCard(state)
                Spacer(Modifier.height(Spacing.xl))
                SectionHeader(
                    title = "Milestones",
                    subtitle = "${state.unlockedCount} of ${state.achievements.size} reached",
                )
                Spacer(Modifier.height(Spacing.m))
            }
        }

        items(
            count = (state.achievements.size + 1) / 2,
            key = { rowIndex -> state.achievements[rowIndex * 2].def.key },
        ) { rowIndex ->
            val first = state.achievements.getOrNull(rowIndex * 2)
            val second = state.achievements.getOrNull(rowIndex * 2 + 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screen, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                if (first != null) {
                    Box(Modifier.weight(1f)) { AchievementBadge(first) }
                }
                if (second != null) {
                    Box(Modifier.weight(1f)) { AchievementBadge(second) }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

@Composable
private fun DaySection(state: ProgressUiState) {
    Column {
        SoftCard(Modifier.fillMaxWidth(), contentPadding = Spacing.xl) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = state.goalFraction,
                    diameter = 112.dp,
                    strokeWidth = 12.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.todayTotal.grouped(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "of ${state.dailyGoal.grouped()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.size(Spacing.xl))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = DateFormats.full(state.today),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = when {
                            state.todayTotal >= state.dailyGoal ->
                                "Goal met. Everything after this is extra."
                            state.todayTotal == 0 ->
                                "The day is still open."
                            else ->
                                "${(state.dailyGoal - state.todayTotal).grouped()} left to your goal."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.l))
        SectionHeader(title = "By dhikr", subtitle = "What today was made of")
        Spacer(Modifier.height(Spacing.m))

        if (state.breakdown.isEmpty()) {
            SoftCard(Modifier.fillMaxWidth()) {
                Text(
                    text = "Nothing counted yet today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val peak = state.breakdown.maxOf { it.total }.coerceAtLeast(1)
            state.breakdown.forEach { item ->
                BreakdownRow(item = item, peak = peak)
                Spacer(Modifier.height(Spacing.s))
            }
        }
    }
}

@Composable
private fun BreakdownRow(item: BreakdownItem, peak: Int) {
    val gradients = LocalAppGradients.current
    val accent = gradients.accent(item.accentIndex)

    SoftCard(Modifier.fillMaxWidth(), contentPadding = Spacing.l, elevation = 5.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.total.grouped(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(Spacing.s))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(Radii.chip)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth((item.total.toFloat() / peak).coerceIn(0.02f, 1f))
                    .height(6.dp)
                    .clip(Radii.chip)
                    .background(Brush.horizontalGradient(accent))
            )
        }
    }
}

@Composable
private fun WeekSection(state: ProgressUiState) {
    val week = state.week
    Column {
        SoftCard(Modifier.fillMaxWidth(), contentPadding = Spacing.xl) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = week.total.grouped(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "this week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TrendChip(week.deltaPercent)
            }
            Spacer(Modifier.height(Spacing.xl))
            WeeklyBars(days = week.days, goal = state.dailyGoal, today = state.today)
        }

        Spacer(Modifier.height(Spacing.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            StatTile(
                value = week.activeDays.toString(),
                label = "days active",
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.CalendarMonth,
            )
            StatTile(
                value = week.average.grouped(),
                label = "daily average",
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
            )
            StatTile(
                value = (week.best?.total ?: 0).grouped(),
                label = "best day",
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Spa,
            )
        }

        Spacer(Modifier.height(Spacing.m))
        SoftCard(Modifier.fillMaxWidth(), contentPadding = Spacing.l) {
            Text(
                text = when {
                    week.previousTotal == 0 && week.total == 0 ->
                        "No counts yet this week or last. A single dhikr is a fine place to start."
                    week.previousTotal == 0 ->
                        "Your first full week of counting. There is nothing to compare it to yet."
                    week.total >= week.previousTotal ->
                        "You are ${(week.total - week.previousTotal).grouped()} ahead of last week."
                    else ->
                        "A quieter week than the last one. Weeks differ; the habit is what carries."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthSection(state: ProgressUiState) {
    val month = state.month
    Column {
        SoftCard(Modifier.fillMaxWidth(), contentPadding = Spacing.xl) {
            Text(
                text = DateFormats.monthYear(month.month),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "${month.total.grouped()} counted across ${month.activeDays} days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xl))
            MonthHeatmap(stats = month, today = state.today)
        }

        Spacer(Modifier.height(Spacing.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            StatTile(
                value = month.activeDays.toString(),
                label = "days present",
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.CalendarMonth,
            )
            StatTile(
                value = (month.best?.total ?: 0).grouped(),
                label = "best day",
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
            )
            StatTile(
                value = state.activeDays.toString(),
                label = "days all time",
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Spa,
            )
        }
    }
}

@Composable
private fun TrendChip(deltaPercent: Int) {
    val icon = when {
        deltaPercent > 2 -> Icons.AutoMirrored.Rounded.TrendingUp
        deltaPercent < -2 -> Icons.AutoMirrored.Rounded.TrendingDown
        else -> Icons.AutoMirrored.Rounded.TrendingFlat
    }
    val label = when {
        deltaPercent > 2 -> "+$deltaPercent% vs last week"
        deltaPercent < -2 -> "$deltaPercent% vs last week"
        else -> "Steady"
    }
    Row(
        modifier = Modifier
            .clip(Radii.chip)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = Spacing.m, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Lifetime progress, framed as something growing rather than a score. There is no comparison to
 * anyone else here, and nothing about it can go down.
 */
@Composable
private fun GardenCard(state: ProgressUiState) {
    val gradients = LocalAppGradients.current
    val growth = state.growth

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radii.cardLarge)
            .background(Brush.linearGradient(gradients.hero))
            .sheenOverlay()
            .padding(Spacing.xl),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Spa,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.size(Spacing.m))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = growth.stage.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Text(
                        text = "${state.lifetimeTotal.grouped()} remembrances in all",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.l))
            Text(
                text = growth.stage.blurb,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f),
            )

            Spacer(Modifier.height(Spacing.l))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(Radii.chip)
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(growth.progressToNext)
                        .height(8.dp)
                        .clip(Radii.chip)
                        .background(Color.White)
                )
            }
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = growth.nextStage?.let {
                    "${growth.remainingToNext.grouped()} more until ${it.displayName}"
                } ?: "Fully grown. Keep tending it.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun StreakCard(state: ProgressUiState) {
    val gradients = LocalAppGradients.current
    val streak = state.streak

    SoftCard(Modifier.fillMaxWidth(), contentPadding = Spacing.xl) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradients.accent(3))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.LocalFireDepartment,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(23.dp),
                )
            }
            Spacer(Modifier.size(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (streak.current > 0) {
                        "${streak.current} day${if (streak.current == 1) "" else "s"} in a row"
                    } else {
                        "No streak running"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when {
                        streak.atRisk -> "Today is still open — anything at all keeps it going."
                        streak.current == 0 && streak.best > 0 ->
                            "Your longest run was ${streak.best} days. It is still yours."
                        streak.current > 0 && streak.current >= streak.best ->
                            "This is your longest run so far."
                        streak.best > 0 -> "Your longest run: ${streak.best} days."
                        else -> "One day is enough to start one."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
