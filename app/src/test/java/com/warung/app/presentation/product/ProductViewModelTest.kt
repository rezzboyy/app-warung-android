package com.warung.app.presentation.product

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.warung.app.data.local.entity.CategoryEntity
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeProductRepository
    private lateinit var viewModel: ProductViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeProductRepository()
        viewModel = ProductViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadCategoriesOnInit() = runTest(testDispatcher) {
        val categories = listOf(CategoryEntity(id = 1, name = "Sembako"))
        fakeRepository.setCategories(categories)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.categories.value.size)
        assertEquals("Sembako", viewModel.categories.value[0].name)
    }

    @Test
    fun testSaveProduct() = runTest(testDispatcher) {
        viewModel.saveProduct(
            id = 0,
            name = "Kopi Sachet",
            barcode = "123456",
            categoryId = 1,
            buyPrice = 1500.0,
            sellPrice = 2000.0,
            stock = 20,
            unit = "Sachet"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.productsList.size)
        assertEquals("Kopi Sachet", fakeRepository.productsList[0].name)
        assertEquals(2000.0, fakeRepository.productsList[0].sellPrice, 0.001)
    }

    @Test
    fun testDeleteProduct() = runTest(testDispatcher) {
        val product = ProductEntity(id = 1, name = "Kecap", buyPrice = 5000.0, sellPrice = 6000.0, stock = 5)
        fakeRepository.insertProduct(product)

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepository.productsList.size)

        viewModel.deleteProduct(product)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, fakeRepository.productsList.size)
    }
}

class FakeProductRepository : ProductRepository {
    val productsList = mutableListOf<ProductEntity>()
    private val categoriesList = mutableListOf<CategoryEntity>()

    private val productsFlow = MutableStateFlow<List<ProductEntity>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())

    fun setCategories(list: List<CategoryEntity>) {
        categoriesList.clear()
        categoriesList.addAll(list)
        categoriesFlow.value = categoriesList.toList()
    }

    override fun getAllProducts(): Flow<List<ProductEntity>> = productsFlow

    override fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>> =
        productsFlow.map { list -> list.filter { it.categoryId == categoryId } }

    override fun searchProducts(query: String): Flow<List<ProductEntity>> =
        productsFlow.map { list -> list.filter { it.name.contains(query, ignoreCase = true) } }

    override fun getProductByBarcode(barcode: String): Flow<ProductEntity?> =
        productsFlow.map { list -> list.find { it.barcode == barcode } }

    override fun getProductById(id: Long): Flow<ProductEntity?> =
        productsFlow.map { list -> list.find { it.id == id } }

    override fun getLowStockProducts(threshold: Int): Flow<List<ProductEntity>> =
        productsFlow.map { list -> list.filter { it.stock <= threshold } }

    override suspend fun insertProduct(product: ProductEntity): Long {
        productsList.add(product)
        productsFlow.value = productsList.toList()
        return product.id
    }

    override suspend fun updateProduct(product: ProductEntity) {
        val index = productsList.indexOfFirst { it.id == product.id }
        if (index != -1) {
            productsList[index] = product
            productsFlow.value = productsList.toList()
        }
    }

    override suspend fun deleteProduct(product: ProductEntity) {
        productsList.removeAll { it.id == product.id }
        productsFlow.value = productsList.toList()
    }

    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoriesFlow

    override suspend fun insertCategory(category: CategoryEntity): Long {
        categoriesList.add(category)
        categoriesFlow.value = categoriesList.toList()
        return category.id
    }
}
