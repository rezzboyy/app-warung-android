package com.warung.app.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val indonesianLocale = Locale("id", "ID")
    private val rupiahFormat: NumberFormat = NumberFormat.getCurrencyInstance(indonesianLocale).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    fun formatRupiah(amount: Double): String {
        return rupiahFormat.format(amount).replace("Rp", "Rp ")
    }

    fun formatRupiah(amount: Long): String {
        return formatRupiah(amount.toDouble())
    }

    fun parseRupiah(text: String): Double {
        val cleanString = text.replace("[^0-9]".toRegex(), "")
        return cleanString.toDoubleOrNull() ?: 0.0
    }
}
