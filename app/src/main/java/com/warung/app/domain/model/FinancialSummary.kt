package com.warung.app.domain.model

data class FinancialSummary(
    val totalRevenue: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalTransactions: Int = 0,
    val netProfit: Double = 0.0
)
