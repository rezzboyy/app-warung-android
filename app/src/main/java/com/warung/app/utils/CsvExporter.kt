package com.warung.app.utils

import android.content.Context
import android.content.Intent
import com.warung.app.data.local.entity.CashFlowEntity
import com.warung.app.data.local.entity.CustomerEntity
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.data.local.entity.TransactionEntity

object CsvExporter {

    fun exportTransactionsToCsv(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append("No,Nomor Invoice,Tanggal,Waktu,Total Belanja,Metode Bayar,Status\n")
        transactions.forEachIndexed { index, tx ->
            val dateStr = DateFormatter.formatDate(tx.createdAt)
            val timeStr = DateFormatter.formatTime(tx.createdAt)
            val totalStr = tx.grandTotal.toLong().toString()
            sb.append("${index + 1},\"${tx.invoiceNumber}\",\"$dateStr\",\"$timeStr\",$totalStr,\"${tx.paymentMethod}\",\"${tx.status}\"\n")
        }
        return sb.toString()
    }

    fun exportExpensesToCsv(expenses: List<CashFlowEntity>): String {
        val sb = StringBuilder()
        sb.append("No,Tanggal,Waktu,Kategori,Keterangan,Nominal\n")
        expenses.forEachIndexed { index, expense ->
            val dateStr = DateFormatter.formatDate(expense.date)
            val timeStr = DateFormatter.formatTime(expense.date)
            val amountStr = expense.amount.toLong().toString()
            val cleanDesc = expense.description.replace("\"", "\"\"")
            sb.append("${index + 1},\"$dateStr\",\"$timeStr\",=\"${expense.category}\",\"$cleanDesc\",$amountStr\n")
        }
        return sb.toString()
    }

    fun exportDebtsToCsv(customers: List<CustomerEntity>): String {
        val sb = StringBuilder()
        sb.append("No,Nama Pelanggan,Nomor HP,Alamat,Total Sisa Kasbon,Status\n")
        customers.forEachIndexed { index, c ->
            val phone = c.phoneNumber ?: "-"
            val address = (c.address ?: "-").replace("\"", "\"\"")
            val status = if (c.totalDebt > 0) "Belum Lunas" else "Lunas"
            sb.append("${index + 1},\"${c.name}\",\"$phone\",\"$address\",${c.totalDebt.toLong()},\"$status\"\n")
        }
        return sb.toString()
    }

    fun exportProductsToCsv(products: List<ProductEntity>): String {
        val sb = StringBuilder()
        sb.append("No,Nama Produk,Barcode,Harga Modal,Harga Jual,Stok,Satuan\n")
        products.forEachIndexed { index, p ->
            val barcode = p.barcode ?: "-"
            sb.append("${index + 1},\"${p.name}\",\"$barcode\",${p.buyPrice.toLong()},${p.sellPrice.toLong()},${p.stock},\"${p.unit}\"\n")
        }
        return sb.toString()
    }

    fun shareCsvContent(context: Context, title: String, csvContent: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, csvContent)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan / Simpan Laporan CSV"))
    }
}
