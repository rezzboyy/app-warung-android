# Roadmap & Task Breakdown (App Warung)

Dokumen ini memuat daftar tugas dan tahapan pengembangan aplikasi kasir & pembukuan **App Warung** dengan arsitektur **Kotlin + XML**.

---

## Status Progres
- [ ] Belum Dimulai
- [/] Sedang Dikerjakan
- [x] Selesai

> [!NOTE]
> **Catatan Pengujian Pengguna (Feedback Testing)**:
> 1. Ditemukan bug pada bagian layout (tata letak elemen tumpang tindih / overflow pada ukuran layar HP tertentu).
> 2. Beberapa tombol layout mengalami kendala/bug tampilan dan interaktivitas.
> 3. Fitur scanner barcode belum dapat memindai langsung menggunakan kamera HP (sebelumnya baru input teks/placeholder).
> Seluruh perencanaan perbaikan dijabarkan pada **Fase 8, 9, dan 10** di bawah.

---

## Fase 1: Inisialisasi Proyek & Fondasi Arsitektur
- [x] **1.1 Android Project Setup**
  - [x] Konfigurasi `build.gradle.kts` (Project & App level).
  - [x] Konfigurasi dependency: Jetpack Navigation, Room DB, ViewBinding, Coroutines, Material3.
  - [x] Setup ProGuard & BuildConfig untuk release/debug.
  - [x] Setup environment CLI: JDK 17, Android SDK (API 34), build-tools (34.0.0), ADB, dan Gradle 8.7.
- [x] **1.2 Desain Sistem & Base Classes**
  - [x] Setup `colors.xml`, `strings.xml`, `themes.xml` (Material3 Light/Dark).
  - [x] Setup base layout template (`activity_main.xml` dengan `BottomNavigationView`).
  - [x] Buat helper utilities: `CurrencyFormatter`, `DateFormatter`, `Resource<T>` state wrapper, `Constants`.
- [x] **1.3 Database Setup (Room)**
  - [x] Setup `AppDatabase.kt` dengan Room abstraction & pre-populasi kategori default.
  - [x] Setup Application-level container singleton di `AppWarungApplication.kt`.

---

## Fase 2: Manajemen Master Data (Produk & Kategori)
- [x] **2.1 Entitas & DAO Produk**
  - [x] Buat Room Entity: `CategoryEntity` dan `ProductEntity` (plus `CustomerEntity`, `DebtEntity`, `TransactionEntity`, `TransactionItemEntity`, `CashFlowEntity`).
  - [x] Buat `CategoryDao` & `ProductDao` (CRUD, reactive Flow query, filter stok tipis, update stok).
  - [x] Buat `ProductRepository` dan implementasinya (`ProductRepositoryImpl`).
- [x] **2.2 UI Manajemen Produk (XML + Fragment)**
  - [x] Buat layout `fragment_product_list.xml` (SearchView, Filter ChipGroup, RecyclerView, Empty State, FAB).
  - [x] Buat layout `item_product_card.xml` & adapter `ProductListAdapter` (Indikator stok tipis, harga modal/jual, aksi edit & hapus).
  - [x] Buat `ProductViewModel` & `ProductViewModelFactory` untuk handle state katalog produk reaktif via StateFlow & Coroutines.
  - [x] Buat form dialog `dialog_add_edit_product.xml` & `AddEditProductDialogFragment` (Validasi input nama, barcode, kategori dropdown, harga modal, harga jual, stok, satuan).
  - [x] Pengujian unit & fungsional: `ProductViewModelTest`, `CurrencyFormatterTest`, `DateFormatterTest`, `ResourceTest` (100% Passed).
  - [ ] Integrasi picker foto produk lokal.

---

## Fase 3: Modul Kasir & POS (Point of Sale)
- [x] **3.1 Keranjang Belanja & Business Logic**
  - [x] Model data keranjang: `CartItem` dan `CartState` (Subtotal, Grand Total, Margin Keuntungan, validasi stok).
  - [x] `PosViewModel` untuk kalkulasi subtotal, diskon, grand total, dan manajemen state reaktif.
  - [x] Validasi batas ketersediaan stok fisik saat barang ditambahkan ke keranjang.
