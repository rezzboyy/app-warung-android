package com.warung.app.presentation.report

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.warung.app.data.local.entity.CashFlowEntity
import com.warung.app.data.local.entity.TransactionEntity
import com.warung.app.domain.model.FinancialSummary
import com.warung.app.domain.repository.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeReportRepository
    private lateinit var viewModel: ReportViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeReportRepository()
        viewModel = ReportViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDateRangeCalculations() {
        val (todayStart, todayEnd) = ReportViewModel.calculateDateRange(TimeRangeFilter.TODAY)
        assertTrue(todayStart <= todayEnd)

        val (weekStart, weekEnd) = ReportViewModel.calculateDateRange(TimeRangeFilter.LAST_7_DAYS)
        assertTrue(weekStart < weekEnd)
        assertTrue(weekStart < todayStart)

        val (allStart, allEnd) = ReportViewModel.calculateDateRange(TimeRangeFilter.ALL_TIME)
        assertEquals(0L, allStart)
        assertEquals(Long.MAX_VALUE, allEnd)
    }

    @Test
    fun testRecordExpense() = runTest(testDispatcher) {
        viewModel.recordExpense(
            amount = 35000.0,
            category = "Operasional",
            description = "Beli plastik dan es balok"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.expenses.size)
        val expense = fakeRepository.expenses[0]
        assertEquals(35000.0, expense.amount, 0.001)
        assertEquals("Operasional", expense.category)
        assertEquals("Beli plastik dan es balok", expense.description)
    }

    @Test
    fun testFinancialSummary() = runTest(testDispatcher) {
        fakeRepository.setSummary(
            FinancialSummary(
                totalRevenue = 500000.0,
                totalExpense = 150000.0,
                totalTransactions = 12,
                netProfit = 350000.0
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val summary = viewModel.summary.value
        assertEquals(500000.0, summary.totalRevenue, 0.001)
        assertEquals(150000.0, summary.totalExpense, 0.001)
        assertEquals(12, summary.totalTransactions)
        assertEquals(350000.0, summary.netProfit, 0.001)
    }

    @Test
    fun testChangeTimeRange() = runTest(testDispatcher) {
        viewModel.setTimeRange(TimeRangeFilter.THIS_MONTH)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TimeRangeFilter.THIS_MONTH, viewModel.timeRange.value)
    }
}

class FakeReportRepository : ReportRepository {
    val expenses = mutableListOf<CashFlowEntity>()
    private val summaryFlow = MutableStateFlow(FinancialSummary())
    private val expensesFlow = MutableStateFlow<List<CashFlowEntity>>(emptyList())

    fun setSummary(summary: FinancialSummary) {
        summaryFlow.value = summary
    }

    override fun getFinancialSummary(startDate: Long, endDate: Long): Flow<FinancialSummary> = summaryFlow

    override fun getExpensesBetweenDates(startDate: Long, endDate: Long): Flow<List<CashFlowEntity>> = expensesFlow

    override fun getAllExpenses(): Flow<List<CashFlowEntity>> = expensesFlow

    override suspend fun addExpense(
        amount: Double,
        category: String,
        description: String,
        date: Long
    ): Long {
        val id = (expenses.size + 1).toLong()
        val entity = CashFlowEntity(
            id = id,
            amount = amount,
            category = category,
            description = description,
            date = date
        )
        expenses.add(entity)
        expensesFlow.value = expenses.toList()
        return id
    }

    override fun getRecentTransactions(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        flowOf(emptyList())
}
