package com.warung.app.data.repository

import com.warung.app.data.local.dao.CashFlowDao
import com.warung.app.data.local.dao.TransactionDao
import com.warung.app.data.local.entity.CashFlowEntity
import com.warung.app.data.local.entity.TransactionEntity
import com.warung.app.domain.model.FinancialSummary
import com.warung.app.domain.repository.ReportRepository
import com.warung.app.utils.Constants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ReportRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val cashFlowDao: CashFlowDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ReportRepository {

    override fun getFinancialSummary(startDate: Long, endDate: Long): Flow<FinancialSummary> =
        combine(
            transactionDao.getTotalRevenueBetweenDates(startDate, endDate),
            cashFlowDao.getTotalExpenseBetweenDates(startDate, endDate),
            transactionDao.getTransactionCountBetweenDates(startDate, endDate)
        ) { revenue, expenseNullable, count ->
            val expense = expenseNullable ?: 0.0
            val netProfit = revenue - expense
            FinancialSummary(
                totalRevenue = revenue,
                totalExpense = expense,
                totalTransactions = count,
                netProfit = netProfit
            )
        }.flowOn(ioDispatcher)

    override fun getExpensesBetweenDates(startDate: Long, endDate: Long): Flow<List<CashFlowEntity>> =
        cashFlowDao.getCashFlowsBetweenDates(startDate, endDate).flowOn(ioDispatcher)

    override fun getAllExpenses(): Flow<List<CashFlowEntity>> =
        cashFlowDao.getAllCashFlows().flowOn(ioDispatcher)

    override suspend fun addExpense(
        amount: Double,
        category: String,
        description: String,
        date: Long
    ): Long = withContext(ioDispatcher) {
        val entity = CashFlowEntity(
            amount = amount,
            category = category,
            description = description,
            date = date,
            type = Constants.CASH_FLOW_EXPENSE
        )
        cashFlowDao.insertCashFlow(entity)
    }

    override fun getRecentTransactions(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetweenDates(startDate, endDate).flowOn(ioDispatcher)
}
