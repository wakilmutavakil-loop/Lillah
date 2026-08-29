package com.lillah.dhikr.domain.gamification

import com.lillah.dhikr.domain.model.StreakInfo

/**
 * Streaks are deliberately forgiving. A day counts if *anything* was remembered on it — not if a
 * goal was hit — and today's silence never breaks a streak, because the day is not over yet.
 */
object Streaks {

    fun compute(activeDays: Collection<Long>, todayEpochDay: Long): StreakInfo {
        if (activeDays.isEmpty()) return StreakInfo()
        val days = activeDays.toHashSet()
        val activeToday = days.contains(todayEpochDay)

        // Anchor on today when it already has activity, otherwise on yesterday: an untouched
        // today is still in progress, so it should not zero out a run.
        val anchor = when {
            activeToday -> todayEpochDay
            days.contains(todayEpochDay - 1) -> todayEpochDay - 1
            else -> null
        }

        var current = 0
        if (anchor != null) {
            var cursor = anchor
            while (days.contains(cursor)) {
                current++
                cursor--
            }
        }

        return StreakInfo(
            current = current,
            best = longestRun(days),
            activeToday = activeToday,
            atRisk = !activeToday && current > 0,
        )
    }

    fun longestRun(activeDays: Collection<Long>): Int {
        if (activeDays.isEmpty()) return 0
        val sorted = activeDays.toSortedSet()
        var best = 0
        var run = 0
        var previous: Long? = null
        for (day in sorted) {
            run = if (previous != null && day == previous + 1) run + 1 else 1
            if (run > best) best = run
            previous = day
        }
        return best
    }
}