- [x] **3.2 UI Kasir (XML + ViewBinding)**
  - [x] Buat layout `fragment_pos.xml` (Grid katalog produk kasir, filter kategori chips, bar pencarian, sticky bottom cart bar).
  - [x] Buat bottom sheet `bottom_sheet_cart.xml` & `CartBottomSheet` (Daftar item, stepper tambah/kurang qty, hapus item, tombol checkout).
  - [x] Buat dialog pembayaran `dialog_checkout.xml` & `CheckoutDialogFragment`:
    - [x] Pilihan metode bayar: Tunai (`CASH`), QRIS (`QRIS`), Kasbon (`DEBT`).
    - [x] Input nominal uang diterima & tombol pecahan cepat (Uang Pas, 10rb, 20rb, 50rb, 100rb).
    - [x] Kalkulasi kembalian otomatis & peringatan nominal uang kurang secara real-time.
- [x] **3.3 Eksekusi Transaksi & Atomic DB Write**
  - [x] `TransactionRepository` & `TransactionRepositoryImpl` (Atomic `@Transaction` di Room: Simpan transaksi + kurangi stok barang seketika).
  - [x] Dialog struk digital `dialog_receipt.xml` & `ReceiptDialogFragment` (Ringkasan transaksi sukses, detail item, invoice, tunai, dan kembalian).
  - [x] Pengujian unit & fungsional: `CartStateTest`, `PosViewModelTest` (100% Passed).

---

## Fase 4: Modul Kasbon & Hutang Pelanggan (Buku Bon)
- [x] **4.1 Entitas & DAO Hutang**
  - [x] Room Entity: `CustomerEntity` & `DebtEntity`.
  - [x] Query DAO: Total piutang agregat (`getTotalOutstandingDebt`), filter pelanggan berhutang, pencarian nama/no HP.
  - [x] `DebtRepository` & `DebtRepositoryImpl` (Distribusi pelunasan hutang FIFO otomatis di dalam `@Transaction`).
- [x] **4.2 UI Catatan Kasbon**
  - [x] Layout `fragment_debt_book.xml` & `DebtBookFragment` (Kartu ringkasan total piutang warung, filter chip status lunas/belum lunas, bar pencarian).
  - [x] Layout `item_customer_debt.xml` & `CustomerDebtAdapter` (Badge status hutang dinamis, tombol bayar/cicil, tombol WhatsApp).
  - [x] Form pendaftaran pelanggan & kasbon baru `dialog_add_customer_debt.xml` & `AddCustomerDebtDialogFragment`.
  - [x] Form pembayaran/cicilan hutang `dialog_pay_debt.xml` & `PayDebtDialogFragment` (Tombol bayar lunas instan & validasi pembayaran).
  - [x] Fitur kirim pengingat tagihan otomatis via WhatsApp Intent (`https://api.whatsapp.com/send?phone=...&text=...`) dengan format pesan sopan warung Indonesia.
  - [x] Pengujian unit & fungsional: `DebtViewModelTest` (100% Passed).

---

## Fase 5: Modul Laporan & Arus Kas (Keuangan)
- [x] **5.1 Pencatatan Pengeluaran Warung**
  - [x] Room Entity: `CashFlowEntity` (Pengeluaran operasional, kulakan, listrik, sewa, gaji).
  - [x] Dialog catat pengeluaran cepat `dialog_add_expense.xml` & `AddExpenseDialogFragment`.
- [x] **5.2 Dashboard Laporan**
  - [x] Layout `fragment_reports.xml` & `ReportFragment` (4 Kartu metrik: Omzet, Pengeluaran, Laba Bersih, Jumlah Transaksi).
  - [x] Filter rentang waktu: Hari Ini, 7 Hari Terakhir, Bulan Ini, dan Semua.
  - [x] Riwayat pengeluaran interaktif dengan `item_expense.xml` & `ExpenseAdapter`.
  - [x] `ReportRepository` & `ReportRepositoryImpl` (Penggabungan reaktif data penjualan kasir & arus kas).
  - [x] Pengujian unit & fungsional: `ReportViewModelTest` (100% Passed).
