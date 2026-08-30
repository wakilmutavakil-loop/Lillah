package com.lillah.dhikr.ui.screens.progress

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lillah.dhikr.core.di.AppContainer
import com.lillah.dhikr.domain.gamification.AchievementStatus
import com.lillah.dhikr.domain.gamification.GrowthState
import com.lillah.dhikr.domain.model.BreakdownItem
import com.lillah.dhikr.domain.model.MonthStats
import com.lillah.dhikr.domain.model.StreakInfo
import com.lillah.dhikr.domain.model.WeekStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class ProgressRange(val label: String) { Day("Day"), Week("Week"), Month("Month") }

@Immutable
data class ProgressUiState(
    val isLoading: Boolean = true,
    val todayTotal: Int = 0,
    val dailyGoal: Int = 100,
    val breakdown: List<BreakdownItem> = emptyList(),
    val week: WeekStats = WeekStats(),
    val month: MonthStats = MonthStats(),
    val streak: StreakInfo = StreakInfo(),
    val growth: GrowthState = GrowthState(),
    val lifetimeTotal: Long = 0,
    val activeDays: Int = 0,
    val achievements: List<AchievementStatus> = emptyList(),
    val today: LocalDate = LocalDate.now(),
) {
    val goalFraction: Float
        get() = (todayTotal.toFloat() / dailyGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val unlockedCount: Int get() = achievements.count { it.isUnlocked }
}

@Immutable
private data class RangeBundle(
    val week: WeekStats,
    val month: MonthStats,
    val lifetime: Long,
    val activeDays: Int,
)

class ProgressViewModel(container: AppContainer) : ViewModel() {

    private val statsRepository = container.statsRepository
    private val gamification = container.gamificationRepository
    private val settingsRepository = container.settingsRepository
    private val clock = container.clock

    private val _range = MutableStateFlow(ProgressRange.Day)
    val range: StateFlow<ProgressRange> = _range.asStateFlow()

    private val rangeBundle = combine(
        statsRepository.observeWeek(),
        statsRepository.observeMonth(),
        statsRepository.observeLifetimeTotal(),
        statsRepository.observeActiveDayCount(),
    ) { week, month, lifetime, activeDays -> RangeBundle(week, month, lifetime, activeDays) }

    val uiState: StateFlow<ProgressUiState> = combine(
        combine(
            statsRepository.observeTodayTotal(),
            statsRepository.observeTodayBreakdown(),
            settingsRepository.settings,
        ) { total, breakdown, settings -> Triple(total, breakdown, settings.dailyGoal) },
        rangeBundle,
        statsRepository.observeStreak(),
        statsRepository.observeGrowth(),
        gamification.observeStatuses(),
    ) { today, bundle, streak, growth, achievements ->
        val (total, breakdown, goal) = today
        ProgressUiState(
            isLoading = false,
            todayTotal = total,
            dailyGoal = goal,
            breakdown = breakdown,
            week = bundle.week,
            month = bundle.month,
            streak = streak,
            growth = growth,
            lifetimeTotal = bundle.lifetime,
            activeDays = bundle.activeDays,
            achievements = achievements,
            today = clock.today(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun selectRange(range: ProgressRange) {
        _range.value = range
    }
}
