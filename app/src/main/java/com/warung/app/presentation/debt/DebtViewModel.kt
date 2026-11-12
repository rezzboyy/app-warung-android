package com.warung.app.presentation.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warung.app.data.local.entity.CustomerEntity
import com.warung.app.domain.repository.DebtRepository
import com.warung.app.utils.Resource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

enum class DebtFilterMode {
    ALL,
    UNPAID_ONLY,
    PAID_ONLY
}

class DebtViewModel(
    private val repository: DebtRepository
) : ViewModel() {

    private val _customersState = MutableStateFlow<Resource<List<CustomerEntity>>>(Resource.Loading)
    val customersState: StateFlow<Resource<List<CustomerEntity>>> = _customersState.asStateFlow()

    private val _totalDebt = MutableStateFlow(0.0)
    val totalDebt: StateFlow<Double> = _totalDebt.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterMode = MutableStateFlow(DebtFilterMode.ALL)
    val filterMode: StateFlow<DebtFilterMode> = _filterMode.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        observeTotalDebt()
        observeCustomers()
    }

    private fun observeTotalDebt() {
        repository.getTotalOutstandingDebt()
            .onEach { _totalDebt.value = it }
            .catch { _totalDebt.value = 0.0 }
            .launchIn(viewModelScope)
    }

    private fun observeCustomers() {
        combine(_searchQuery, _filterMode) { query, filter ->
            Pair(query, filter)
        }.onEach { (query, filter) ->
            _customersState.value = Resource.Loading

            val flow = if (query.isNotBlank()) {
                repository.searchCustomers(query)
            } else {
                when (filter) {
                    DebtFilterMode.ALL -> repository.getAllCustomers()
                    DebtFilterMode.UNPAID_ONLY -> repository.getCustomersWithDebt()
                    DebtFilterMode.PAID_ONLY -> repository.getAllCustomers()
                }
            }

            flow.catch { e ->
                _customersState.value = Resource.Error(e.localizedMessage ?: "Gagal memuat daftar kasbon", e)
            }.collect { list ->
                val filteredList = when (filter) {
                    DebtFilterMode.PAID_ONLY -> list.filter { it.totalDebt <= 0.0 }
                    DebtFilterMode.UNPAID_ONLY -> list.filter { it.totalDebt > 0.0 }
                    DebtFilterMode.ALL -> list
                }
                _customersState.value = Resource.Success(filteredList)
            }
        }.launchIn(viewModelScope)
    }

    fun search(query: String) {
        _searchQuery.value = query.trim()
    }

    fun setFilter(mode: DebtFilterMode) {
        _filterMode.value = mode
    }

    fun saveCustomerWithDebt(
        name: String,
        phone: String?,
        address: String?,
        initialDebt: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                val customerId = repository.saveCustomer(
                    id = 0L,
                    name = name.trim(),
                    phoneNumber = phone?.trim()?.ifEmpty { null },
                    address = address?.trim()?.ifEmpty { null }
                )

                if (initialDebt > 0.0) {
                    repository.addDebt(
                        customerId = customerId,
                        amount = initialDebt,
                        notes = notes?.trim()?.ifEmpty { "Kasbon Awal" },
                        dueDate = null
                    )
                }

                _userMessage.emit("Pelanggan '$name' berhasil disimpan!")
            } catch (e: Exception) {
                _userMessage.emit("Gagal menyimpan: ${e.localizedMessage}")
            }
        }
    }

    fun addDebt(customerId: Long, customerName: String, amount: Double, notes: String?) {
        viewModelScope.launch {
            try {
                repository.addDebt(customerId, amount, notes, null)
                _userMessage.emit("Kasbon tambahan berhasil dicatat untuk '$customerName'")
            } catch (e: Exception) {
                _userMessage.emit("Gagal mencatat kasbon: ${e.localizedMessage}")
            }
        }
    }

    fun payDebt(customerId: Long, customerName: String, amount: Double, notes: String?) {
        viewModelScope.launch {
            try {
                repository.payDebt(customerId, amount, notes)
                _userMessage.emit("Pembayaran Rp ${amount.toLong()} berhasil dicatat untuk '$customerName'")
            } catch (e: Exception) {
                _userMessage.emit("Gagal mencatat pembayaran: ${e.localizedMessage}")
            }
        }
    }
}