- [x] **5.3 Ekspor Data**
  - [x] Generator laporan ke format CSV / Excel (`CsvExporter.kt`).
  - [x] Ekspor transaksi kasir, pengeluaran, kasbon, dan katalog produk.
  - [x] Pengujian unit & fungsional: `CsvExporterTest` (100% Passed).

---

## Fase 6: Hardware & Fitur Tambahan
- [x] **6.1 Barcode Scanner**
  - [x] Dialog scan barcode `dialog_scan_barcode.xml` & `BarcodeScannerDialogFragment`.
  - [x] Tombol scan barcode di layar kasir POS & auto-insert ke keranjang belanja.
- [x] **6.2 Printer Thermal Bluetooth (ESC/POS)**
  - [x] Service Bluetooth SPP untuk koneksi printer thermal 58mm / 80mm (`BluetoothPrinterManager.kt`).
  - [x] Builder array byte ESC/POS struk kasir (`EscPosTicketBuilder.kt`): Header toko, rincian item, garis putus-putus, total, kembalian, footer.
  - [x] Fitur cetak struk langsung dari struk belanja & fitur Test Print di menu pengaturan.
  - [x] Pengujian unit & fungsional: `EscPosTicketBuilderTest` (100% Passed).
- [x] **6.3 Backup & Restore Database**
  - [x] Pencadangan database lokal Room SQLite (`warung_pos.db`) langsung ke direktori penyimpanan perangkat.
- [x] **6.4 Keamanan & Profil Warung**
  - [x] Dialog pengaturan profil toko (`dialog_settings.xml` & `SettingsDialogFragment`).
  - [x] Konfigurasi nama toko, alamat, telepon, dan footer struk via `StorePreference.kt`.

---

## Fase 7: Testing, Optimasi & Produksi
- [x] **7.1 Unit & Functional Testing**
  - [x] Seluruh rangkaian pengujian unit (8 Test Suites: POS, Produk, Kasbon, Laporan, Formatter, CsvExporter, EscPosTicketBuilder) lulus 100%.
- [x] **7.2 Build Produksi & Kompilasi Akhir**
  - [x] Build APK Android (`assembleDebug`) sukses (7.39 MB) bebas error dan siap dipakai langsung di HP.

---

## Fase 8: Perbaikan Bug Layout & Optimalisasi Tampilan UI
- [x] **8.1 Perbaikan Layout Item Kartu Produk POS (`item_pos_product.xml`)**
  - [x] Perbaiki konflik constraint antara `tv_pos_product_price`, `tv_pos_product_stock`, dan `btn_add_to_cart` yang menyebabkan teks stok terpotong/keluar dari batas kartu produk.
  - [x] Rapikan penataan tombol tambah `btn_add_to_cart` agar tetap proporsional dan tidak menabrak teks harga atau stok.
- [x] **8.2 Perbaikan Baris Tombol Aksi di Kartu Kasbon (`item_customer_debt.xml`)**
  - [x] Atasi masalah overflow tombol aksi (`btn_add_debt`, `btn_whatsapp_reminder`, `btn_pay_debt`) yang terlalu lebar pada layar smartphone standar.
  - [x] Desain ulang baris aksi dengan tombol icon yang lebih ringkas atau tata letak responsif bertingkat agar tidak terpotong.
- [x] **8.3 Perbaikan Padding & Clearance Sticky Cart POS (`fragment_pos.xml`)**
  - [x] Perbesar bottom padding pada `RecyclerView` produk POS agar kartu produk baris terbawah tidak tertutup oleh bar belanja mengambang (`card_bottom_cart`).
