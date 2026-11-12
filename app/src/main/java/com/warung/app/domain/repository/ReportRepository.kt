package com.warung.app.domain.repository

import com.warung.app.data.local.entity.CashFlowEntity
import com.warung.app.data.local.entity.TransactionEntity
import com.warung.app.domain.model.FinancialSummary
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun getFinancialSummary(startDate: Long, endDate: Long): Flow<FinancialSummary>
    fun getExpensesBetweenDates(startDate: Long, endDate: Long): Flow<List<CashFlowEntity>>
    fun getAllExpenses(): Flow<List<CashFlowEntity>>
    suspend fun addExpense(
        amount: Double,
        category: String,
        description: String,
        date: Long = System.currentTimeMillis()
    ): Long
    fun getRecentTransactions(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
}
