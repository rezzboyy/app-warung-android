package com.warung.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CashDenominationHelperTest {

    @Test
    fun `calculateSmartDenominations with zero or negative returns standard`() {
        val denoms = CashDenominationHelper.calculateSmartDenominations(0.0)
        assertEquals(4, denoms.size)
        assertEquals(10000.0, denoms[0], 0.01)
        assertEquals(20000.0, denoms[1], 0.01)
        assertEquals(50000.0, denoms[2], 0.01)
        assertEquals(100000.0, denoms[3], 0.01)
    }

    @Test
    fun `calculateSmartDenominations with 12000 returns rounded options larger than total`() {
        val total = 12000.0
        val denoms = CashDenominationHelper.calculateSmartDenominations(total)
        assertEquals(4, denoms.size)

        // opt1: ceil to 10k is 20k
        assertEquals(20000.0, denoms[0], 0.01)
        // opt2: ceil to 50k is 50k
        assertEquals(50000.0, denoms[1], 0.01)
        // opt3: ceil to 100k is 100k
        assertEquals(100000.0, denoms[2], 0.01)
        // opt4: 150k
        assertEquals(150000.0, denoms[3], 0.01)

        // All denominations must be greater than total
        denoms.forEach { denom ->
            assertTrue(denom >= total)
        }
    }

    @Test
    fun `calculateSmartDenominations with 75000 returns proper ascending values`() {
        val total = 75000.0
        val denoms = CashDenominationHelper.calculateSmartDenominations(total)
        assertEquals(4, denoms.size)

        // opt1: ceil to 10k -> 80.000
        assertEquals(80000.0, denoms[0], 0.01)
        // opt2: 100.000
        assertEquals(100000.0, denoms[1], 0.01)
        // All options >= total
        denoms.forEach { denom ->
            assertTrue(denom >= total)
        }
    }

    @Test
    fun `formatDenominationLabel formats thousands correctly`() {
        assertEquals("10rb", CashDenominationHelper.formatDenominationLabel(10000.0))
        assertEquals("50rb", CashDenominationHelper.formatDenominationLabel(50000.0))
        assertEquals("100rb", CashDenominationHelper.formatDenominationLabel(100000.0))
        assertEquals("150rb", CashDenominationHelper.formatDenominationLabel(150000.0))
    }
}
