package com.warung.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DateFormatterTest {

    @Test
    fun testFormatDateNotException() {
        val now = System.currentTimeMillis()
        val formattedDate = DateFormatter.formatDate(now)
        assertNotNull(formattedDate)

        val formattedDateTime = DateFormatter.formatDateTime(now)
        assertNotNull(formattedDateTime)

        val formattedTime = DateFormatter.formatTime(now)
        assertNotNull(formattedTime)

        val invoiceDate = DateFormatter.formatInvoiceDate(now)
        assertEquals(8, invoiceDate.length) // yyyyMMdd -> 8 characters
    }
}
