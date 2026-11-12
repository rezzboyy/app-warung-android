package com.warung.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["category_id"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "barcode")
    val barcode: String? = null,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    @ColumnInfo(name = "buy_price")
    val buyPrice: Double,

    @ColumnInfo(name = "sell_price")
    val sellPrice: Double,

    @ColumnInfo(name = "stock")
    val stock: Int,

    @ColumnInfo(name = "unit")
    val unit: String = "Pcs",

    @ColumnInfo(name = "image_path")
    val imagePath: String? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
