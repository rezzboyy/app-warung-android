package com.warung.app

import android.app.Application
import com.warung.app.data.local.AppDatabase
import com.warung.app.data.repository.DebtRepositoryImpl
import com.warung.app.data.repository.ProductRepositoryImpl
import com.warung.app.data.repository.ReportRepositoryImpl
import com.warung.app.data.repository.TransactionRepositoryImpl
import com.warung.app.domain.repository.DebtRepository
import com.warung.app.domain.repository.ProductRepository
import com.warung.app.domain.repository.ReportRepository
import com.warung.app.domain.repository.TransactionRepository

class AppWarungApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(
            productDao = database.productDao(),
            categoryDao = database.categoryDao()
        )
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(
            database = database,
            transactionDao = database.transactionDao(),
            productDao = database.productDao()
        )
    }

    val debtRepository: DebtRepository by lazy {
        DebtRepositoryImpl(
            database = database,
            debtDao = database.debtDao()
        )
    }

    val reportRepository: ReportRepository by lazy {
        ReportRepositoryImpl(
            transactionDao = database.transactionDao(),
            cashFlowDao = database.cashFlowDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AppWarungApplication
            private set
    }
}
