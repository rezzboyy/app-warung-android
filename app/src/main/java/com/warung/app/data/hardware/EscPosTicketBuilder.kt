package com.warung.app.data.hardware

import com.warung.app.data.local.entity.TransactionItemEntity
import com.warung.app.utils.CurrencyFormatter
import com.warung.app.utils.DateFormatter
import java.io.ByteArrayOutputStream

object EscPosTicketBuilder {

    private val ESC: Byte = 0x1B
    private val GS: Byte = 0x1D

    // Commands
    private val INIT_PRINTER = byteArrayOf(ESC, 0x40)
    private val ALIGN_LEFT = byteArrayOf(ESC, 0x61, 0x00)
    private val ALIGN_CENTER = byteArrayOf(ESC, 0x61, 0x01)
    private val ALIGN_RIGHT = byteArrayOf(ESC, 0x61, 0x02)
    private val BOLD_ON = byteArrayOf(ESC, 0x45, 0x01)
    private val BOLD_OFF = byteArrayOf(ESC, 0x45, 0x00)
    private val DOUBLE_HEIGHT_ON = byteArrayOf(GS, 0x21, 0x10)
    private val DOUBLE_HEIGHT_OFF = byteArrayOf(GS, 0x21, 0x00)
    private val FEED_PAPER_AND_CUT = byteArrayOf(GS, 0x56, 0x41, 0x10)

    private const val PAPER_WIDTH_CHARS = 32 // Standar 58mm printer

    fun buildReceipt(
        storeName: String,
        storeAddress: String,
        storePhone: String,
        invoiceNumber: String,
        dateEpoch: Long,
        items: List<TransactionItemEntity>,
        grandTotal: Double,
        paidAmount: Double,
        changeAmount: Double,
        paymentMethod: String,
        footer: String
    ): ByteArray {
        val out = ByteArrayOutputStream()

        out.write(INIT_PRINTER)

        // Store Header (Center, Bold)
        out.write(ALIGN_CENTER)
        out.write(BOLD_ON)
        out.write(DOUBLE_HEIGHT_ON)
        out.write("$storeName\n".toByteArray(Charsets.ISO_8859_1))
        out.write(DOUBLE_HEIGHT_OFF)
        out.write(BOLD_OFF)

        if (storeAddress.isNotBlank()) {
            out.write("$storeAddress\n".toByteArray(Charsets.ISO_8859_1))
        }
        if (storePhone.isNotBlank()) {
            out.write("Telp: $storePhone\n".toByteArray(Charsets.ISO_8859_1))
        }

        // Line separator
        out.write("${createSeparator('-')}\n".toByteArray(Charsets.ISO_8859_1))

        // Invoice & Date
        out.write(ALIGN_LEFT)
        out.write("No. Inv : $invoiceNumber\n".toByteArray(Charsets.ISO_8859_1))
        out.write("Waktu   : ${DateFormatter.formatDateTime(dateEpoch)}\n".toByteArray(Charsets.ISO_8859_1))
        out.write("Kasir   : Kasir Utama\n".toByteArray(Charsets.ISO_8859_1))

        out.write("${createSeparator('-')}\n".toByteArray(Charsets.ISO_8859_1))

        // Items
        items.forEach { item ->
            val itemName = "${item.productName} x${item.quantity}"
            out.write("$itemName\n".toByteArray(Charsets.ISO_8859_1))

            val priceDetail = "  @ ${CurrencyFormatter.formatRupiah(item.sellPrice)}"
            val subtotal = CurrencyFormatter.formatRupiah(item.subtotal)
            out.write("${formatTwoColumns(priceDetail, subtotal)}\n".toByteArray(Charsets.ISO_8859_1))
        }

        out.write("${createSeparator('-')}\n".toByteArray(Charsets.ISO_8859_1))

        // Financial Summary
        out.write(BOLD_ON)
        out.write("${formatTwoColumns("TOTAL BELANJA", CurrencyFormatter.formatRupiah(grandTotal))}\n".toByteArray(Charsets.ISO_8859_1))
        out.write(BOLD_OFF)

        out.write("${formatTwoColumns("Metode Bayar", paymentMethod)}\n".toByteArray(Charsets.ISO_8859_1))
        out.write("${formatTwoColumns("Uang Diterima", CurrencyFormatter.formatRupiah(paidAmount))}\n".toByteArray(Charsets.ISO_8859_1))
        out.write("${formatTwoColumns("Kembalian", CurrencyFormatter.formatRupiah(changeAmount))}\n".toByteArray(Charsets.ISO_8859_1))

        out.write("${createSeparator('=')}\n".toByteArray(Charsets.ISO_8859_1))

        // Footer
        out.write(ALIGN_CENTER)
        out.write("$footer\n".toByteArray(Charsets.ISO_8859_1))
        out.write("Aplikasi Kasir App Warung\n\n\n\n".toByteArray(Charsets.ISO_8859_1))

        out.write(FEED_PAPER_AND_CUT)

        return out.toByteArray()
    }

    fun buildTestTicket(storeName: String, storeAddress: String): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(INIT_PRINTER)
        out.write(ALIGN_CENTER)
        out.write(BOLD_ON)
        out.write("$storeName\n".toByteArray(Charsets.ISO_8859_1))
        out.write(BOLD_OFF)
        out.write("$storeAddress\n".toByteArray(Charsets.ISO_8859_1))
        out.write("${createSeparator('-')}\n".toByteArray(Charsets.ISO_8859_1))
        out.write("UJI KONEKSI PRINTER BERHASIL!\n".toByteArray(Charsets.ISO_8859_1))
        out.write("Printer thermal siap digunakan.\n".toByteArray(Charsets.ISO_8859_1))
        out.write("${createSeparator('-')}\n".toByteArray(Charsets.ISO_8859_1))
        out.write("\n\n\n".toByteArray(Charsets.ISO_8859_1))
        return out.toByteArray()
    }

    fun formatTwoColumns(left: String, right: String, totalWidth: Int = PAPER_WIDTH_CHARS): String {
        val spaceCount = totalWidth - left.length - right.length
        return if (spaceCount > 0) {
            left + " ".repeat(spaceCount) + right
        } else {
            val truncatedLeft = left.take((totalWidth - right.length - 1).coerceAtLeast(1))
            val remainingSpaces = (totalWidth - truncatedLeft.length - right.length).coerceAtLeast(1)
            truncatedLeft + " ".repeat(remainingSpaces) + right
        }
    }

    fun createSeparator(char: Char = '-', width: Int = PAPER_WIDTH_CHARS): String {
        return char.toString().repeat(width)
    }
}