- [x] **8.4 Optimalisasi Tata Letak Dialog & Form Input**
  - [x] Tambahkan ikon/tombol trigger pemindai barcode kamera langsung di samping field input barcode pada form tambah/edit produk (`dialog_add_edit_product.xml`).
  - [x] Rapikan tata letak tombol pecahan uang cepat pada dialog checkout (`dialog_checkout.xml`) agar lebih adaptif terhadap total belanja warung.

---

## Fase 9: Integrasi Live Camera Scanner untuk Barcode HP (CameraX + ML Kit)
- [x] **9.1 Setup Dependency & Izin Kamera Android**
  - [x] Tambahkan library CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) dan Google ML Kit Barcode Scanning (`barcode-scanning`) ke `app/build.gradle.kts`.
  - [x] Verifikasi deklarasi `android.permission.CAMERA` dan `<uses-feature android:name="android.hardware.camera">` di `AndroidManifest.xml`.
  - [x] Implementasikan alur permintaan izin kamera (Runtime Permission Request) dengan dialog penjelasan jika izin belum diberikan oleh pengguna.
- [x] **9.2 Implementasi UI Dialog Scanner Kamera Interaktif (`dialog_scan_barcode.xml`)**
  - [x] Ganti placeholder card statis dengan `androidx.camera.view.PreviewView` untuk live feed kamera HP.
  - [x] Tambahkan overlay viewfinder kotak target pindai dengan animasi garis pemindaian barcode.
  - [x] Tambahkan kontrol kamera: tombol toggle Senter / Flashlight dan tombol switch kamera belakang/depan.
  - [x] Pertahankan opsi input manual nomor barcode sebagai alternatif bila barcode fisik pada kemasan rusak/pudar.
- [x] **9.3 Logika Pemindaian Barcode Real-time (`BarcodeScannerDialogFragment.kt`)**
  - [x] Integrasikan CameraX `ImageAnalysis` dengan ML Kit `BarcodeScanning` untuk mendeteksi berbagai format barcode (EAN-13, EAN-8, UPC-A, Code 128, QR Code).
  - [x] Berikan feedback instan berupa getar haptic (vibration) dan bunyi beep saat barcode berhasil terbaca.
  - [x] Integrasi ke alur POS: otomatis temukan produk di Room Database dan tambahkan ke keranjang belanja kasir.
  - [x] Integrasi ke formulir Tambah/Edit Produk: otomatis mengisi kolom barcode saat mendaftarkan barang baru.

---

## Fase 10: Audit Interaktivitas Tombol & Pengalaman Pengguna (UX Audit)
- [x] **10.1 Audit & Pengecekan Seluruh Tombol Layout**
  - [x] Verifikasi listener pada seluruh tombol navigasi, filter chip (kategori, rentang waktu laporan, status kasbon), dan tombol aksi cepat.
  - [x] Pastikan seluruh tombol memiliki visual feedback sentuhan (ripple effect) yang jelas dan disable state saat operasi async sedang berjalan untuk mencegah double-tap.
- [x] **10.2 Uji Kompatibilitas Layar & Rotasi**
  - [x] Uji tampilan layout pada berbagai densitas layar (mdpi hingga xxhdpi) dan ukuran layar HP (5 inci hingga 6.7 inci).
  - [x] Pastikan dialog input dan bottom sheet dapat di-scroll lancar saat keyboard virtual (soft keyboard) muncul tanpa menutupi tombol simpan/batal.

---

## Fase 11: Pengujian Menyeluruh & Build APK Terbaru
- [x] **11.1 Pengujian Unit & Logika Scanner**
  - [x] Jalankan seluruh unit test suite yang ada dan pastikan 100% lulus (Termasuk `CashDenominationHelperTest`).
- [x] **11.2 Pembuatan Ikon Aplikasi Resmi (Adaptive Icon & Branding)**
  - [x] Desain ikon modern Toko & Kasir Warung (kanopi warung bergaris oranye-putih, mesin kasir POS layar biru, kertas struk keluar, dan koin emas rupiah).
  - [x] Konfigurasi Android Adaptive Icon standar (`mipmap-anydpi-v26/ic_launcher.xml` & `ic_launcher_round.xml`) serta fallback untuk semua launcher Android.
