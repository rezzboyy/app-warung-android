package com.warung.app.presentation.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warung.app.data.local.entity.CategoryEntity
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.data.local.entity.TransactionEntity
import com.warung.app.data.local.entity.TransactionItemEntity
import com.warung.app.domain.model.CartItem
import com.warung.app.domain.model.CartState
import com.warung.app.domain.repository.ProductRepository
import com.warung.app.domain.repository.TransactionRepository
import com.warung.app.utils.Constants
import com.warung.app.utils.DateFormatter
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

sealed class PosEvent {
    data class ShowToast(val message: String) : PosEvent()
    object OpenCheckoutDialog : PosEvent()
    data class TransactionSuccess(
        val transaction: TransactionEntity,
        val items: List<TransactionItemEntity>
    ) : PosEvent()
}

class PosViewModel(
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _productsState = MutableStateFlow<Resource<List<ProductEntity>>>(Resource.Loading)
    val productsState: StateFlow<Resource<List<ProductEntity>>> = _productsState.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    private val _completedTransaction = MutableStateFlow<Pair<TransactionEntity, List<TransactionItemEntity>>?>(null)
    val completedTransaction: StateFlow<Pair<TransactionEntity, List<TransactionItemEntity>>?> = _completedTransaction.asStateFlow()

    private val _posEvent = MutableSharedFlow<PosEvent>()
    val posEvent: SharedFlow<PosEvent> = _posEvent.asSharedFlow()

    init {
        loadCategories()
        observeProducts()
    }

    private fun loadCategories() {
        productRepository.getAllCategories()
            .onEach { _categories.value = it }
            .catch { _categories.value = emptyList() }
            .launchIn(viewModelScope)
    }

    private fun observeProducts() {
        combine(_searchQuery, _selectedCategoryId) { query, categoryId ->
            Pair(query, categoryId)
        }.onEach { (query, categoryId) ->
            _productsState.value = Resource.Loading
            val flow = when {
                query.isNotBlank() -> productRepository.searchProducts(query)
                categoryId != null -> productRepository.getProductsByCategory(categoryId)
                else -> productRepository.getAllProducts()
            }

            flow.catch { e ->
                _productsState.value = Resource.Error(e.localizedMessage ?: "Gagal memuat produk kasir", e)
            }.collect { list ->
                _productsState.value = Resource.Success(list)
            }
        }.launchIn(viewModelScope)
    }

    fun search(query: String) {
        _searchQuery.value = query.trim()
    }

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun addToCart(product: ProductEntity) {
        val currentItems = _cartState.value.items.toMutableList()
        val existingIndex = currentItems.indexOfFirst { it.product.id == product.id }

        if (existingIndex >= 0) {
            val currentItem = currentItems[existingIndex]
            if (currentItem.quantity < product.stock) {
                currentItems[existingIndex] = currentItem.copy(quantity = currentItem.quantity + 1)
                _cartState.value = _cartState.value.copy(items = currentItems)
            } else {
                viewModelScope.launch {
                    _posEvent.emit(PosEvent.ShowToast("Stok '${product.name}' tidak mencukupi (tersedia: ${product.stock} ${product.unit})"))
                }
            }
        } else {
            if (product.stock > 0) {
                currentItems.add(CartItem(product = product, quantity = 1))
                _cartState.value = _cartState.value.copy(items = currentItems)
            } else {
                viewModelScope.launch {
                    _posEvent.emit(PosEvent.ShowToast("Stok '${product.name}' sudah habis!"))
                }
            }
        }
    }

    fun decreaseCartItem(productId: Long) {
        val currentItems = _cartState.value.items.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == productId }

        if (index >= 0) {
            val item = currentItems[index]
            if (item.quantity > 1) {
                currentItems[index] = item.copy(quantity = item.quantity - 1)
                _cartState.value = _cartState.value.copy(items = currentItems)
            } else {
                currentItems.removeAt(index)
                _cartState.value = _cartState.value.copy(items = currentItems)
            }
        }
    }

    fun removeCartItem(productId: Long) {
        val updatedItems = _cartState.value.items.filterNot { it.product.id == productId }
        _cartState.value = _cartState.value.copy(items = updatedItems)
    }

    fun clearCart() {
        _cartState.value = CartState()
    }

    fun checkout(paymentMethod: String, paidAmount: Double) {
        val cart = _cartState.value
        if (cart.isEmpty) {
            viewModelScope.launch {
                _posEvent.emit(PosEvent.ShowToast("Keranjang belanja masih kosong!"))
            }
            return
        }

        val grandTotal = cart.grandTotal
        val changeAmount = if (paymentMethod == Constants.PAYMENT_METHOD_DEBT) {
            0.0
        } else {
            (paidAmount - grandTotal).coerceAtLeast(0.0)
        }

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val invoiceNumber = "TRX-${DateFormatter.formatInvoiceDate(now)}-${(1000..9999).random()}"

                val transaction = TransactionEntity(
                    invoiceNumber = invoiceNumber,
                    createdAt = now,
                    subtotal = cart.subtotal,
                    discount = cart.discount,
                    grandTotal = grandTotal,
                    paymentMethod = paymentMethod,
                    paidAmount = paidAmount,
                    changeAmount = changeAmount,
                    status = if (paymentMethod == Constants.PAYMENT_METHOD_DEBT) Constants.STATUS_UNPAID else Constants.STATUS_COMPLETED
                )

                val items = cart.items.map { cartItem ->
                    TransactionItemEntity(
                        transactionId = 0L,
                        productId = cartItem.product.id,
                        productName = cartItem.product.name,
                        buyPrice = cartItem.product.buyPrice,
                        sellPrice = cartItem.product.sellPrice,
                        quantity = cartItem.quantity,
                        subtotal = cartItem.subtotal
                    )
                }

                transactionRepository.processTransaction(transaction, items)
                _completedTransaction.value = Pair(transaction, items)
                _posEvent.emit(PosEvent.TransactionSuccess(transaction, items))
                clearCart()
            } catch (e: Exception) {
                _posEvent.emit(PosEvent.ShowToast("Gagal memproses transaksi: ${e.localizedMessage}"))
            }
        }
    }

    fun resetCompletedTransaction() {
        _completedTransaction.value = null
    }
}
