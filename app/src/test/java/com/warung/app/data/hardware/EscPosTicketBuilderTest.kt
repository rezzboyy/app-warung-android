package com.warung.app.data.hardware

import com.warung.app.data.local.entity.TransactionItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EscPosTicketBuilderTest {

    @Test
    fun testFormatTwoColumns() {
        val left = "Indomie"
        val right = "Rp 3.500"
        val formatted = EscPosTicketBuilder.formatTwoColumns(left, right, 32)

        assertEquals(32, formatted.length)
        assertTrue(formatted.startsWith("Indomie"))
        assertTrue(formatted.endsWith("Rp 3.500"))
    }

    @Test
    fun testCreateSeparator() {
        val separator = EscPosTicketBuilder.createSeparator('-', 32)
        assertEquals(32, separator.length)
        assertTrue(separator.all { it == '-' })
    }

    @Test
    fun testBuildReceipt() {
        val items = listOf(
            TransactionItemEntity(
                id = 1,
                transactionId = 1,
                productId = 1,
                productName = "Kopi Hitam",
                buyPrice = 2000.0,
                sellPrice = 3000.0,
                quantity = 2,
                subtotal = 6000.0
            )
        )

        val bytes = EscPosTicketBuilder.buildReceipt(
            storeName = "Warung Berkah",
            storeAddress = "Jl. Sudirman 10",
            storePhone = "0812345678",
            invoiceNumber = "TRX-20261020-001",
            dateEpoch = System.currentTimeMillis(),
            items = items,
            grandTotal = 6000.0,
            paidAmount = 10000.0,
            changeAmount = 4000.0,
            paymentMethod = "CASH",
            footer = "Terima kasih!"
        )

        assertTrue(bytes.isNotEmpty())
        // Should start with ESC @ (0x1B, 0x40)
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x40.toByte(), bytes[1])
    }

    @Test
    fun testBuildTestTicket() {
        val bytes = EscPosTicketBuilder.buildTestTicket("Warung Berkah", "Jl. Sudirman")
        assertTrue(bytes.isNotEmpty())
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x40.toByte(), bytes[1])
    }
}
