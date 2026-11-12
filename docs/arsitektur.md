# Arsitektur Aplikasi (App Warung)

Dokumen ini menjelaskan rancangan arsitektur perangkat lunak untuk **App Warung**, mengadopsi prinsip **Clean Architecture** yang dipadukan dengan pola **MVVM (Model-View-ViewModel)** berbasis **Kotlin + XML ViewBinding** dan pendekatan *Offline-First*.

---

## 1. Prinsip Utama Arsitektur

1. **Separation of Concerns (SoC):** Setiap layer memiliki batasan tanggung jawab yang tegas. Tidak ada logika bisnis di dalam Activity/Fragment, dan tidak ada ketergantungan framework Android di domain logic murni.
2. **Single Source of Truth (SSOT):** Room Database lokal adalah satu-satunya sumber kebenaran data aplikasi. Seluruh pembacaan data kasir, produk, dan hutang bersumber dari database lokal secara reaktif (`Flow`).
3. **Single Activity Pattern:** Aplikasi menggunakan satu `MainActivity` sebagai kontainer induk dengan `NavHostFragment` dan `BottomNavigationView` yang menaungi seluruh Fragment fitur.
4. **Offline-First Resilience:** Seluruh fitur inti (kasir, transaksi, pengurangan stok, pencatatan kasbon) dapat beroperasi 100% tanpa jaringan internet.

---

## 2. Diagram Aliran Data (Data Flow)

```
[ Pengguna ]
     │ (Interaksi: Klik tombol, Input barcode, Pilih item)
     ▼
[ View Layer: Activity / Fragment ] (XML Layouts + ViewBinding)
     │
     │ Memanggil fungsi aksi (Intent/Event)
     ▼
[ ViewModel Layer ]
     │ Memproses State (`StateFlow` / `LiveData`)
     │ Menjalankan coroutine di `viewModelScope`
     ▼
[ Domain Layer / Use Cases ] (KasirUseCase, StockUseCase, DebtUseCase)
     │ Validasi bisnis (cek kecukupan stok, kalkulasi diskon/margin)
     ▼
[ Repository Layer ] (ProductRepository, TransactionRepository, DebtRepository)
     │ Berjalan di `Dispatchers.IO`
     ▼
[ Local Data Source ]
     ├── [ Room Database ] (SQLite Tables: Products, Transactions, Debts, CashFlow)
     └── [ Hardware Driver ] (Bluetooth ESC/POS Thermal Printer, ML Kit CameraX)
```

---

## 3. Struktur Paket Proyek (Package Structure)

Kode sumber diatur berdasarkan fitur (*Package-by-Feature*) yang dipadukan dengan layer arsitektur:

```
com.warung.app/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── ProductDao.kt
│   │   │   ├── CategoryDao.kt
│   │   │   ├── TransactionDao.kt
│   │   │   ├── DebtDao.kt
│   │   │   └── CashFlowDao.kt
│   │   ├── entity/
│   │   │   ├── ProductEntity.kt
│   │   │   ├── CategoryEntity.kt
│   │   │   ├── TransactionEntity.kt
│   │   │   ├── TransactionItemEntity.kt
│   │   │   ├── CustomerEntity.kt
│   │   │   ├── DebtEntity.kt
│   │   │   └── CashFlowEntity.kt
│   │   └── AppDatabase.kt
│   ├── repository/
│   │   ├── ProductRepositoryImpl.kt
│   │   ├── TransactionRepositoryImpl.kt
│   │   └── DebtRepositoryImpl.kt
│   └── hardware/
│       ├── BluetoothPrinterManager.kt
│       └── EscPosTicketBuilder.kt
├── domain/
│   ├── model/
│   │   ├── Product.kt
│   │   ├── CartItem.kt
│   │   ├── TransactionSummary.kt
│   │   └── DebtSummary.kt
│   ├── repository/
│   │   ├── ProductRepository.kt
│   │   ├── TransactionRepository.kt
│   │   └── DebtRepository.kt
│   └── usecase/
│       ├── ProcessTransactionUseCase.kt
│       ├── ManageStockUseCase.kt
│       └── RecordDebtPaymentUseCase.kt
├── presentation/
│   ├── base/
│   │   ├── BaseFragment.kt
│   │   └── BaseViewModel.kt
│   ├── pos/                  # Fitur Kasir
│   │   ├── PosFragment.kt
│   │   ├── PosViewModel.kt
│   │   ├── adapter/
│   │   │   ├── ProductCatalogAdapter.kt
│   │   │   └── CartItemAdapter.kt
│   │   └── dialog/
│   │       ├── CartBottomSheet.kt
│   │       └── CheckoutDialog.kt
│   ├── product/              # Manajemen Stok & Produk
│   │   ├── ProductListFragment.kt
│   │   ├── AddEditProductFragment.kt
│   │   └── ProductViewModel.kt
│   ├── debt/                 # Buku Kasbon Pelanggan
│   │   ├── DebtBookFragment.kt
│   │   ├── DebtDetailFragment.kt
│   │   └── DebtViewModel.kt
│   ├── report/               # Laporan & Arus Kas
│   │   ├── ReportFragment.kt
│   │   └── ReportViewModel.kt
│   └── settings/             # Profil Toko & Printer
│       ├── SettingsFragment.kt
│       └── PrinterSettingDialog.kt
├── utils/
│   ├── CurrencyFormatter.kt   # Format Rupiah
│   ├── DateFormatter.kt       # Format Tanggal Transaksi
│   ├── Resource.kt            # State wrapper (Success, Error, Loading)
│   └── Constants.kt
├── MainActivity.kt
└── AppWarungApplication.kt
```

