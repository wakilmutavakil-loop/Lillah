package com.lillah.dhikr.domain

import com.lillah.dhikr.domain.sync.contributionPercent
import com.lillah.dhikr.domain.sync.formatContributionPercent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContributionTest {

    @Test
    fun `share is the user's total over the worldwide total`() {
        assertEquals(0.00097, contributionPercent(12_450, 1_284_563_920), 0.00001)
        assertEquals(0.0015, contributionPercent(30_000, 2_000_000_000), 0.0001)
        assertEquals(50.0, contributionPercent(50, 100), 0.0001)
        assertEquals(100.0, contributionPercent(100, 100), 0.0001)
    }

    @Test
    fun `an empty world is zero rather than a division by zero`() {
        assertEquals(0.0, contributionPercent(0, 0), 0.0)
        assertEquals(0.0, contributionPercent(500, 0), 0.0)
        assertEquals(0.0, contributionPercent(0, 1_000), 0.0)
    }

    @Test
    fun `a large worldwide total does not lose precision`() {
        // Integer arithmetic would floor this to zero; the calculation is in doubles for a reason.
        val percent = contributionPercent(1, 1_000_000_000_000L)
        assertTrue(percent > 0.0)
    }

    @Test
    fun `formatting keeps small shares legible instead of rounding them to zero`() {
        assertEquals("0.00097%", formatContributionPercent(0.00097))
        assertEquals("0.0015%", formatContributionPercent(0.0015))
        assertEquals("12.5%", formatContributionPercent(12.5))
        assertEquals("1.25%", formatContributionPercent(1.25))
        assertEquals("0%", formatContributionPercent(0.0))
    }

    @Test
    fun `a vanishingly small share says so rather than showing zero`() {
        assertEquals("< 0.00001%", formatContributionPercent(0.0000001))
    }

    @Test
    fun `the share never exceeds one hundred percent for a coherent pair`() {
        val percent = contributionPercent(1_000, 1_000)
        assertTrue(percent <= 100.0)
    }
}
