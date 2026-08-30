package com.lillah.dhikr.domain

import com.lillah.dhikr.domain.gamification.Streaks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreaksTest {

    private val today = 20_000L

    @Test
    fun `no activity gives an empty streak`() {
        val info = Streaks.compute(emptyList(), today)
        assertEquals(0, info.current)
        assertEquals(0, info.best)
        assertFalse(info.activeToday)
        assertFalse(info.atRisk)
    }

    @Test
    fun `counting today starts a streak of one`() {
        val info = Streaks.compute(listOf(today), today)
        assertEquals(1, info.current)
        assertTrue(info.activeToday)
        assertFalse(info.atRisk)
    }

    @Test
    fun `consecutive days ending today accumulate`() {
        val days = listOf(today, today - 1, today - 2, today - 3)
        assertEquals(4, Streaks.compute(days, today).current)
    }

    @Test
    fun `an untouched today does not break a run that reached yesterday`() {
        val days = listOf(today - 1, today - 2, today - 3)
        val info = Streaks.compute(days, today)
        assertEquals("the day is not over, so the run still stands", 3, info.current)
        assertFalse(info.activeToday)
        assertTrue("the UI should nudge, not scold", info.atRisk)
    }

    @Test
    fun `a fully missed day ends the run`() {
        val days = listOf(today - 2, today - 3, today - 4)
        assertEquals(0, Streaks.compute(days, today).current)
    }

    @Test
    fun `the best run survives the current one ending`() {
        val days = listOf(today - 10, today - 9, today - 8, today - 7, today - 6, today)
        val info = Streaks.compute(days, today)
        assertEquals(1, info.current)
        assertEquals(5, info.best)
    }

    @Test
    fun `gaps are ignored when measuring the longest run`() {
        val days = listOf(1L, 2L, 3L, 10L, 11L, 20L, 21L, 22L, 23L)
        assertEquals(4, Streaks.longestRun(days))
    }

    @Test
    fun `duplicate days do not inflate a streak`() {
        val days = listOf(today, today, today - 1, today - 1)
        assertEquals(2, Streaks.compute(days, today).current)
    }

    @Test
    fun `unsorted input is handled`() {
        val days = listOf(today - 2, today, today - 1)
        assertEquals(3, Streaks.compute(days, today).current)
    }
}
