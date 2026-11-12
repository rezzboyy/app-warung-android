package com.warung.app.presentation.debt

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.warung.app.data.local.entity.CustomerEntity
import com.warung.app.data.local.entity.DebtEntity
import com.warung.app.domain.repository.DebtRepository
import com.warung.app.utils.Constants
import com.warung.app.utils.Resource
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebtViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDebtRepository
    private lateinit var viewModel: DebtViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDebtRepository()
        viewModel = DebtViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSaveCustomerWithDebt() = runTest(testDispatcher) {
        viewModel.saveCustomerWithDebt(
            name = "Pak Budi",
            phone = "08123456789",
            address = "Jl. Melati No. 5",
            initialDebt = 50000.0,
            notes = "Kasbon sembako"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.customers.size)
        val customer = fakeRepository.customers[0]
        assertEquals("Pak Budi", customer.name)
        assertEquals(50000.0, customer.totalDebt, 0.001)

        assertEquals(1, fakeRepository.debts.size)
        assertEquals(50000.0, fakeRepository.debts[0].amount, 0.001)
    }

    @Test
    fun testAddAdditionalDebt() = runTest(testDispatcher) {
        val customerId = fakeRepository.saveCustomer(0, "Ibu Siti", "08199999", null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addDebt(customerId, "Ibu Siti", 25000.0, "Beli telur")
        testDispatcher.scheduler.advanceUntilIdle()

        val customer = fakeRepository.customers.find { it.id == customerId }
        assertEquals(25000.0, customer?.totalDebt ?: 0.0, 0.001)
    }

    @Test
    fun testPayDebt() = runTest(testDispatcher) {
        val customerId = fakeRepository.saveCustomer(0, "Ibu Siti", "08199999", null)
        fakeRepository.addDebt(customerId, 50000.0, "Awal", null)
        testDispatcher.scheduler.advanceUntilIdle()

        // Pay partial 20.000
        viewModel.payDebt(customerId, "Ibu Siti", 20000.0, "Cicilan 1")
        testDispatcher.scheduler.advanceUntilIdle()

        val customer = fakeRepository.customers.find { it.id == customerId }
        assertEquals(30000.0, customer?.totalDebt ?: 0.0, 0.001)

        // Pay remaining 30.000
        viewModel.payDebt(customerId, "Ibu Siti", 30000.0, "Lunas")
        testDispatcher.scheduler.advanceUntilIdle()

        val fullyPaidCustomer = fakeRepository.customers.find { it.id == customerId }
        assertEquals(0.0, fullyPaidCustomer?.totalDebt ?: 0.0, 0.001)
    }

    @Test
    fun testTotalOutstandingDebtObservable() = runTest(testDispatcher) {
        val c1 = fakeRepository.saveCustomer(0, "Pelanggan 1", null, null)
        fakeRepository.addDebt(c1, 10000.0, null, null)

        val c2 = fakeRepository.saveCustomer(0, "Pelanggan 2", null, null)
        fakeRepository.addDebt(c2, 25000.0, null, null)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(35000.0, viewModel.totalDebt.value, 0.001)
    }
}

class FakeDebtRepository : DebtRepository {
    val customers = mutableListOf<CustomerEntity>()
    val debts = mutableListOf<DebtEntity>()

    private val customersFlow = MutableStateFlow<List<CustomerEntity>>(emptyList())
    private val debtsFlow = MutableStateFlow<List<DebtEntity>>(emptyList())

    override fun getAllCustomers(): Flow<List<CustomerEntity>> = customersFlow

    override fun getCustomersWithDebt(): Flow<List<CustomerEntity>> =
        customersFlow.map { list -> list.filter { it.totalDebt > 0.0 } }

    override fun searchCustomers(query: String): Flow<List<CustomerEntity>> =
        customersFlow.map { list ->
            list.filter {
                it.name.contains(query, ignoreCase = true) ||
                (it.phoneNumber?.contains(query) == true)
            }
        }

    override fun getCustomerById(id: Long): Flow<CustomerEntity?> =
        customersFlow.map { list -> list.find { it.id == id } }

    override fun getTotalOutstandingDebt(): Flow<Double> =
        customersFlow.map { list -> list.sumOf { it.totalDebt } }

    override fun getDebtsByCustomerId(customerId: Long): Flow<List<DebtEntity>> =
        debtsFlow.map { list -> list.filter { it.customerId == customerId } }

    override fun getUnpaidDebts(): Flow<List<DebtEntity>> =
        debtsFlow.map { list -> list.filter { it.status != Constants.DEBT_STATUS_PAID } }

    override suspend fun saveCustomer(
        id: Long,
        name: String,
        phoneNumber: String?,
        address: String?
    ): Long {
        val newId = if (id == 0L) (customers.size + 1).toLong() else id
        val customer = CustomerEntity(
            id = newId,
            name = name,
            phoneNumber = phoneNumber,
            address = address,
            totalDebt = 0.0
        )
        customers.add(customer)
        customersFlow.value = customers.toList()
        return newId
    }

    override suspend fun addDebt(
        customerId: Long,
        amount: Double,
        notes: String?,
        dueDate: Long?
    ): Long {
        val debtId = (debts.size + 1).toLong()
        val debt = DebtEntity(
            id = debtId,
            customerId = customerId,
            amount = amount,
            remainingAmount = amount,
            notes = notes,
            dueDate = dueDate
        )
        debts.add(debt)
        debtsFlow.value = debts.toList()

        val index = customers.indexOfFirst { it.id == customerId }
        if (index != -1) {
            val current = customers[index]
            customers[index] = current.copy(totalDebt = current.totalDebt + amount)
            customersFlow.value = customers.toList()
        }

        return debtId
    }

    override suspend fun payDebt(customerId: Long, amount: Double, notes: String?) {
        val index = customers.indexOfFirst { it.id == customerId }
        if (index != -1) {
            val current = customers[index]
            customers[index] = current.copy(totalDebt = (current.totalDebt - amount).coerceAtLeast(0.0))
            customersFlow.value = customers.toList()
        }
    }
}
