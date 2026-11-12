package com.warung.app.utils

import com.warung.app.data.local.entity.CashFlowEntity
import com.warung.app.data.local.entity.CustomerEntity
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.data.local.entity.TransactionEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun testExportTransactionsToCsv() {
        val tx = listOf(
            TransactionEntity(
                id = 1,
                invoiceNumber = "TRX-20261020-001",
                createdAt = System.currentTimeMillis(),
                subtotal = 50000.0,
                grandTotal = 50000.0,
                paymentMethod = "CASH",
                paidAmount = 50000.0,
                changeAmount = 0.0,
                status = "COMPLETED"
            )
        )

        val csv = CsvExporter.exportTransactionsToCsv(tx)
        assertTrue(csv.contains("Nomor Invoice"))
        assertTrue(csv.contains("TRX-20261020-001"))
        assertTrue(csv.contains("50000"))
    }

    @Test
    fun testExportExpensesToCsv() {
        val expenses = listOf(
            CashFlowEntity(
                id = 1,
                date = System.currentTimeMillis(),
                type = "EXPENSE",
                category = "Operasional",
                amount = 20000.0,
                description = "Beli es batu"
            )
        )

        val csv = CsvExporter.exportExpensesToCsv(expenses)
        assertTrue(csv.contains("Kategori"))
        assertTrue(csv.contains("Beli es batu"))
        assertTrue(csv.contains("20000"))
    }

    @Test
    fun testExportDebtsToCsv() {
        val customers = listOf(
            CustomerEntity(
                id = 1,
                name = "Pak Budi",
                phoneNumber = "0812345678",
                address = "Blok A",
                totalDebt = 45000.0
            )
        )

        val csv = CsvExporter.exportDebtsToCsv(customers)
        assertTrue(csv.contains("Nama Pelanggan"))
        assertTrue(csv.contains("Pak Budi"))
        assertTrue(csv.contains("45000"))
        assertTrue(csv.contains("Belum Lunas"))
    }

    @Test
    fun testExportProductsToCsv() {
        val products = listOf(
            ProductEntity(
                id = 1,
                name = "Beras 5kg",
                barcode = "899123456",
                buyPrice = 60000.0,
                sellPrice = 65000.0,
                stock = 10,
                unit = "Karung"
            )
        )

        val csv = CsvExporter.exportProductsToCsv(products)
        assertTrue(csv.contains("Nama Produk"))
        assertTrue(csv.contains("Beras 5kg"))
        assertTrue(csv.contains("899123456"))
    }
}
