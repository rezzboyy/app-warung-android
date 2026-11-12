# Rules & Standard Pedoman Pengembangan (App Warung)

Dokumen ini memuat standar teknis, konvensi penamaan, arsitektur, dan aturan kualitas kode untuk proyek **App Warung** berbasis **Kotlin + XML** (Android Native).

---

## 1. Tech Stack & Library Standards

| Komponen | Teknologi / Library | Catatan |
|---|---|---|
| **Bahasa Pemrograman** | Kotlin (1.9+ / 2.0+) | Null-safety ketat, coroutine-first |
| **UI Framework** | XML Layouts + ViewBinding | Tidak menggunakan Jetpack Compose atau synthetics |
| **Design System** | Material Components 3 (MDC) | `Theme.Material3.*` |
| **Arsitektur** | MVVM + Clean Architecture | UI (Activity/Fragment) -> ViewModel -> UseCase/Repository -> Data Source |
| **State & Reactive** | StateFlow / SharedFlow & LiveData | Gunakan Flow untuk asynchronous stream, LiveData untuk lifecycle-aware UI |
| **Local Database** | Room Database | Offline-first, SQLite abstraction |
| **Dependency Injection** | Hilt (Dagger Hilt) | `@AndroidEntryPoint`, `@HiltViewModel` |
| **Background Processing** | Kotlin Coroutines + WorkManager | WorkManager untuk background sync/backup berkala |
| **Image Loading** | Coil / Glide | Optimasi cache untuk foto produk |
| **Hardware Integration** | ESC/POS via Bluetooth SPP, ML Kit Barcode Scanning | Untuk printer thermal kasir & scan barcode produk |

---

## 2. Arsitektur & Prinsip Kode

### 2.1 Pola MVVM
1. **Activity / Fragment (View Layer):**
   - Hanya bertanggung jawab untuk rendering UI dan menangkap interaksi pengguna.
   - Dilarang keras menaruh kalkulasi harga, logika diskon, atau query database di dalam View.
   - Selalu gunakan **ViewBinding** (contoh: `ActivityMainBinding`, `ItemProductBinding`). Hindari `findViewById` manual.
   - Membersihkan binding reference di `onDestroyView()` pada Fragment untuk mencegah memory leak.

2. **ViewModel Layer:**
   - Menyimpan state UI dalam bentuk `StateFlow` atau `LiveData`.
   - Mengelola interaksi dengan Repository via Coroutines (`viewModelScope`).
   - Tidak boleh memegang referensi ke `Context`, `Activity`, atau `View` (hindari memory leak). Gunakan `AndroidViewModel` hanya jika mutlak membutuhkan Application context.

3. **Repository & Data Source Layer:**
   - Single Source of Truth (SSOT): Database lokal (Room) adalah sumber utama data aplikasi (offline-first).
   - Seluruh operasi disk I/O harus berjalan di `Dispatchers.IO`.

---

## 3. Konvensi Penamaan (Naming Conventions)

### 3.1 Kode Kotlin
- **Classes & Interfaces:** PascalCase (contoh: `ProductRepository`, `TransactionViewModel`, `ProductDao`).
- **Functions & Methods:** camelCase (contoh: `calculateGrandTotal()`, `insertTransaction()`).
- **Variables & Properties:** camelCase (contoh: `productPrice`, `customerDebt`).
- **Constants:** UPPER_SNAKE_CASE (contoh: `DATABASE_NAME`, `MAX_RECENT_TRANSACTIONS`).

### 3.2 XML Layouts & Resources
Semua nama file XML harus menggunakan format `prefix_subjek_deskripsi.xml` (lowercase_snake_case):

| Tipe Resource | Konvensi Penamaan | Contoh |
|---|---|---|
| **Activity Layout** | `activity_<nama>.xml` | `activity_main.xml`, `activity_pos.xml` |
| **Fragment Layout** | `fragment_<nama>.xml` | `fragment_dashboard.xml`, `fragment_product_list.xml` |
| **RecyclerView Item** | `item_<nama>.xml` | `item_product.xml`, `item_transaction.xml`, `item_cart.xml` |
| **Dialog Layout** | `dialog_<nama>.xml` | `dialog_pay_debt.xml`, `dialog_confirm_delete.xml` |
| **Custom View Layout** | `view_<nama>.xml` | `view_pos_summary.xml` |
| **Drawables** | `ic_<nama>.xml` / `bg_<nama>.xml` | `ic_shopping_cart.xml`, `bg_button_rounded.xml` |
| **Colors** | `<purpose>_<shade>` | `color_primary_600`, `text_secondary` |
| **Strings** | `<screen>_<deskripsi>` | `pos_checkout_button`, `error_insufficient_stock` |

### 3.3 ID View di XML
Gunakan prefix singkatan tipe widget diikuti fungsi elemen:
- `tv_` : TextView (`tv_product_title`, `tv_total_price`)
- `btn_` : Button (`btn_checkout`, `btn_scan_barcode`)
- `et_` / `til_` : EditText / TextInputLayout (`et_product_price`, `til_product_name`)
- `rv_` : RecyclerView (`rv_products`, `rv_cart_items`)
- `iv_` : ImageView (`iv_product_thumbnail`)
- `cb_` : CheckBox (`cb_debt_paid`)
- `s_` / `act_` : Spinner / AutoCompleteTextView (`act_category`)

---

## 4. UI/UX & Formatting Rules

1. **Format Mata Uang:**
   - Semua nominal uang wajib diformat ke format Rupiah (`Rp 15.000`) menggunakan utility helper tersentralisasi (contoh: `CurrencyFormatter.formatRupiah(amount)`).
2. **Ukuran Font & Spacing:**
   - Spacing & Margin: Wajib menggunakan `dp` (skala 4dp/8dp: `8dp`, `16dp`, `24dp`).
   - Ukuran Teks: Wajib menggunakan `sp`. Dilarang menggunakan `dp` untuk teks.
3. **Responsive & Tablet Support:**
   - Aplikasi warung sering digunakan di perangkat HP murah atau tablet kasir (landscaped POS).
   - Layout XML kasir harus mendukung scroll view atau konfigurasi tablet (`layout-w600dp`).
4. **Validasi Input Kasir:**
   - Input harga dan kuantitas hanya boleh menerima input angka (`android:inputType="number"` atau `numberDecimal`).
   - Sediakan tombol pintas uang pas (contoh: Rp 10.000, Rp 20.000, Rp 50.000, Rp 100.000).

---

## 5. Keamanan & Integritas Data

1. **Atomic Transactions:**
   - Transaksi penjualan dan pengurangan stok harus dibungkus dalam `@Transaction` di Room Database agar tidak terjadi stok minus jika aplikasi tertutup paksa di tengah proses.
2. **Offline-First Resilience:**
   - Seluruh fungsionalitas inti (kasir, catat stok, catat hutang) harus 100% dapat berjalan tanpa koneksi internet.
3. **Backup & Restore Lokal:**
   - Sediakan fitur ekspor dan impor database SQLite terenkripsi / zip ke memori perangkat (eksternal storage/Documents) agar data warung aman jika ganti perangkat.