- [x] **11.3 Kompilasi & Build APK Debug Baru**
  - [x] Jalankan `./gradlew assembleDebug` untuk menghasilkan berkas APK terbaru yang siap di-install dan diuji langsung di HP pengguna (`app-debug.apk` 30.1 MB).

---

## Fase 12: Perbaikan Bug Tampilan Layar Laporan & Navigasi (Berdasarkan Screenshot Pengguna)
- [x] **12.1 Perbaikan Filter Chips Rentang Waktu (`fragment_reports.xml`)**
  - [x] Bungkus ChipGroup dengan `HorizontalScrollView` (`singleLine="true"`, `scrollbars="none"`) agar chip "Semua" tidak terlempar ke baris kedua dan seluruh opsi filter ("Hari Ini", "7 Hari Terakhir", "Bulan Ini", "Semua") sejajar dalam 1 baris yang dapat digeser.
- [x] **12.2 Perbaikan Tombol Aksi Ekspor Laporan (`fragment_reports.xml`)**
  - [x] Perbaiki clipping/garis bawah tombol yang terpotong dengan menaikkan tinggi tombol menjadi `48dp` dan menyederhanakan teks menjadi `"Ekspor Laporan"` (1 baris teks proporsional).
  - [x] Selaraskan tinggi dan font tombol `+ Pengeluaran` menjadi `48dp` agar simetris.
- [x] **12.3 Perbaikan Label Menu Bottom Navigation (`activity_main.xml`)**
  - [x] Tambahkan konfigurasi `app:labelVisibilityMode="labeled"` pada `BottomNavigationView` agar seluruh tab ("Kasir", "Produk", "Kasbon", "Laporan") selalu menampilkan teks label bersama ikonnya (tidak hanya tab yang sedang aktif).
- [x] **12.4 Peningkatan Empty State Riwayat Pengeluaran (`fragment_reports.xml`)**
  - [x] Tambahkan ikon receipt dan deskripsi pemandu agar area riwayat pengeluaran tidak terlihat kosong atau patah saat belum ada data.

---

## Fase 13: Fitur Ekspor Laporan Dokumen Microsoft Word (DOCX / OpenXML)
- [x] **13.1 Arsitektur Generator DOCX Native (Pure Kotlin OpenXML)**
  - [x] Bangun generator OpenXML murni tanpa library pihak ketiga yang berat (`DocxReportExporter.kt`).
  - [x] Konfigurasi ZIP Packaging standar Office Open XML: `[Content_Types].xml`, `_rels/.rels`, `word/_rels/document.xml.rels`, `word/styles.xml`, `word/document.xml`.
  - [x] Format dokumen standar cetak A4 dengan margin 20mm, typography Calibri, header toko profesional (Nama Toko, Alamat, No. Telp, Periode, Tanggal Cetak).
- [x] **13.2 Format & Tata Letak Seluruh Jenis Transaksi & Riwayat Warung**
  - [x] **Ringkasan Eksekutif Keuangan**: Kotak kartu metrik eksekutif (Total Omzet, Total Pengeluaran, Keuntungan Bersih, Sisa Piutang Kasbon, Jumlah Transaksi).
  - [x] **Tabel 1: Riwayat Transaksi Penjualan Kasir**: Nomor urut, Nomor invoice, Tanggal/Waktu, Rincian lengkap nama barang dan quantity (`Nama Produk xQty`), Metode Pembayaran, dan Total Nilai Belanja.
  - [x] **Tabel 2: Riwayat Pengeluaran Operasional**: Tanggal/Waktu, Kategori pengeluaran, Keterangan biaya operasional, dan Nominal pengeluaran.
  - [x] **Tabel 3: Buku Kasbon & Piutang Pelanggan**: Nama pelanggan, Kontak HP/WA, Alamat, Status pelunasan, dan Sisa saldo hutang kasbon.
  - [x] **Tabel 4: Monitoring Katalog & Stok Produk**: Nama produk, Kategori barang, Kode Barcode EAN/UPC, Harga Modal, Harga Jual, Sisa Stok Fisik + Satuan, dan Status Stok (Aman / Menipis).
  - [x] **Kolom Pengesahan / Tanda Tangan**: Area tanda tangan penanggung jawab / pemilik toko warung dan footer otomatis.
