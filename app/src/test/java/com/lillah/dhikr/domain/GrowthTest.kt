package com.lillah.dhikr.domain

import com.lillah.dhikr.domain.gamification.GrowthStage
import com.lillah.dhikr.domain.gamification.GrowthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthTest {

    @Test
    fun `nothing counted is still a seed`() {
        assertEquals(GrowthStage.Seed, GrowthStage.forTotal(0))
    }

    @Test
    fun `a stage begins exactly at its threshold`() {
        assertEquals(GrowthStage.Sprout, GrowthStage.forTotal(100))
        assertEquals(GrowthStage.Seed, GrowthStage.forTotal(99))
    }

    @Test
    fun `progress runs from one threshold to the next`() {
        val halfway = GrowthState.forTotal(300)
        assertEquals(GrowthStage.Sprout, halfway.stage)
        assertEquals(GrowthStage.Sapling, halfway.nextStage)
        assertEquals(0.5f, halfway.progressToNext, 0.001f)
        assertEquals(200L, halfway.remainingToNext)
    }

    @Test
    fun `the final stage reports itself as complete`() {
        val grown = GrowthState.forTotal(1_000_000)
        assertEquals(GrowthStage.Garden, grown.stage)
        assertNull(grown.nextStage)
        assertEquals(1f, grown.progressToNext, 0.001f)
        assertEquals(0L, grown.remainingToNext)
    }

    @Test
    fun `stages only ever move forward as the total grows`() {
        var previous = -1
        listOf(0L, 50L, 100L, 499L, 500L, 2_000L, 10_000L, 50_000L, 150_000L).forEach { total ->
            val ordinal = GrowthStage.forTotal(total).ordinal
            assertTrue("growth must be monotonic at $total", ordinal >= previous)
            previous = ordinal
        }
    }
}