---

## 4. Penjelasan Tiap Layer

### 4.1 Presentation Layer (UI)
- **Komponen:** Activity, Fragment, RecyclerView Adapter, Dialog, BottomSheet.
- **Teknologi:** XML Layouts, Material Design 3, ViewBinding.
- **Aturan:**
  - View **hanya** bertugas menampilkan data dan mengirimkan input pengguna ke ViewModel.
  - Wajib membersihkan referensi ViewBinding di `onDestroyView()` pada setiap Fragment (`_binding = null`).
  - Tidak ada import `com.warung.app.data.local.entity.*` di View; View hanya mengetahui Model Domain.

### 4.2 ViewModel Layer
- **Komponen:** Jetpack ViewModel, `StateFlow`, `SharedFlow`.
- **Aturan:**
  - ViewModel mempertahankan state UI saat rotasi layar atau perubahan konfigurasi.
  - Memanggil UseCase atau Repository melalui `viewModelScope.launch(Dispatchers.IO)`.
  - Mengirimkan event satu kali (misal: *Show Toast*, *Show Error Dialog*, *Open Receipt Screen*) menggunakan `SharedFlow` atau SingleLiveEvent.

### 4.3 Domain Layer
- **Komponen:** Use Cases, Business Entities (POJO), Repository Interface.
- **Aturan:**
  - Merupakan inti logika aplikasi yang tidak bergantung pada framework Android.
  - Contoh: `ProcessTransactionUseCase` mengecek ketersediaan stok, menghitung harga total, membuat nomor invoice unik, lalu memanggil fungsi transaksi database.

### 4.4 Data Layer
- **Komponen:** Room Database, DAO, Entity, Repository Implementation, Hardware Drivers.
- **Aturan:**
  - Menghubungkan database lokal SQLite dengan entitas bisnis.
  - Menggunakan anotasi `@Transaction` di Room untuk memastikan eksekusi banyak operasi (seperti simpan transaksi + kurangi stok barang) bersifat atomik (all or nothing).

---

## 5. Strategi Hardware & Ekosistem Luar

1. **ESC/POS Bluetooth Thermal Printing:**
   - Dikelola oleh `BluetoothPrinterManager` yang berdiri sendiri.
   - Menggunakan soket Bluetooth SPP (Serial Port Profile, UUID: `00001101-0000-1000-8000-00805F9B34FB`).
   - `EscPosTicketBuilder` bertugas menyusun array byte teks struk (format 58mm / 80mm).
2. **Barcode Scanning:**
   - Terisolasi di modul visual menggunakan Google ML Kit Vision Barcode + CameraX.
   - Hasil scan dikirim langsung ke `PosViewModel.onBarcodeScanned(barcode)`.
