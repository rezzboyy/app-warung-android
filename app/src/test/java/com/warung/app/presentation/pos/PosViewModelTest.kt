package com.warung.app.presentation.pos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.data.local.entity.TransactionEntity
import com.warung.app.data.local.entity.TransactionItemEntity
import com.warung.app.domain.repository.TransactionRepository
import com.warung.app.presentation.product.FakeProductRepository
import com.warung.app.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeProductRepository: FakeProductRepository
    private lateinit var fakeTransactionRepository: FakeTransactionRepository
    private lateinit var viewModel: PosViewModel

    private val testProduct = ProductEntity(
        id = 1,
        name = "Kopi Hitam",
        buyPrice = 2000.0,
        sellPrice = 3000.0,
        stock = 2, // only 2 items in stock
        unit = "Cangkir"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeProductRepository = FakeProductRepository()
        fakeTransactionRepository = FakeTransactionRepository()
        viewModel = PosViewModel(fakeProductRepository, fakeTransactionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAddToCart() = runTest(testDispatcher) {
        viewModel.addToCart(testProduct)
        testDispatcher.scheduler.advanceUntilIdle()

        val cart = viewModel.cartState.value
        assertEquals(1, cart.items.size)
        assertEquals(1, cart.items[0].quantity)
        assertEquals(3000.0, cart.grandTotal, 0.001)

        // Add same item again
        viewModel.addToCart(testProduct)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedCart = viewModel.cartState.value
        assertEquals(1, updatedCart.items.size)
        assertEquals(2, updatedCart.items[0].quantity)
        assertEquals(6000.0, updatedCart.grandTotal, 0.001)
    }

    @Test
    fun testAddToCartExceedingStockClamped() = runTest(testDispatcher) {
        // Product has stock = 2
        viewModel.addToCart(testProduct) // qty = 1
        viewModel.addToCart(testProduct) // qty = 2 (max reached)
        viewModel.addToCart(testProduct) // should NOT increase beyond 2!

        testDispatcher.scheduler.advanceUntilIdle()

        val cart = viewModel.cartState.value
        assertEquals(2, cart.items[0].quantity)
    }

    @Test
    fun testDecreaseAndRemoveCartItem() = runTest(testDispatcher) {
        viewModel.addToCart(testProduct)
        viewModel.addToCart(testProduct) // qty = 2
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.decreaseCartItem(testProduct.id) // qty becomes 1
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.cartState.value.items[0].quantity)

        viewModel.decreaseCartItem(testProduct.id) // item removed
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.cartState.value.isEmpty)
    }

    @Test
    fun testCheckoutSuccess() = runTest(testDispatcher) {
        viewModel.addToCart(testProduct) // 1 item, grandTotal = 3000
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.checkout(
            paymentMethod = Constants.PAYMENT_METHOD_CASH,
            paidAmount = 5000.0
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Cart should be empty
        assertTrue(viewModel.cartState.value.isEmpty)

        // Transaction saved in repository
        assertEquals(1, fakeTransactionRepository.transactions.size)
        val savedTx = fakeTransactionRepository.transactions[0]
        assertEquals(3000.0, savedTx.grandTotal, 0.001)
        assertEquals(5000.0, savedTx.paidAmount, 0.001)
        assertEquals(2000.0, savedTx.changeAmount, 0.001)

        // Completed transaction state populated
        assertNotNull(viewModel.completedTransaction.value)
    }
}

class FakeTransactionRepository : TransactionRepository {
    val transactions = mutableListOf<TransactionEntity>()
    val transactionItems = mutableListOf<TransactionItemEntity>()

    override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(transactions)

    override fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        flowOf(transactions.filter { it.createdAt in startDate..endDate })

    override fun getTransactionById(id: Long): Flow<TransactionEntity?> =
        flowOf(transactions.find { it.id == id })

    override fun getTransactionItems(transactionId: Long): Flow<List<TransactionItemEntity>> =
        flowOf(transactionItems.filter { it.transactionId == transactionId })

    override fun getAllTransactionItems(): Flow<List<TransactionItemEntity>> =
        flowOf(transactionItems)

    override suspend fun processTransaction(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Long {
        val id = (transactions.size + 1).toLong()
        val txWithId = transaction.copy(id = id)
        transactions.add(txWithId)
        transactionItems.addAll(items.map { it.copy(transactionId = id) })
        return id
    }
}
