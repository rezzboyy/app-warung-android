package com.warung.app.data.repository

import androidx.room.withTransaction
import com.warung.app.data.local.AppDatabase
import com.warung.app.data.local.dao.ProductDao
import com.warung.app.data.local.dao.TransactionDao
import com.warung.app.data.local.entity.TransactionEntity
import com.warung.app.data.local.entity.TransactionItemEntity
import com.warung.app.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class TransactionRepositoryImpl(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val productDao: ProductDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions().flowOn(ioDispatcher)

    override fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetweenDates(startDate, endDate).flowOn(ioDispatcher)

    override fun getTransactionById(id: Long): Flow<TransactionEntity?> =
        transactionDao.getTransactionById(id).flowOn(ioDispatcher)

    override fun getTransactionItems(transactionId: Long): Flow<List<TransactionItemEntity>> =
        transactionDao.getTransactionItems(transactionId).flowOn(ioDispatcher)

    override fun getAllTransactionItems(): Flow<List<TransactionItemEntity>> =
        transactionDao.getAllTransactionItems().flowOn(ioDispatcher)

    override suspend fun processTransaction(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Long = withContext(ioDispatcher) {
        database.withTransaction {
            val transactionId = transactionDao.insertTransaction(transaction)
            val itemsWithTxId = items.map { it.copy(transactionId = transactionId) }
            transactionDao.insertTransactionItems(itemsWithTxId)

            itemsWithTxId.forEach { item ->
                productDao.reduceStock(item.productId, item.quantity)
            }

            transactionId
        }
    }
}
