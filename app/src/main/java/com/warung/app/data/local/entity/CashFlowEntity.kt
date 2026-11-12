package com.warung.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.warung.app.utils.Constants

@Entity(tableName = "cash_flows")
data class CashFlowEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "date")
    val date: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "type")
    val type: String = Constants.CASH_FLOW_EXPENSE,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "description")
    val description: String
)
