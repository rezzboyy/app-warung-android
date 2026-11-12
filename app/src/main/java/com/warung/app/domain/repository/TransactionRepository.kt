package com.warung.app.domain.repository

import com.warung.app.data.local.entity.TransactionEntity
import com.warung.app.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    fun getTransactionById(id: Long): Flow<TransactionEntity?>
    fun getTransactionItems(transactionId: Long): Flow<List<TransactionItemEntity>>
    fun getAllTransactionItems(): Flow<List<TransactionItemEntity>>
    suspend fun processTransaction(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Long
}
