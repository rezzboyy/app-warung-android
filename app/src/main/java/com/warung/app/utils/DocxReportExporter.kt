package com.warung.app.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.warung.app.data.local.entity.CashFlowEntity
import com.warung.app.data.local.entity.CustomerEntity
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.data.local.entity.TransactionEntity
import com.warung.app.data.local.entity.TransactionItemEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class DocxReportType {
    FULL,
    TRANSACTIONS,
    EXPENSES,
    DEBTS,
    PRODUCTS
}

data class FullReportData(
    val storeName: String,
    val storeAddress: String,
    val storePhone: String,
    val dateGenerated: String,
    val timeRange: String,
    val totalRevenue: Double,
    val totalExpense: Double,
    val netProfit: Double,
    val totalDebt: Double,
    val transactions: List<TransactionEntity>,
    val transactionItems: List<TransactionItemEntity>,
    val expenses: List<CashFlowEntity>,
    val customers: List<CustomerEntity>,
    val products: List<ProductEntity>,
    val categoryMap: Map<Long, String> = emptyMap()
)

object DocxReportExporter {

    private const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""

    private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    private const val DOCUMENT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Calibri"/>
        <w:sz w:val="22"/>
        <w:color w:val="212121"/>
      </w:rPr>
    </w:rPrDefault>
  </w:docDefaults>
</w:styles>"""

    fun generateFullReportDocx(data: FullReportData): ByteArray {
        return generateDocxReport(data, DocxReportType.FULL)
    }

    fun generateDocxReport(data: FullReportData, reportType: DocxReportType = DocxReportType.FULL): ByteArray {
        val docXml = buildDocumentXml(data, reportType)
        return packageDocx(docXml)
    }

    private fun packageDocx(documentXml: String): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(CONTENT_TYPES_XML.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(ROOT_RELS_XML.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 3. word/_rels/document.xml.rels
            zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zos.write(DOCUMENT_RELS_XML.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 4. word/styles.xml
            zos.putNextEntry(ZipEntry("word/styles.xml"))
            zos.write(STYLES_XML.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 5. word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(documentXml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun buildDocumentXml(data: FullReportData, reportType: DocxReportType): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
        sb.append("<w:body>")

        val titleSuffix = when (reportType) {
            DocxReportType.FULL -> "LAPORAN LENGKAP KEUANGAN, TRANSAKSI & PEMBUKUAN"
            DocxReportType.TRANSACTIONS -> "LAPORAN RIWAYAT TRANSAKSI PENJUALAN KASIR"
            DocxReportType.EXPENSES -> "LAPORAN RIWAYAT PENGELUARAN OPERASIONAL"
            DocxReportType.DEBTS -> "LAPORAN BUKU KASBON & PIUTANG PELANGGAN"
            DocxReportType.PRODUCTS -> "LAPORAN KATALOG PRODUK & MONITORING STOK"
        }

        // --- Document Header ---
        sb.append(createHeadingParagraph(data.storeName.uppercase(), isTitle = true, color = "1565C0"))
        sb.append(createCenteredParagraph(titleSuffix, isBold = true, fontSize = 24, color = "37474F"))
        sb.append(createCenteredParagraph("${data.storeAddress} • Telp/WA: ${data.storePhone}", fontSize = 18, color = "757575"))
        sb.append(createCenteredParagraph("Periode Data: ${data.timeRange} | Dicetak Pada: ${data.dateGenerated}", fontSize = 18, color = "757575"))
        sb.append(createDividerBar())

        when (reportType) {
            DocxReportType.FULL -> {
                // Executive Summary
                sb.append(createSectionHeading("RINGKASAN EKSEKUTIF KEUANGAN"))
                sb.append(buildExecutiveSummaryTable(data))
                sb.append(createSpacer(240))

                // Section 1: Transactions
                sb.append(createSectionHeading("1. RIWAYAT TRANSAKSI PENJUALAN KASIR"))
                sb.append(buildTransactionsTable(data))
                sb.append(createSpacer(240))

                // Section 2: Expenses
                sb.append(createSectionHeading("2. RIWAYAT PENGELUARAN OPERASIONAL WARUNG"))
                sb.append(buildExpensesTable(data.expenses))
                sb.append(createSpacer(240))

                // Section 3: Debts
                sb.append(createSectionHeading("3. BUKU KASBON & PIUTANG PELANGGAN"))
                sb.append(buildDebtsTable(data.customers))
                sb.append(createSpacer(240))

                // Section 4: Products
                sb.append(createSectionHeading("4. KATALOG PRODUK & MONITORING STOK"))
                sb.append(buildProductsTable(data.products, data.categoryMap))
                sb.append(createSpacer(300))
            }
            DocxReportType.TRANSACTIONS -> {
                sb.append(createSectionHeading("RINGKASAN PENJUALAN"))
                sb.append(buildTwoCardSummaryTable(
                    "TOTAL OMZET PENJUALAN", CurrencyFormatter.formatRupiah(data.totalRevenue), "1565C0", "E3F2FD",
                    "JUMLAH TRANSAKSI KASIR", "${data.transactions.size} Transaksi Sukses", "2E7D32", "E8F5E9"
                ))
                sb.append(createSpacer(240))
                sb.append(createSectionHeading("DETAIL TRANSAKSI PENJUALAN KASIR"))
                sb.append(buildTransactionsTable(data))
                sb.append(createSpacer(300))
            }
            DocxReportType.EXPENSES -> {
                sb.append(createSectionHeading("RINGKASAN PENGELUARAN"))
                sb.append(buildTwoCardSummaryTable(
                    "TOTAL PENGELUARAN", CurrencyFormatter.formatRupiah(data.totalExpense), "C62828", "FFEBEE",
                    "JUMLAH CATATAN BIAYA", "${data.expenses.size} Catatan Pengeluaran", "37474F", "F1F5F9"
                ))
                sb.append(createSpacer(240))
                sb.append(createSectionHeading("DETAIL PENGELUARAN OPERASIONAL"))
                sb.append(buildExpensesTable(data.expenses))
                sb.append(createSpacer(300))
            }
            DocxReportType.DEBTS -> {
                sb.append(createSectionHeading("RINGKASAN PIUTANG KASBON"))
                sb.append(buildTwoCardSummaryTable(
                    "TOTAL SISA PIUTANG KASBON", CurrencyFormatter.formatRupiah(data.totalDebt), "E65100", "FFF3E0",
                    "TOTAL PELANGGAN KASBON", "${data.customers.size} Orang Tercatat", "37474F", "F1F5F9"
                ))
                sb.append(createSpacer(240))
                sb.append(createSectionHeading("DETAIL BUKU KASBON & PIUTANG"))
                sb.append(buildDebtsTable(data.customers))
                sb.append(createSpacer(300))
            }
            DocxReportType.PRODUCTS -> {
                val lowStockCount = data.products.count { it.stock <= 5 }
                sb.append(createSectionHeading("RINGKASAN STOK PRODUK"))
                sb.append(buildTwoCardSummaryTable(
                    "TOTAL JENIS PRODUK", "${data.products.size} Jenis Barang", "1565C0", "E3F2FD",
                    "PERINGATAN STOK MENIPIS (<= 5)", "$lowStockCount Produk Perlu Restok", if (lowStockCount > 0) "C62828" else "2E7D32", if (lowStockCount > 0) "FFEBEE" else "E8F5E9"
                ))
                sb.append(createSpacer(240))
                sb.append(createSectionHeading("DETAIL KATALOG PRODUK & MONITORING STOK"))
                sb.append(buildProductsTable(data.products, data.categoryMap))
                sb.append(createSpacer(300))
            }
        }

        // --- Document Sign-off / Signature Area ---
        sb.append(buildSignOffSection(data.storeName))

        // Page setup: A4 with 20mm margins
        sb.append("""<w:sectPr>
            <w:pgSz w:w="11906" w:h="16838"/>
            <w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134" w:header="720" w:footer="720" w:gutter="0"/>
        </w:sectPr>""")

        sb.append("</w:body></w:document>")
        return sb.toString()
    }

    private fun buildExecutiveSummaryTable(data: FullReportData): String {
        val totalTrx = data.transactions.size
        return """
        <w:tbl>
          <w:tblPr>
            <w:tblW w:w="9638" w:type="dxa"/>
            <w:tblBorders>
              <w:top w:val="single" w:sz="6" w:space="0" w:color="1E88E5"/>
              <w:left w:val="single" w:sz="6" w:space="0" w:color="1E88E5"/>
              <w:bottom w:val="single" w:sz="6" w:space="0" w:color="1E88E5"/>
              <w:right w:val="single" w:sz="6" w:space="0" w:color="1E88E5"/>
              <w:insideH w:val="single" w:sz="4" w:space="0" w:color="E0E0E0"/>
              <w:insideV w:val="single" w:sz="4" w:space="0" w:color="E0E0E0"/>
            </w:tblBorders>
            <w:tblCellMar>
              <w:top w:w="140" w:type="dxa"/>
              <w:bottom w:w="140" w:type="dxa"/>
              <w:left w:w="180" w:type="dxa"/>
              <w:right w:w="180" w:type="dxa"/>
            </w:tblCellMar>
          </w:tblPr>
          <w:tr>
            ${createCardCell("TOTAL OMZET PENJUALAN", CurrencyFormatter.formatRupiah(data.totalRevenue), "1565C0", "E3F2FD", 4819)}
            ${createCardCell("TOTAL PENGELUARAN", CurrencyFormatter.formatRupiah(data.totalExpense), "C62828", "FFEBEE", 4819)}
          </w:tr>
          <w:tr>
            ${createCardCell("KEUNTUNGAN BERSIH", CurrencyFormatter.formatRupiah(data.netProfit), "2E7D32", "E8F5E9", 4819)}
            ${createCardCell("TOTAL SISA PIUTANG KASBON", CurrencyFormatter.formatRupiah(data.totalDebt), "E65100", "FFF3E0", 4819)}
          </w:tr>
          <w:tr>
            ${createCardCell("JUMLAH TRANSAKSI KASIR", "$totalTrx Transaksi Sukses", "37474F", "F1F5F9", 4819)}
            ${createCardCell("STATUS OPERASIONAL", "Aktif & Terverifikasi", "0D47A1", "F1F5F9", 4819)}
          </w:tr>
        </w:tbl>
        """
    }

    private fun buildTwoCardSummaryTable(
        title1: String, value1: String, color1: String, bg1: String,
        title2: String, value2: String, color2: String, bg2: String
    ): String {
        return """
        <w:tbl>
          <w:tblPr>
            <w:tblW w:w="9638" w:type="dxa"/>
            <w:tblBorders>
              <w:top w:val="single" w:sz="6" w:space="0" w:color="1E88E5"/>
              <w:left w:val="single" w:sz="6" w:space="0" w:color="1E88E5"/>
              <w:bottom w:val="single" w:sz="6" w:space="0" w:color="1E88E5"/>
              <w:right w:val="single" w:sz="6" w:space="0" w:color="1E88E5"/>
              <w:insideH w:val="single" w:sz="4" w:space="0" w:color="E0E0E0"/>
              <w:insideV w:val="single" w:sz="4" w:space="0" w:color="E0E0E0"/>
            </w:tblBorders>
            <w:tblCellMar>
              <w:top w:w="140" w:type="dxa"/>
              <w:bottom w:w="140" w:type="dxa"/>
              <w:left w:w="180" w:type="dxa"/>
              <w:right w:w="180" w:type="dxa"/>
            </w:tblCellMar>
          </w:tblPr>
          <w:tr>
            ${createCardCell(title1, value1, color1, bg1, 4819)}
            ${createCardCell(title2, value2, color2, bg2, 4819)}
          </w:tr>
        </w:tbl>
        """
    }

    private fun createCardCell(title: String, value: String, valueColor: String, bgColor: String, widthDxa: Int): String {
        return """
        <w:tc>
          <w:tcPr>
            <w:tcW w:w="$widthDxa" w:type="dxa"/>
            <w:shd w:val="clear" w:color="auto" w:fill="$bgColor"/>
          </w:tcPr>
          <w:p>
            <w:pPr><w:spacing w:before="60" w:after="40"/></w:pPr>
            <w:r>
              <w:rPr><w:b/><w:sz w:val="18"/><w:color w:val="616161"/></w:rPr>
              <w:t>${escapeXml(title)}</w:t>
            </w:r>
          </w:p>
          <w:p>
            <w:pPr><w:spacing w:before="0" w:after="60"/></w:pPr>
            <w:r>
              <w:rPr><w:b/><w:sz w:val="28"/><w:color w:val="$valueColor"/></w:rPr>
              <w:t>${escapeXml(value)}</w:t>
            </w:r>
          </w:p>
        </w:tc>
        """
    }

    private fun buildTransactionsTable(data: FullReportData): String {
        val itemsByTxId = data.transactionItems.groupBy { it.transactionId }

        val sb = StringBuilder()
        sb.append(startTable())
        // Header
        sb.append(createTableRow(
            listOf("No", "Invoice", "Tanggal/Waktu", "Rincian Barang Belanja", "Metode", "Total"),
            listOf(500, 2000, 1600, 3138, 1100, 1300),
            isHeader = true
        ))

        if (data.transactions.isEmpty()) {
            sb.append(createEmptyTableRow(6, "Tidak ada data transaksi kasir pada periode ini."))
        } else {
            var grandSum = 0.0
            data.transactions.forEachIndexed { idx, tx ->
                val items = itemsByTxId[tx.id] ?: emptyList()
                val itemsSummary = if (items.isNotEmpty()) {
                    items.joinToString(", ") { "${it.productName} x${it.quantity}" }
                } else {
                    "-"
                }
                grandSum += tx.grandTotal
                val isZebra = (idx % 2 == 1)
                sb.append(createTableRow(
                    listOf(
                        (idx + 1).toString(),
                        tx.invoiceNumber,
                        DateFormatter.formatDateTime(tx.createdAt),
                        itemsSummary,
                        tx.paymentMethod,
                        CurrencyFormatter.formatRupiah(tx.grandTotal)
                    ),
                    listOf(500, 2000, 1600, 3138, 1100, 1300),
                    isZebra = isZebra
                ))
            }
            // Summary row
            sb.append(createSummaryRow("TOTAL OMZET TRANSAKSI", CurrencyFormatter.formatRupiah(grandSum), 500 + 2000 + 1600 + 3138 + 1100, 1300))
        }

        sb.append(endTable())
        return sb.toString()
    }

    private fun buildExpensesTable(expenses: List<CashFlowEntity>): String {
        val sb = StringBuilder()
        sb.append(startTable())
        sb.append(createTableRow(
            listOf("No", "Tanggal/Waktu", "Kategori", "Keterangan", "Nominal"),
            listOf(500, 2000, 2200, 3238, 1700),
            isHeader = true
        ))

        if (expenses.isEmpty()) {
            sb.append(createEmptyTableRow(5, "Tidak ada catatan pengeluaran operasional."))
        } else {
            var totalExpense = 0.0
            expenses.forEachIndexed { idx, exp ->
                totalExpense += exp.amount
                sb.append(createTableRow(
                    listOf(
                        (idx + 1).toString(),
                        DateFormatter.formatDateTime(exp.date),
                        exp.category,
                        exp.description,
                        CurrencyFormatter.formatRupiah(exp.amount)
                    ),
                    listOf(500, 2000, 2200, 3238, 1700),
                    isZebra = (idx % 2 == 1)
                ))
            }
            sb.append(createSummaryRow("TOTAL PENGELUARAN", CurrencyFormatter.formatRupiah(totalExpense), 500 + 2000 + 2200 + 3238, 1700))
        }

        sb.append(endTable())
        return sb.toString()
    }

    private fun buildDebtsTable(customers: List<CustomerEntity>): String {
        val sb = StringBuilder()
        sb.append(startTable())
        sb.append(createTableRow(
            listOf("No", "Nama Pelanggan", "No. Telepon / WA", "Alamat", "Status", "Sisa Kasbon"),
            listOf(500, 2200, 1800, 2338, 1200, 1600),
            isHeader = true
        ))

        if (customers.isEmpty()) {
            sb.append(createEmptyTableRow(6, "Tidak ada catatan buku kasbon pelanggan."))
        } else {
            var totalOutstanding = 0.0
            customers.forEachIndexed { idx, c ->
                totalOutstanding += c.totalDebt
                val status = if (c.totalDebt > 0) "Belum Lunas" else "Lunas"
                sb.append(createTableRow(
                    listOf(
                        (idx + 1).toString(),
                        c.name,
                        c.phoneNumber ?: "-",
                        c.address ?: "-",
                        status,
                        CurrencyFormatter.formatRupiah(c.totalDebt)
                    ),
                    listOf(500, 2200, 1800, 2338, 1200, 1600),
                    isZebra = (idx % 2 == 1)
                ))
            }
            sb.append(createSummaryRow("TOTAL PIUTANG WARUNG", CurrencyFormatter.formatRupiah(totalOutstanding), 500 + 2200 + 1800 + 2338 + 1200, 1600))
        }

        sb.append(endTable())
        return sb.toString()
    }

    private fun buildProductsTable(products: List<ProductEntity>, categoryMap: Map<Long, String>): String {
        val sb = StringBuilder()
        sb.append(startTable())
        sb.append(createTableRow(
            listOf("No", "Nama Produk", "Kategori", "Barcode", "Harga Modal", "Harga Jual", "Stok", "Status"),
            listOf(500, 2300, 1500, 1300, 1100, 1100, 800, 1038),
            isHeader = true
        ))

        if (products.isEmpty()) {
            sb.append(createEmptyTableRow(8, "Katalog produk masih kosong."))
        } else {
            products.forEachIndexed { idx, p ->
                val catName = p.categoryId?.let { categoryMap[it] } ?: "Umum"
                val stockStatus = if (p.stock <= 5) "Menipis" else "Aman"
                sb.append(createTableRow(
                    listOf(
                        (idx + 1).toString(),
                        p.name,
                        catName,
                        p.barcode ?: "-",
                        CurrencyFormatter.formatRupiah(p.buyPrice),
                        CurrencyFormatter.formatRupiah(p.sellPrice),
                        "${p.stock} ${p.unit}",
                        stockStatus
                    ),
                    listOf(500, 2300, 1500, 1300, 1100, 1100, 800, 1038),
                    isZebra = (idx % 2 == 1)
                ))
            }
        }

        sb.append(endTable())
        return sb.toString()
    }

    private fun buildSignOffSection(storeName: String): String {
        return """
        <w:p>
          <w:pPr><w:jc w:val="right"/><w:spacing w:before="360" w:after="80"/></w:pPr>
          <w:r>
            <w:rPr><w:sz w:val="20"/><w:color w:val="616161"/></w:rPr>
            <w:t>Penanggung Jawab / Pemilik Toko,</w:t>
          </w:r>
        </w:p>
        <w:p>
          <w:pPr><w:jc w:val="right"/><w:spacing w:before="400" w:after="80"/></w:pPr>
          <w:r>
            <w:rPr><w:b/><w:u w:val="single"/><w:sz w:val="22"/><w:color w:val="212121"/></w:rPr>
            <w:t>( ${escapeXml(storeName)} )</w:t>
          </w:r>
        </w:p>
        <w:p>
          <w:pPr><w:jc w:val="center"/><w:spacing w:before="300" w:after="60"/></w:pPr>
          <w:r>
            <w:rPr><w:i/><w:sz w:val="16"/><w:color w:val="9E9E9E"/></w:rPr>
            <w:t>Laporan ini dibuat otomatis oleh Aplikasi Kasir &amp; Pembukuan Warung (App Warung)</w:t>
          </w:r>
        </w:p>
        """
    }

    private fun startTable(): String {
        return """
        <w:tbl>
          <w:tblPr>
            <w:tblW w:w="9638" w:type="dxa"/>
            <w:tblBorders>
              <w:top w:val="single" w:sz="4" w:space="0" w:color="CFD8DC"/>
              <w:left w:val="single" w:sz="4" w:space="0" w:color="CFD8DC"/>
              <w:bottom w:val="single" w:sz="4" w:space="0" w:color="CFD8DC"/>
              <w:right w:val="single" w:sz="4" w:space="0" w:color="CFD8DC"/>
              <w:insideH w:val="single" w:sz="4" w:space="0" w:color="ECEFF1"/>
              <w:insideV w:val="single" w:sz="4" w:space="0" w:color="ECEFF1"/>
            </w:tblBorders>
            <w:tblCellMar>
              <w:top w:w="120" w:type="dxa"/>
              <w:bottom w:w="120" w:type="dxa"/>
              <w:left w:w="140" w:type="dxa"/>
              <w:right w:w="140" w:type="dxa"/>
            </w:tblCellMar>
          </w:tblPr>
        """
    }

    private fun endTable(): String = "</w:tbl>"

    private fun createTableRow(
        values: List<String>,
        widths: List<Int>,
        isHeader: Boolean = false,
        isZebra: Boolean = false
    ): String {
        val sb = StringBuilder("<w:tr>")
        val bgColor = if (isHeader) "1E88E5" else if (isZebra) "F8FAFC" else "FFFFFF"
        val textColor = if (isHeader) "FFFFFF" else "212121"
        val fontSize = if (isHeader) "18" else "17"

        values.forEachIndexed { i, text ->
            val w = widths.getOrElse(i) { 1000 }
            val align = if (isHeader || i == 0) "center" else if (text.startsWith("Rp ") || text.endsWith("Transaksi")) "right" else "left"

            sb.append("""
            <w:tc>
              <w:tcPr>
                <w:tcW w:w="$w" w:type="dxa"/>
                <w:shd w:val="clear" w:color="auto" w:fill="$bgColor"/>
              </w:tcPr>
              <w:p>
                <w:pPr>
                  <w:jc w:val="$align"/>
                  <w:spacing w:before="60" w:after="60"/>
                </w:pPr>
                <w:r>
                  <w:rPr>
                    ${if (isHeader) "<w:b/>" else ""}
                    <w:color w:val="$textColor"/>
                    <w:sz w:val="$fontSize"/>
                  </w:rPr>
                  <w:t xml:space="preserve">${escapeXml(text)}</w:t>
                </w:r>
              </w:p>
            </w:tc>
            """)
        }
        sb.append("</w:tr>")
        return sb.toString()
    }

    private fun createEmptyTableRow(colSpan: Int, message: String): String {
        return """
        <w:tr>
          <w:tc>
            <w:tcPr>
              <w:tcW w:w="9638" w:type="dxa"/>
              <w:gridSpan w:val="$colSpan"/>
              <w:shd w:val="clear" w:color="auto" w:fill="FAFAFA"/>
            </w:tcPr>
            <w:p>
              <w:pPr><w:jc w:val="center"/><w:spacing w:before="120" w:after="120"/></w:pPr>
              <w:r>
                <w:rPr><w:i/><w:color w:val="757575"/><w:sz w:val="18"/></w:rPr>
                <w:t>${escapeXml(message)}</w:t>
              </w:r>
            </w:p>
          </w:tc>
        </w:tr>
        """
    }

    private fun createSummaryRow(label: String, totalValue: String, labelWidth: Int, valueWidth: Int): String {
        return """
        <w:tr>
          <w:tc>
            <w:tcPr>
              <w:tcW w:w="$labelWidth" w:type="dxa"/>
              <w:shd w:val="clear" w:color="auto" w:fill="E3F2FD"/>
            </w:tcPr>
            <w:p>
              <w:pPr><w:jc w:val="right"/><w:spacing w:before="80" w:after="80"/></w:pPr>
              <w:r>
                <w:rPr><w:b/><w:color w:val="0D47A1"/><w:sz w:val="18"/></w:rPr>
                <w:t>${escapeXml(label)} :</w:t>
              </w:r>
            </w:p>
          </w:tc>
          <w:tc>
            <w:tcPr>
              <w:tcW w:w="$valueWidth" w:type="dxa"/>
              <w:shd w:val="clear" w:color="auto" w:fill="E3F2FD"/>
            </w:tcPr>
            <w:p>
              <w:pPr><w:jc w:val="right"/><w:spacing w:before="80" w:after="80"/></w:pPr>
              <w:r>
                <w:rPr><w:b/><w:color w:val="0D47A1"/><w:sz w:val="18"/></w:rPr>
                <w:t>${escapeXml(totalValue)}</w:t>
              </w:r>
            </w:p>
          </w:tc>
        </w:tr>
        """
    }

    private fun createHeadingParagraph(text: String, isTitle: Boolean = false, color: String = "1565C0"): String {
        val size = if (isTitle) "36" else "26"
        return """
        <w:p>
          <w:pPr>
            <w:jc w:val="center"/>
            <w:spacing w:before="${if (isTitle) 100 else 240}" w:after="60"/>
          </w:pPr>
          <w:r>
            <w:rPr>
              <w:b/>
              <w:color w:val="$color"/>
              <w:sz w:val="$size"/>
            </w:rPr>
            <w:t>${escapeXml(text)}</w:t>
          </w:r>
        </w:p>
        """
    }

    private fun createSectionHeading(title: String): String {
        return """
        <w:p>
          <w:pPr>
            <w:spacing w:before="240" w:after="80"/>
          </w:pPr>
          <w:r>
            <w:rPr>
              <w:b/>
              <w:color w:val="1565C0"/>
              <w:sz w:val="22"/>
            </w:rPr>
            <w:t>${escapeXml(title)}</w:t>
          </w:r>
        </w:p>
        """
    }

    private fun createCenteredParagraph(text: String, isBold: Boolean = false, fontSize: Int = 20, color: String = "333333"): String {
        return """
        <w:p>
          <w:pPr>
            <w:jc w:val="center"/>
            <w:spacing w:before="30" w:after="40"/>
          </w:pPr>
          <w:r>
            <w:rPr>
              ${if (isBold) "<w:b/>" else ""}
              <w:color w:val="$color"/>
              <w:sz w:val="$fontSize"/>
            </w:rPr>
            <w:t>${escapeXml(text)}</w:t>
          </w:r>
        </w:p>
        """
    }

    private fun createDividerBar(): String {
        return """
        <w:p>
          <w:pPr>
            <w:spacing w:before="60" w:after="140"/>
            <w:pBdr>
              <w:bottom w:val="single" w:sz="18" w:space="1" w:color="1E88E5"/>
            </w:pBdr>
          </w:pPr>
        </w:p>
        """
    }

    private fun createSpacer(spacingBefore: Int): String {
        return """
        <w:p>
          <w:pPr><w:spacing w:before="$spacingBefore"/></w:pPr>
        </w:p>
        """
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun shareDocxFile(context: Context, fileName: String, docxBytes: ByteArray) {
        val cleanName = if (fileName.endsWith(".docx")) fileName else "$fileName.docx"
        val file = File(context.cacheDir, cleanName)
        FileOutputStream(file).use { fos ->
            fos.write(docxBytes)
            fos.flush()
        }

        val uri = FileProvider.getUriForFile(context, "com.warung.app.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, cleanName.removeSuffix(".docx"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Buka atau Bagikan Laporan (.docx)"))
    }
}
