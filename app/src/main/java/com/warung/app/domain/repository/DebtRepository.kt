package com.warung.app.domain.repository

import com.warung.app.data.local.entity.CustomerEntity
import com.warung.app.data.local.entity.DebtEntity
import kotlinx.coroutines.flow.Flow

interface DebtRepository {
    fun getAllCustomers(): Flow<List<CustomerEntity>>
    fun getCustomersWithDebt(): Flow<List<CustomerEntity>>
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>
    fun getCustomerById(id: Long): Flow<CustomerEntity?>
    fun getTotalOutstandingDebt(): Flow<Double>

    fun getDebtsByCustomerId(customerId: Long): Flow<List<DebtEntity>>
    fun getUnpaidDebts(): Flow<List<DebtEntity>>

    suspend fun saveCustomer(
        id: Long = 0,
        name: String,
        phoneNumber: String?,
        address: String?
    ): Long

    suspend fun addDebt(
        customerId: Long,
        amount: Double,
        notes: String?,
        dueDate: Long?
    ): Long

    suspend fun payDebt(
        customerId: Long,
        amount: Double,
        notes: String?
    )
}
