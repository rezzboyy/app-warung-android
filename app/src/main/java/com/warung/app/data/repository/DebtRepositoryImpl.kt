package com.warung.app.data.repository

import androidx.room.withTransaction
import com.warung.app.data.local.AppDatabase
import com.warung.app.data.local.dao.DebtDao
import com.warung.app.data.local.entity.CustomerEntity
import com.warung.app.data.local.entity.DebtEntity
import com.warung.app.domain.repository.DebtRepository
import com.warung.app.utils.Constants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class DebtRepositoryImpl(
    private val database: AppDatabase,
    private val debtDao: DebtDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DebtRepository {

    override fun getAllCustomers(): Flow<List<CustomerEntity>> =
        debtDao.getAllCustomers().flowOn(ioDispatcher)

    override fun getCustomersWithDebt(): Flow<List<CustomerEntity>> =
        debtDao.getCustomersWithDebt().flowOn(ioDispatcher)

    override fun searchCustomers(query: String): Flow<List<CustomerEntity>> =
        debtDao.searchCustomers(query).flowOn(ioDispatcher)

    override fun getCustomerById(id: Long): Flow<CustomerEntity?> =
        debtDao.getCustomerById(id).flowOn(ioDispatcher)

    override fun getTotalOutstandingDebt(): Flow<Double> =
        debtDao.getTotalOutstandingDebt().flowOn(ioDispatcher)

    override fun getDebtsByCustomerId(customerId: Long): Flow<List<DebtEntity>> =
        debtDao.getDebtsByCustomerId(customerId).flowOn(ioDispatcher)

    override fun getUnpaidDebts(): Flow<List<DebtEntity>> =
        debtDao.getUnpaidDebts().flowOn(ioDispatcher)

    override suspend fun saveCustomer(
        id: Long,
        name: String,
        phoneNumber: String?,
        address: String?
    ): Long = withContext(ioDispatcher) {
        val customer = CustomerEntity(
            id = id,
            name = name,
            phoneNumber = phoneNumber,
            address = address
        )
        if (id == 0L) {
            debtDao.insertCustomer(customer)
        } else {
            debtDao.updateCustomer(customer)
            id
        }
    }

    override suspend fun addDebt(
        customerId: Long,
        amount: Double,
        notes: String?,
        dueDate: Long?
    ): Long = withContext(ioDispatcher) {
        database.withTransaction {
            val debt = DebtEntity(
                customerId = customerId,
                amount = amount,
                remainingAmount = amount,
                status = Constants.DEBT_STATUS_UNPAID,
                notes = notes,
                dueDate = dueDate,
                updatedAt = System.currentTimeMillis()
            )
            val debtId = debtDao.insertDebt(debt)
            debtDao.addCustomerDebt(customerId, amount)
            debtId
        }
    }

    override suspend fun payDebt(
        customerId: Long,
        amount: Double,
        notes: String?
    ) = withContext(ioDispatcher) {
        database.withTransaction {
            debtDao.deductCustomerDebt(customerId, amount)

            // Distribute payment to unpaid debts (FIFO)
            var paymentRemaining = amount
            val customerDebts = debtDao.getDebtsByCustomerId(customerId).first()
                .filter { it.status != Constants.DEBT_STATUS_PAID }

            for (debt in customerDebts) {
                if (paymentRemaining <= 0) break

                if (paymentRemaining >= debt.remainingAmount) {
                    paymentRemaining -= debt.remainingAmount
                    val updatedDebt = debt.copy(
                        remainingAmount = 0.0,
                        status = Constants.DEBT_STATUS_PAID,
                        updatedAt = System.currentTimeMillis()
                    )
                    debtDao.updateDebt(updatedDebt)
                } else {
                    val newRemaining = debt.remainingAmount - paymentRemaining
                    paymentRemaining = 0.0
                    val updatedDebt = debt.copy(
                        remainingAmount = newRemaining,
                        status = Constants.DEBT_STATUS_PARTIAL,
                        updatedAt = System.currentTimeMillis()
                    )
                    debtDao.updateDebt(updatedDebt)
                }
            }
        }
    }
}
