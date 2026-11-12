package com.warung.app.domain.repository

import com.warung.app.data.local.entity.CategoryEntity
import com.warung.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAllProducts(): Flow<List<ProductEntity>>
    fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>>
    fun searchProducts(query: String): Flow<List<ProductEntity>>
    fun getProductByBarcode(barcode: String): Flow<ProductEntity?>
    fun getProductById(id: Long): Flow<ProductEntity?>
    fun getLowStockProducts(threshold: Int = 5): Flow<List<ProductEntity>>
    suspend fun insertProduct(product: ProductEntity): Long
    suspend fun updateProduct(product: ProductEntity)
    suspend fun deleteProduct(product: ProductEntity)

    fun getAllCategories(): Flow<List<CategoryEntity>>
    suspend fun insertCategory(category: CategoryEntity): Long
}
