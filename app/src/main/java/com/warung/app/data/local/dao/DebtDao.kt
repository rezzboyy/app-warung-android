package com.warung.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.warung.app.data.local.entity.CustomerEntity
import com.warung.app.data.local.entity.DebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    // Customer operations
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerById(id: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE total_debt > 0 ORDER BY total_debt DESC")
    fun getCustomersWithDebt(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phone_number LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT COALESCE(SUM(total_debt), 0.0) FROM customers")
    fun getTotalOutstandingDebt(): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET total_debt = total_debt + :amount WHERE id = :customerId")
    suspend fun addCustomerDebt(customerId: Long, amount: Double)

    @Query("UPDATE customers SET total_debt = total_debt - :amount WHERE id = :customerId")
    suspend fun deductCustomerDebt(customerId: Long, amount: Double)

    // Debt operations
    @Query("SELECT * FROM debts ORDER BY updated_at DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE customer_id = :customerId ORDER BY updated_at DESC")
    fun getDebtsByCustomerId(customerId: Long): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE status != 'PAID' ORDER BY due_date ASC")
    fun getUnpaidDebts(): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)
}
