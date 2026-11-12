package com.warung.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val indonesianLocale = Locale("id", "ID")

    fun formatDateTime(epochMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", indonesianLocale)
        return sdf.format(Date(epochMillis))
    }

    fun formatDate(epochMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", indonesianLocale)
        return sdf.format(Date(epochMillis))
    }

    fun formatTime(epochMillis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", indonesianLocale)
        return sdf.format(Date(epochMillis))
    }

    fun formatInvoiceDate(epochMillis: Long): String {
        val sdf = SimpleDateFormat("yyyyMMdd", indonesianLocale)
        return sdf.format(Date(epochMillis))
    }
}
