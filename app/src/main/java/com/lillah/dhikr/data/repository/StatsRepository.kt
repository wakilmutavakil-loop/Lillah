package com.lillah.dhikr.data.repository

import com.lillah.dhikr.core.time.AppClock
import com.lillah.dhikr.data.local.dao.CountDao
import com.lillah.dhikr.data.local.dao.DayTotal
import com.lillah.dhikr.domain.gamification.GrowthState
import com.lillah.dhikr.domain.gamification.Streaks
import com.lillah.dhikr.domain.model.BreakdownItem
import com.lillah.dhikr.domain.model.DayPoint
import com.lillah.dhikr.domain.model.MonthStats
import com.lillah.dhikr.domain.model.StreakInfo
import com.lillah.dhikr.domain.model.WeekStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

class StatsRepository(
    private val countDao: CountDao,
    private val clock: AppClock,
) {

    /**
     * Emits the current day and re-emits when the date turns over.
     *
     * Everything scoped to "today" hangs off this. Without it, an app left open overnight would
     * keep counting into yesterday's row and show a stale total in the morning.
     */
    private fun today(): Flow<Long> = flow {
        while (true) {
            emit(clock.todayEpochDay())
            val nextMidnight = clock.today().plusDays(1)
                .atStartOfDay(clock.zone()).toInstant().toEpochMilli()
            delay((nextMidnight - clock.nowMillis()).coerceIn(1_000L, MAX_SLEEP_MILLIS))
        }
    }

    fun observeTodayTotal(): Flow<Int> =
        today().flatMapLatest { day -> countDao.observeDayTotal(day) }

    fun observeLifetimeTotal(): Flow<Long> = countDao.observeLifetimeTotal()

    fun observeGrowth(): Flow<GrowthState> =
        countDao.observeLifetimeTotal().map { GrowthState.forTotal(it) }

    fun observeStreak(): Flow<StreakInfo> = today().flatMapLatest { day ->
        countDao.observeActiveDays().map { Streaks.compute(it, day) }
    }

    fun observeActiveDayCount(): Flow<Int> = countDao.observeActiveDayCount()

    fun observeTodayBreakdown(): Flow<List<BreakdownItem>> = today().flatMapLatest { day ->
        countDao.observeDayBreakdown(day).map { rows ->
            rows.map { BreakdownItem(it.dhikrId, it.name, it.arabic, it.accentIndex, it.total) }
        }
    }

    fun observeDhikrTodayCounts(): Flow<Map<Long, Int>> = today().flatMapLatest { day ->
        countDao.observeDayBreakdown(day).map { rows -> rows.associate { it.dhikrId to it.total } }
    }

    /** The week containing today, plus the week before it for comparison. */
    fun observeWeek(): Flow<WeekStats> = today().flatMapLatest { weekStats(clock.today()) }

    /** The locale's week containing [reference], plus the week before it for comparison. */
    fun weekStats(reference: LocalDate): Flow<WeekStats> {
        val start = startOfWeek(reference)
        val end = start.plusDays(6)
        val previousStart = start.minusWeeks(1)

        return combine(
            countDao.observeDayTotals(start.toEpochDay(), end.toEpochDay()),
            countDao.observeDayTotals(previousStart.toEpochDay(), start.minusDays(1).toEpochDay()),
        ) { current, previous ->
            val days = fillRange(start, end, current)
            WeekStats(
                days = days,
                total = days.sumOf { it.total },
                previousTotal = previous.sumOf { it.total },
                activeDays = days.count { it.total > 0 },
            )
        }
    }

    fun observeMonth(): Flow<MonthStats> = today().flatMapLatest { monthStats(clock.today()) }

    fun monthStats(reference: LocalDate): Flow<MonthStats> {
        val first = reference.withDayOfMonth(1)
        val last = first.plusMonths(1).minusDays(1)
        return countDao.observeDayTotals(first.toEpochDay(), last.toEpochDay()).map { rows ->
            val days = fillRange(first, last, rows)
            MonthStats(
                month = first,
                days = days,
                total = days.sumOf { it.total },
                activeDays = days.count { it.total > 0 },
            )
        }
    }

    /** Rolling window used by the sparkline on the home screen. */
    fun observeRecentDays(count: Int = 14): Flow<List<DayPoint>> = today().flatMapLatest {
        val end = clock.today()
        val start = end.minusDays((count - 1).toLong())
        countDao.observeDayTotals(start.toEpochDay(), end.toEpochDay())
            .map { rows -> fillRange(start, end, rows) }
    }

    /** Calendar year to date, re-anchored when the day rolls over. */
    fun observeYearTotal(): Flow<Int> = today().flatMapLatest {
        val start = clock.today().withDayOfYear(1)
        val end = clock.today()
        countDao.observeDayTotals(start.toEpochDay(), end.toEpochDay())
            .map { rows -> rows.sumOf { it.total } }
    }

    fun observeMonthTotal(): Flow<Int> = observeMonth().map { it.total }

    fun observeWeekTotal(): Flow<Int> = observeWeek().map { it.total }

    suspend fun todayTotal(): Int = countDao.dayTotal(clock.todayEpochDay())

    suspend fun clearToday() = countDao.clearDay(clock.todayEpochDay())

    suspend fun clearAllHistory() = countDao.clearAll()

    companion object {
        /** Cap on a single sleep, so a clock jump cannot park the ticker for days. */
        private const val MAX_SLEEP_MILLIS = 6 * 60 * 60 * 1000L

        fun startOfWeek(date: LocalDate): LocalDate {
            val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
            val delta = (date.dayOfWeek.value - firstDay.value + 7) % 7
            return date.minusDays(delta.toLong())
        }

        /** Gaps in the ledger are real zeroes, so charts get a dense series either way. */
        fun fillRange(start: LocalDate, end: LocalDate, rows: List<DayTotal>): List<DayPoint> {
            val byDay = rows.associate { it.epochDay to it.total }
            val days = mutableListOf<DayPoint>()
            var cursor = start
            while (!cursor.isAfter(end)) {
                days += DayPoint(cursor, byDay[cursor.toEpochDay()] ?: 0)
                cursor = cursor.plusDays(1)
            }
            return days
        }
    }
}
