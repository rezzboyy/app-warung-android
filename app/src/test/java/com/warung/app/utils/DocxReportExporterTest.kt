package com.warung.app.utils

import com.warung.app.data.local.entity.CashFlowEntity
import com.warung.app.data.local.entity.CustomerEntity
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.data.local.entity.TransactionEntity
import com.warung.app.data.local.entity.TransactionItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class DocxReportExporterTest {

    private fun createSampleData(): FullReportData {
        return FullReportData(
            storeName = "Warung Berkah & Bahagia",
            storeAddress = "Jl. Merdeka No. 45 <RT 01/02>",
            storePhone = "081234567890",
            dateGenerated = "20 Oktober 2026, 12:00",
            timeRange = "Bulan Ini",
            totalRevenue = 150000.0,
            totalExpense = 35000.0,
            netProfit = 115000.0,
            totalDebt = 25000.0,
            transactions = listOf(
                TransactionEntity(
                    id = 1,
                    invoiceNumber = "TRX-20261020-001",
                    createdAt = 1729425600000L,
                    subtotal = 50000.0,
                    grandTotal = 50000.0,
                    paymentMethod = "CASH",
                    paidAmount = 50000.0,
                    changeAmount = 0.0,
                    status = "COMPLETED"
                )
            ),
            transactionItems = listOf(
                TransactionItemEntity(
                    id = 1,
                    transactionId = 1,
                    productId = 10,
                    productName = "Beras Rojolele 5kg",
                    buyPrice = 45000.0,
                    sellPrice = 50000.0,
                    quantity = 1,
                    subtotal = 50000.0
                )
            ),
            expenses = listOf(
                CashFlowEntity(
                    id = 1,
                    date = 1729425600000L,
                    type = "EXPENSE",
                    category = "Operasional",
                    amount = 35000.0,
                    description = "Beli Gas LPG 3kg"
                )
            ),
            customers = listOf(
                CustomerEntity(
                    id = 1,
                    name = "Pak Ahmad",
                    phoneNumber = "081987654321",
                    address = "Gang Melati",
                    totalDebt = 25000.0
                )
            ),
            products = listOf(
                ProductEntity(
                    id = 10,
                    name = "Beras Rojolele 5kg",
                    buyPrice = 45000.0,
                    sellPrice = 50000.0,
                    stock = 3,
                    unit = "Sak",
                    barcode = "8991234567890"
                )
            ),
            categoryMap = mapOf(1L to "Sembako")
        )
    }

    private fun extractZipEntries(docxBytes: ByteArray): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(docxBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val content = zis.readBytes().toString(Charsets.UTF_8)
                entries[entry.name] = content
                entry = zis.nextEntry
            }
        }
        return entries
    }

    @Test
    fun testGenerateFullReportDocxProducesValidDocxArchive() {
        val data = createSampleData()
        val docxBytes = DocxReportExporter.generateFullReportDocx(data)

        assertTrue(docxBytes.isNotEmpty())

        val entries = extractZipEntries(docxBytes)

        // Must contain required OpenXML parts
        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("_rels/.rels"))
        assertTrue(entries.containsKey("word/_rels/document.xml.rels"))
        assertTrue(entries.containsKey("word/styles.xml"))
        assertTrue(entries.containsKey("word/document.xml"))

        val docXml = entries["word/document.xml"]
        assertNotNull(docXml)

        // Store info & escaped special characters
        assertTrue(docXml!!.contains("WARUNG BERKAH &amp; BAHAGIA"))
        assertTrue(docXml.contains("&lt;RT 01/02&gt;"))
        assertTrue(docXml.contains("081234567890"))
        assertTrue(docXml.contains("Bulan Ini"))

        // Executive summary
        assertTrue(docXml.contains("RINGKASAN EKSEKUTIF KEUANGAN"))
        assertTrue(docXml.contains("TOTAL OMZET PENJUALAN"))

        // Section 1: Transactions and item details
        assertTrue(docXml.contains("1. RIWAYAT TRANSAKSI PENJUALAN KASIR"))
        assertTrue(docXml.contains("TRX-20261020-001"))
        assertTrue(docXml.contains("Beras Rojolele 5kg x1"))

        // Section 2: Expenses
        assertTrue(docXml.contains("2. RIWAYAT PENGELUARAN OPERASIONAL WARUNG"))
        assertTrue(docXml.contains("Beli Gas LPG 3kg"))

        // Section 3: Debts
        assertTrue(docXml.contains("3. BUKU KASBON &amp; PIUTANG PELANGGAN"))
        assertTrue(docXml.contains("Pak Ahmad"))

        // Section 4: Products & stock
        assertTrue(docXml.contains("4. KATALOG PRODUK &amp; MONITORING STOK"))
        assertTrue(docXml.contains("8991234567890"))
        assertTrue(docXml.contains("Menipis")) // stock = 3 <= 5

        // Signature section
        assertTrue(docXml.contains("Penanggung Jawab / Pemilik Toko"))
        assertTrue(docXml.contains("Warung Berkah &amp; Bahagia"))
    }

    @Test
    fun testGenerateTransactionsOnlyDocx() {
        val data = createSampleData()
        val docxBytes = DocxReportExporter.generateDocxReport(data, DocxReportType.TRANSACTIONS)
        val entries = extractZipEntries(docxBytes)
        val docXml = entries["word/document.xml"]!!

        assertTrue(docXml.contains("LAPORAN RIWAYAT TRANSAKSI PENJUALAN KASIR"))
        assertTrue(docXml.contains("TRX-20261020-001"))
        assertTrue(docXml.contains("Beras Rojolele 5kg x1"))
        // Debts section should not be in transactions-only report
        assertTrue(!docXml.contains("BUKU KASBON &amp; PIUTANG PELANGGAN"))
    }

    @Test
    fun testGenerateExpensesOnlyDocx() {
        val data = createSampleData()
        val docxBytes = DocxReportExporter.generateDocxReport(data, DocxReportType.EXPENSES)
        val entries = extractZipEntries(docxBytes)
        val docXml = entries["word/document.xml"]!!

        assertTrue(docXml.contains("LAPORAN RIWAYAT PENGELUARAN OPERASIONAL"))
        assertTrue(docXml.contains("Beli Gas LPG 3kg"))
        // Products table should not be in expenses-only report
        assertTrue(!docXml.contains("KATALOG PRODUK &amp; MONITORING STOK"))
    }

    @Test
    fun testGenerateDebtsOnlyDocx() {
        val data = createSampleData()
        val docxBytes = DocxReportExporter.generateDocxReport(data, DocxReportType.DEBTS)
        val entries = extractZipEntries(docxBytes)
        val docXml = entries["word/document.xml"]!!

        assertTrue(docXml.contains("LAPORAN BUKU KASBON &amp; PIUTANG PELANGGAN"))
        assertTrue(docXml.contains("Pak Ahmad"))
        assertTrue(docXml.contains("Gang Melati"))
    }

    @Test
    fun testGenerateProductsOnlyDocx() {
        val data = createSampleData()
        val docxBytes = DocxReportExporter.generateDocxReport(data, DocxReportType.PRODUCTS)
        val entries = extractZipEntries(docxBytes)
        val docXml = entries["word/document.xml"]!!

        assertTrue(docXml.contains("LAPORAN KATALOG PRODUK &amp; MONITORING STOK"))
        assertTrue(docXml.contains("Beras Rojolele 5kg"))
        assertTrue(docXml.contains("Menipis"))
    }
}
