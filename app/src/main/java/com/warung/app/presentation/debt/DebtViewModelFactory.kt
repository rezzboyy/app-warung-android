package com.warung.app.presentation.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.warung.app.domain.repository.DebtRepository

class DebtViewModelFactory(
    private val repository: DebtRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebtViewModel::class.java)) {
            return DebtViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
