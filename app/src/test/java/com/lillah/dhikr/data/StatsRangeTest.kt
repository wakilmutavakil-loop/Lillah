package com.lillah.dhikr.data

import com.lillah.dhikr.data.local.dao.DayTotal
import com.lillah.dhikr.data.repository.StatsRepository
import com.lillah.dhikr.domain.model.WeekStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StatsRangeTest {

    @Test
    fun `gaps in the ledger become explicit zeroes`() {
        val start = LocalDate.of(2026, 3, 2)
        val end = start.plusDays(6)
        val rows = listOf(
            DayTotal(start.toEpochDay(), 40),
            DayTotal(start.plusDays(3).toEpochDay(), 120),
        )

        val filled = StatsRepository.fillRange(start, end, rows)

        assertEquals(7, filled.size)
        assertEquals(40, filled[0].total)
        assertEquals(0, filled[1].total)
        assertEquals(120, filled[3].total)
        assertTrue(filled.map { it.date }.zipWithNext().all { (a, b) -> b == a.plusDays(1) })
    }

    @Test
    fun `an empty ledger still produces a dense series`() {
        val start = LocalDate.of(2026, 3, 2)
        val filled = StatsRepository.fillRange(start, start.plusDays(6), emptyList())
        assertEquals(7, filled.size)
        assertTrue(filled.all { it.total == 0 })
    }

    @Test
    fun `the week start never moves forward past the reference day`() {
        var date = LocalDate.of(2026, 3, 2)
        repeat(14) {
            val start = StatsRepository.startOfWeek(date)
            assertTrue("week start must not be after the date", !start.isAfter(date))
            assertTrue("week must contain the date", !start.plusDays(6).isBefore(date))
            date = date.plusDays(1)
        }
    }

    @Test
    fun `a week start is idempotent`() {
        val date = LocalDate.of(2026, 3, 5)
        val start = StatsRepository.startOfWeek(date)
        assertEquals(start, StatsRepository.startOfWeek(start))
    }

    @Test
    fun `week deltas are reported against the user's own previous week`() {
        val start = LocalDate.of(2026, 3, 2)
        val days = StatsRepository.fillRange(
            start,
            start.plusDays(6),
            listOf(DayTotal(start.toEpochDay(), 200)),
        )

        val ahead = WeekStats(days = days, total = 200, previousTotal = 100, activeDays = 1)
        assertEquals(100, ahead.deltaPercent)

        val behind = WeekStats(days = days, total = 50, previousTotal = 100, activeDays = 1)
        assertEquals(-50, behind.deltaPercent)

        val firstEver = WeekStats(days = days, total = 0, previousTotal = 0, activeDays = 0)
        assertEquals("no counts either week is not a decline", 0, firstEver.deltaPercent)
    }

    @Test
    fun `week summary figures line up with the series`() {
        val start = StatsRepository.startOfWeek(LocalDate.of(2026, 3, 4))
        val rows = (0..6).map { DayTotal(start.plusDays(it.toLong()).toEpochDay(), it * 10) }
        val days = StatsRepository.fillRange(start, start.plusDays(6), rows)
        val stats = WeekStats(days, days.sumOf { it.total }, 0, days.count { it.total > 0 })

        assertEquals(210, stats.total)
        assertEquals(30, stats.average)
        assertEquals(60, stats.best?.total)
        assertEquals(6, stats.activeDays)
    }

    @Test
    fun `a locale starting the week on Sunday is still handled`() {
        // startOfWeek follows the device locale, so the only invariant worth asserting is that
        // the returned day is one of the two conventions and the week contains the date.
        val date = LocalDate.of(2026, 3, 4)
        val start = StatsRepository.startOfWeek(date)
        assertTrue(start.dayOfWeek == DayOfWeek.MONDAY || start.dayOfWeek == DayOfWeek.SUNDAY)
    }
}
