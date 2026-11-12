package com.warung.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.warung.app.utils.Constants

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["invoice_number"], unique = true),
        Index(value = ["customer_id"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "invoice_number")
    val invoiceNumber: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "customer_id")
    val customerId: Long? = null,

    @ColumnInfo(name = "subtotal")
    val subtotal: Double,

    @ColumnInfo(name = "discount")
    val discount: Double = 0.0,

    @ColumnInfo(name = "grand_total")
    val grandTotal: Double,

    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = Constants.PAYMENT_METHOD_CASH,

    @ColumnInfo(name = "paid_amount")
    val paidAmount: Double,

    @ColumnInfo(name = "change_amount")
    val changeAmount: Double,

    @ColumnInfo(name = "status")
    val status: String = Constants.STATUS_COMPLETED
)