- [x] **13.3 Fleksibilitas Pilihan Ekspor & FileProvider Sharing**
  - [x] Dukungan pilihan ekspor dokumen: Laporan Lengkap (Semua Bagian), Laporan Khusus Transaksi Kasir, Laporan Khusus Pengeluaran, Laporan Khusus Kasbon, Laporan Khusus Katalog & Stok, serta opsi CSV.
  - [x] Konfigurasi Android `FileProvider` (`res/xml/file_paths.xml` & `AndroidManifest.xml`) untuk berbagi langsung file `.docx` ke WhatsApp, Email, Google Docs, WPS Office, Microsoft Word, atau Google Drive.
- [x] **13.4 Pengujian Unit & Verifikasi Langsung di Perangkat (Device Test)**
  - [x] Buat suite pengujian unit `DocxReportExporterTest.kt` (Memverifikasi struktur ZIP, kelengkapan OpenXML, parsing XML escaping, rincian produk belanja). 100% Passed.
  - [x] Verifikasi dan instalasi langsung di HP fisik OnePlus 7 Pro (`GM1910` via ADB `eee39b64`).
  - [x] Uji eksekusi langsung ekspor DOCX (`Laporan_Lengkap_Warung.docx` berhasil digenerate di cache dan ditarik).

---

## Fase 14: Implementasi Sistem Pembaruan Otomatis (OTA) & Skrip Rilis 1-Klik (`release.ps1`)
- [x] **14.1 Pembuatan Keystore Rilis Permanen**
  - [x] Keystore `warung_release.jks` dibuat dengan algoritma RSA 2048-bit, masa berlaku 10.000 hari (hingga tahun 2054).
  - [x] Konfigurasi `signingConfigs` pada `build.gradle.kts` diselaraskan untuk build `release` dan `debug` (mencegah konflik signature saat in-place OTA update).
- [x] **14.2 Engine Pengecekan & Unduhan Pembaruan (`GitHubUpdateManager.kt`)**
  - [x] Client OTA mandiri terintegrasi ke GitHub Releases API (`rezzboyy/app-warung-android`).
  - [x] Pengecekan versi semantik otomatis (`isRemoteVersionNewer`) saat aplikasi dibuka (*cold start*).
  - [x] Unduhan streaming APK dengan progress bar dan peluncuran instan Android `PackageInstaller` via `FileProvider`.
  - [x] Izin Android: `INTERNET`, `ACCESS_NETWORK_STATE`, dan `REQUEST_INSTALL_PACKAGES` (Android 8.0+ Unknown Sources).
- [x] **14.3 Komponen Antarmuka Dialog Pembaruan (`AppUpdateDialogFragment.kt`)**
  - [x] Tampilan dialog Material 3 dengan badge versi, estimasi ukuran file APK, dan catatan pembaruan (*changelog*).
  - [x] Indikator progress unduhan real-time (0% s/d 100%).
  - [x] Tombol "Periksa Pembaruan Sistem (OTA)" manual pada menu Pengaturan Toko (`SettingsDialogFragment.kt`).
- [x] **14.4 Skrip Otomasi Rilis 1-Klik (`release.ps1`) & Rilis Perdana GitHub**
  - [x] Skrip PowerShell `release.ps1` untuk auto-bump versi, kompilasi APK release bertanda tangan, git commit & tag, dan publikasi langsung ke GitHub Releases via GitHub CLI (`gh`).
  - [x] Inisialisasi Git & pembuatan repositori publik `rezzboyy/app-warung-android`.
  - [x] Publikasi rilis perdana `v1.0.1` (Version Code 2) sukses diunggah ke GitHub Releases.


