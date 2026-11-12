package com.warung.app.presentation.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warung.app.data.local.entity.CashFlowEntity
import com.warung.app.domain.model.FinancialSummary
import com.warung.app.domain.repository.ReportRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TimeRangeFilter {
    TODAY,
    LAST_7_DAYS,
    THIS_MONTH,
    ALL_TIME
}

class ReportViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    private val _timeRange = MutableStateFlow(TimeRangeFilter.TODAY)
    val timeRange: StateFlow<TimeRangeFilter> = _timeRange.asStateFlow()

    private val _summary = MutableStateFlow(FinancialSummary())
    val summary: StateFlow<FinancialSummary> = _summary.asStateFlow()

    private val _expenses = MutableStateFlow<List<CashFlowEntity>>(emptyList())
    val expenses: StateFlow<List<CashFlowEntity>> = _expenses.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        observeData()
    }

    private fun observeData() {
        _timeRange.onEach { range ->
            val (startDate, endDate) = calculateDateRange(range)

            repository.getFinancialSummary(startDate, endDate)
                .onEach { _summary.value = it }
                .catch { _summary.value = FinancialSummary() }
                .launchIn(viewModelScope)

            repository.getExpensesBetweenDates(startDate, endDate)
                .onEach { _expenses.value = it }
                .catch { _expenses.value = emptyList() }
                .launchIn(viewModelScope)
        }.launchIn(viewModelScope)
    }

    fun setTimeRange(range: TimeRangeFilter) {
        _timeRange.value = range
    }

    fun recordExpense(amount: Double, category: String, description: String) {
        viewModelScope.launch {
            try {
                repository.addExpense(
                    amount = amount,
                    category = category.trim(),
                    description = description.trim(),
                    date = System.currentTimeMillis()
                )
                _userMessage.emit("Pengeluaran sebesar Rp ${amount.toLong()} berhasil dicatat!")
            } catch (e: Exception) {
                _userMessage.emit("Gagal mencatat pengeluaran: ${e.localizedMessage}")
            }
        }
    }

    companion object {
        fun calculateDateRange(range: TimeRangeFilter): Pair<Long, Long> {
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()

            return when (range) {
                TimeRangeFilter.TODAY -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    Pair(calendar.timeInMillis, now)
                }
                TimeRangeFilter.LAST_7_DAYS -> {
                    calendar.add(Calendar.DAY_OF_YEAR, -7)
                    Pair(calendar.timeInMillis, now)
                }
                TimeRangeFilter.THIS_MONTH -> {
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    Pair(calendar.timeInMillis, now)
                }
                TimeRangeFilter.ALL_TIME -> {
                    Pair(0L, Long.MAX_VALUE)
                }
            }
        }
    }
}
