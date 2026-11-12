package com.warung.app.data.local.pref

import android.content.Context
import android.content.SharedPreferences

class StorePreference(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var storeName: String
        get() = prefs.getString(KEY_STORE_NAME, "App Warung") ?: "App Warung"
        set(value) = prefs.edit().putString(KEY_STORE_NAME, value).apply()

    var storeAddress: String
        get() = prefs.getString(KEY_STORE_ADDRESS, "Jl. Raya Warung No. 1") ?: "Jl. Raya Warung No. 1"
        set(value) = prefs.edit().putString(KEY_STORE_ADDRESS, value).apply()

    var storePhone: String
        get() = prefs.getString(KEY_STORE_PHONE, "081234567890") ?: "081234567890"
        set(value) = prefs.edit().putString(KEY_STORE_PHONE, value).apply()

    var receiptFooter: String
        get() = prefs.getString(KEY_RECEIPT_FOOTER, "Terima kasih atas kunjungan Anda!") ?: "Terima kasih atas kunjungan Anda!"
        set(value) = prefs.edit().putString(KEY_RECEIPT_FOOTER, value).apply()

    var selectedPrinterMac: String?
        get() = prefs.getString(KEY_PRINTER_MAC, null)
        set(value) = prefs.edit().putString(KEY_PRINTER_MAC, value).apply()

    var selectedPrinterName: String?
        get() = prefs.getString(KEY_PRINTER_NAME, null)
        set(value) = prefs.edit().putString(KEY_PRINTER_NAME, value).apply()

    companion object {
        private const val PREF_NAME = "warung_store_prefs"
        private const val KEY_STORE_NAME = "store_name"
        private const val KEY_STORE_ADDRESS = "store_address"
        private const val KEY_STORE_PHONE = "store_phone"
        private const val KEY_RECEIPT_FOOTER = "receipt_footer"
        private const val KEY_PRINTER_MAC = "printer_mac"
        private const val KEY_PRINTER_NAME = "printer_name"

        @Volatile
        private var INSTANCE: StorePreference? = null

        fun getInstance(context: Context): StorePreference {
            return INSTANCE ?: synchronized(this) {
                val instance = StorePreference(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
