package com.lillah.dhikr.domain.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
data class DayPoint(val date: LocalDate, val total: Int)

@Immutable
data class BreakdownItem(
    val dhikrId: Long,
    val name: String,
    val arabic: String?,
    val accentIndex: Int,
    val total: Int,
)

@Immutable
data class StreakInfo(
    val current: Int = 0,
    val best: Int = 0,
    val activeToday: Boolean = false,
    /** True when yesterday carried the streak and today is still open — a nudge, never a scolding. */
    val atRisk: Boolean = false,
)

@Immutable
data class DailySummary(
    val date: LocalDate,
    val total: Int,
    val goal: Int,
    val breakdown: List<BreakdownItem> = emptyList(),
    val collectionsCompleted: List<String> = emptyList(),
) {
    val fraction: Float get() = (total.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val goalMet: Boolean get() = total >= goal
}

@Immutable
data class WeekStats(
    val days: List<DayPoint> = emptyList(),
    val total: Int = 0,
    val previousTotal: Int = 0,
    val activeDays: Int = 0,
) {
    val average: Int get() = if (days.isEmpty()) 0 else total / days.size
    val best: DayPoint? get() = days.maxByOrNull { it.total }
    val deltaPercent: Int
        get() = when {
            previousTotal == 0 && total == 0 -> 0
            previousTotal == 0 -> 100
            else -> (((total - previousTotal).toFloat() / previousTotal) * 100).toInt()
        }
}

@Immutable
data class MonthStats(
    val month: LocalDate = LocalDate.now().withDayOfMonth(1),
    val days: List<DayPoint> = emptyList(),
    val total: Int = 0,
    val activeDays: Int = 0,
) {
    val best: DayPoint? get() = days.maxByOrNull { it.total }
    val peak: Int get() = days.maxOfOrNull { it.total } ?: 0
}
