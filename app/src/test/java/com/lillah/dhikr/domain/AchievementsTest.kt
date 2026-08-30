package com.lillah.dhikr.domain

import com.lillah.dhikr.domain.gamification.AchievementCatalog
import com.lillah.dhikr.domain.gamification.GamificationSnapshot
import com.lillah.dhikr.domain.gamification.Metric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {

    @Test
    fun `keys are unique`() {
        val keys = AchievementCatalog.all.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `every goal is reachable`() {
        assertTrue(AchievementCatalog.all.all { it.goal > 0 })
    }

    @Test
    fun `an empty snapshot unlocks nothing`() {
        assertTrue(AchievementCatalog.satisfied(GamificationSnapshot()).isEmpty())
    }

    @Test
    fun `a single count unlocks only the first milestone`() {
        val satisfied = AchievementCatalog.satisfied(GamificationSnapshot(lifetimeTotal = 1))
        assertEquals(listOf("first_remembrance"), satisfied)
    }

    @Test
    fun `lifetime milestones unlock cumulatively`() {
        val satisfied = AchievementCatalog.satisfied(GamificationSnapshot(lifetimeTotal = 1_000))
        assertTrue(satisfied.containsAll(listOf("first_remembrance", "hundred", "thousand")))
        assertFalse(satisfied.contains("ten_thousand"))
    }

    @Test
    fun `metrics are read independently of one another`() {
        val snapshot = GamificationSnapshot(currentStreak = 7, lifetimeTotal = 5)
        val satisfied = AchievementCatalog.satisfied(snapshot)
        assertTrue(satisfied.contains("streak_7"))
        assertTrue(satisfied.contains("streak_3"))
        assertFalse(satisfied.contains("hundred"))
    }

    @Test
    fun `progress is capped at the goal so a bar never overflows`() {
        val snapshot = GamificationSnapshot(lifetimeTotal = 999_999)
        val statuses = AchievementCatalog.statuses(snapshot, emptyMap())
        statuses.forEach { status ->
            assertTrue(status.progress <= status.def.goal)
            assertTrue(status.fraction <= 1f)
        }
    }

    @Test
    fun `unlocked milestones sort ahead of locked ones`() {
        val snapshot = GamificationSnapshot(lifetimeTotal = 150)
        val statuses = AchievementCatalog.statuses(
            snapshot,
            mapOf("first_remembrance" to 1L, "hundred" to 2L),
        )
        assertTrue(statuses.take(2).all { it.isUnlocked })
        assertFalse(statuses.last().isUnlocked)
    }

    @Test
    fun `every metric is covered by at least one milestone`() {
        val used = AchievementCatalog.all.map { it.metric }.toSet()
        Metric.entries.forEach { metric ->
            assertTrue("no milestone uses $metric", used.contains(metric))
        }
    }
}
