package com.warung.app.presentation.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.warung.app.domain.repository.ProductRepository
import com.warung.app.domain.repository.TransactionRepository

class PosViewModelFactory(
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PosViewModel::class.java)) {
            return PosViewModel(productRepository, transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
