package com.warung.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.warung.app.data.local.entity.CashFlowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashFlowDao {

    @Query("SELECT * FROM cash_flows ORDER BY date DESC")
    fun getAllCashFlows(): Flow<List<CashFlowEntity>>

    @Query("SELECT * FROM cash_flows WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getCashFlowsBetweenDates(startDate: Long, endDate: Long): Flow<List<CashFlowEntity>>

    @Query("SELECT SUM(amount) FROM cash_flows WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate")
    fun getTotalExpenseBetweenDates(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM cash_flows WHERE type = 'INCOME' AND date BETWEEN :startDate AND :endDate")
    fun getTotalIncomeBetweenDates(startDate: Long, endDate: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashFlow(cashFlow: CashFlowEntity): Long

    @Delete
    suspend fun deleteCashFlow(cashFlow: CashFlowEntity)
}
