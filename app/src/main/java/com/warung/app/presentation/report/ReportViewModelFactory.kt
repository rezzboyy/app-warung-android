package com.warung.app.presentation.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.warung.app.domain.repository.ReportRepository

class ReportViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            return ReportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
