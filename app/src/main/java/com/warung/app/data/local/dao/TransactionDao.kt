package com.warung.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.warung.app.data.local.entity.TransactionEntity
import com.warung.app.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE invoice_number = :invoiceNumber LIMIT 1")
    fun getTransactionByInvoice(invoiceNumber: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC")
    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(grand_total), 0.0) FROM transactions WHERE status = 'COMPLETED'")
    fun getTotalRevenue(): Flow<Double>

    @Query("SELECT COALESCE(SUM(grand_total), 0.0) FROM transactions WHERE status = 'COMPLETED' AND created_at BETWEEN :startDate AND :endDate")
    fun getTotalRevenueBetweenDates(startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'COMPLETED' AND created_at BETWEEN :startDate AND :endDate")
    fun getTransactionCountBetweenDates(startDate: Long, endDate: Long): Flow<Int>

    @Query("SELECT * FROM transaction_items WHERE transaction_id = :transactionId")
    fun getTransactionItems(transactionId: Long): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items")
    fun getAllTransactionItems(): Flow<List<TransactionItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateTransactionStatus(id: Long, status: String)

    @Transaction
    suspend fun insertTransactionWithItems(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Long {
        val transactionId = insertTransaction(transaction)
        val itemsWithId = items.map { it.copy(transactionId = transactionId) }
        insertTransactionItems(itemsWithId)
        return transactionId
    }
}
