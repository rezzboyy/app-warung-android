package com.warung.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.warung.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE is_active = 1 ORDER BY name ASC")
    fun getAllActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category_id = :categoryId AND is_active = 1 ORDER BY name ASC")
    fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE (name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') AND is_active = 1 ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :barcode AND is_active = 1 LIMIT 1")
    fun getProductByBarcode(barcode: String): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun getProductById(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE stock <= :threshold AND is_active = 1 ORDER BY stock ASC")
    fun getLowStockProducts(threshold: Int = 5): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :productId AND stock >= :quantity")
    suspend fun reduceStock(productId: Long, quantity: Int): Int

    @Query("UPDATE products SET stock = stock + :quantity WHERE id = :productId")
    suspend fun restoreStock(productId: Long, quantity: Int): Int
}
