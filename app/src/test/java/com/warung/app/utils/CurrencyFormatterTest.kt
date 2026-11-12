package com.warung.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun testFormatRupiah() {
        val formatted = CurrencyFormatter.formatRupiah(15000.0)
        // Check that it starts with Rp and contains 15.000
        assertTrue("Formatted rupiah should start with Rp", formatted.contains("Rp"))
        assertTrue("Formatted rupiah should contain 15.000", formatted.contains("15.000"))
    }

    @Test
    fun testParseRupiah() {
        assertEquals(15000.0, CurrencyFormatter.parseRupiah("Rp 15.000"), 0.001)
        assertEquals(15000.0, CurrencyFormatter.parseRupiah("Rp15000"), 0.001)
        assertEquals(250000.0, CurrencyFormatter.parseRupiah("250.000"), 0.001)
        assertEquals(0.0, CurrencyFormatter.parseRupiah(""), 0.001)
        assertEquals(0.0, CurrencyFormatter.parseRupiah("abc"), 0.001)
    }
}
