package com.warung.app.utils

object Constants {
    const val DATABASE_NAME = "warung_pos.db"

    // Payment Methods
    const val PAYMENT_METHOD_CASH = "CASH"
    const val PAYMENT_METHOD_QRIS = "QRIS"
    const val PAYMENT_METHOD_DEBT = "DEBT"

    // Transaction Status
    const val STATUS_COMPLETED = "COMPLETED"
    const val STATUS_VOID = "VOID"
    const val STATUS_UNPAID = "UNPAID"

    // Debt Status
    const val DEBT_STATUS_UNPAID = "UNPAID"
    const val DEBT_STATUS_PARTIAL = "PARTIAL"
    const val DEBT_STATUS_PAID = "PAID"

    // Cash Flow Types
    const val CASH_FLOW_EXPENSE = "EXPENSE"
    const val CASH_FLOW_INCOME = "INCOME"
}
