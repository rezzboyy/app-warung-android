package com.warung.app.utils

import kotlin.math.ceil

object CashDenominationHelper {

    fun calculateSmartDenominations(total: Double): List<Double> {
        val standard = listOf(10000.0, 20000.0, 50000.0, 100000.0)
        if (total <= 0.0) return standard

        val result = mutableListOf<Double>()
        val ceil10k = ceil(total / 10000.0) * 10000.0
        val opt1 = if (ceil10k == total) total + 10000.0 else ceil10k
        result.add(opt1)

        val ceil50k = ceil(total / 50000.0) * 50000.0
        val opt2 = if (ceil50k <= opt1) opt1 + 10000.0 else ceil50k
        result.add(opt2)

        val ceil100k = ceil(total / 100000.0) * 100000.0
        val opt3 = if (ceil100k <= opt2) opt2 + 50000.0 else ceil100k
        result.add(opt3)

        val opt4 = opt3 + 50000.0
        result.add(opt4)

        return result
    }

    fun formatDenominationLabel(amount: Double): String {
        val thousands = (amount / 1000).toLong()
        return "${thousands}rb"
    }
}
