package com.warung.app.data.repository

import com.warung.app.data.local.dao.CategoryDao
import com.warung.app.data.local.dao.ProductDao
import com.warung.app.data.local.entity.CategoryEntity
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.domain.repository.ProductRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ProductRepositoryImpl(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProductRepository {

    override fun getAllProducts(): Flow<List<ProductEntity>> =
        productDao.getAllActiveProducts().flowOn(ioDispatcher)

    override fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>> =
        productDao.getProductsByCategory(categoryId).flowOn(ioDispatcher)

    override fun searchProducts(query: String): Flow<List<ProductEntity>> =
        productDao.searchProducts(query).flowOn(ioDispatcher)

    override fun getProductByBarcode(barcode: String): Flow<ProductEntity?> =
        productDao.getProductByBarcode(barcode).flowOn(ioDispatcher)

    override fun getProductById(id: Long): Flow<ProductEntity?> =
        productDao.getProductById(id).flowOn(ioDispatcher)

    override fun getLowStockProducts(threshold: Int): Flow<List<ProductEntity>> =
        productDao.getLowStockProducts(threshold).flowOn(ioDispatcher)

    override suspend fun insertProduct(product: ProductEntity): Long =
        withContext(ioDispatcher) {
            productDao.insertProduct(product)
        }

    override suspend fun updateProduct(product: ProductEntity) =
        withContext(ioDispatcher) {
            productDao.updateProduct(product)
        }

    override suspend fun deleteProduct(product: ProductEntity) =
        withContext(ioDispatcher) {
            productDao.deleteProduct(product)
        }

    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories().flowOn(ioDispatcher)

    override suspend fun insertCategory(category: CategoryEntity): Long =
        withContext(ioDispatcher) {
            categoryDao.insertCategory(category)
        }
}
