package com.warung.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.warung.app.utils.Constants

@Entity(
    tableName = "debts",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customer_id"]),
        Index(value = ["transaction_id"])
    ]
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "customer_id")
    val customerId: Long,

    @ColumnInfo(name = "transaction_id")
    val transactionId: Long? = null,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "remaining_amount")
    val remainingAmount: Double,

    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null,

    @ColumnInfo(name = "status")
    val status: String = Constants.DEBT_STATUS_UNPAID,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
