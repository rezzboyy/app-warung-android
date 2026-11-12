package com.warung.app.presentation.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warung.app.data.local.entity.CategoryEntity
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.domain.repository.ProductRepository
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

class ProductViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _productsState = MutableStateFlow<Resource<List<ProductEntity>>>(Resource.Loading)
    val productsState: StateFlow<Resource<List<ProductEntity>>> = _productsState.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        loadCategories()
        observeProducts()
    }

    private fun loadCategories() {
        repository.getAllCategories()
            .onEach { categoryList ->
                _categories.value = categoryList
            }
            .catch {
                _categories.value = emptyList()
            }
            .launchIn(viewModelScope)
    }

    private fun observeProducts() {
        combine(_searchQuery, _selectedCategoryId) { query, categoryId ->
            Pair(query, categoryId)
        }.onEach { (query, categoryId) ->
            _productsState.value = Resource.Loading
            val flow = when {
                query.isNotBlank() -> repository.searchProducts(query)
                categoryId != null -> repository.getProductsByCategory(categoryId)
                else -> repository.getAllProducts()
            }

            flow.catch { e ->
                _productsState.value = Resource.Error(e.localizedMessage ?: "Gagal memuat produk", e)
            }.collect { list ->
                _productsState.value = Resource.Success(list)
            }
        }.launchIn(viewModelScope)
    }

    fun searchProducts(query: String) {
        _searchQuery.value = query.trim()
    }

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun saveProduct(
        id: Long = 0,
        name: String,
        barcode: String?,
        categoryId: Long?,
        buyPrice: Double,
        sellPrice: Double,
        stock: Int,
        unit: String
    ) {
        viewModelScope.launch {
            try {
                val product = ProductEntity(
                    id = id,
                    name = name.trim(),
                    barcode = barcode?.trim()?.ifEmpty { null },
                    categoryId = categoryId,
                    buyPrice = buyPrice,
                    sellPrice = sellPrice,
                    stock = stock,
                    unit = unit.trim().ifEmpty { "Pcs" },
                    isActive = true
                )

                if (id == 0L) {
                    repository.insertProduct(product)
                    _userMessage.emit("Produk '${product.name}' berhasil ditambahkan!")
                } else {
                    repository.updateProduct(product)
                    _userMessage.emit("Produk '${product.name}' berhasil diperbarui!")
                }
            } catch (e: Exception) {
                _userMessage.emit("Gagal menyimpan produk: ${e.localizedMessage}")
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(product)
                _userMessage.emit("Produk '${product.name}' berhasil dihapus.")
            } catch (e: Exception) {
                _userMessage.emit("Gagal menghapus produk: ${e.localizedMessage}")
            }
        }
    }
}
