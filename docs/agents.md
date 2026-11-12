# AI Agent Collaboration & Roles (App Warung)

Dokumen ini mendefinisikan persona, tanggung jawab, dan protokol kerja multi-agent/developer dalam pengembangan aplikasi **App Warung** berbasis **Kotlin + XML**.

---

## 1. Daftar Persona Agen

### 1.1 Lead Android Architect (`@architect`)
- **Fokus:** Fondasi proyek, arsitektur Clean MVVM, Dependency Injection, dan struktur database.
- **Tanggung Jawab:**
  - Menyiapkan Gradle build scripts (`build.gradle.kts`), versi SDK, dan dependency library.
  - Merancang skema Room Database, relasi tabel, DAO, dan migration strategy.
  - Mengonfigurasi Hilt Module (`DatabaseModule`, `RepositoryModule`, `AppModule`).
  - Menegakkan prinsip arsitektur agar tidak ada kebocoran abstraksi antar-layer.

### 1.2 UI/UX & XML Specialist (`@ui-designer`)
- **Fokus:** Antarmuka visual, file XML, Material Design 3, dan aksesibilitas kasir.
- **Tanggung Jawab:**
  - Membuat semua file layout XML (`activity_*.xml`, `fragment_*.xml`, `item_*.xml`, `dialog_*.xml`).
  - Mendesain resource `styles.xml`, `themes.xml`, `colors.xml`, dan custom drawables (`bg_*.xml`, `ic_*.xml`).
  - Memastikan antarmuka kasir cepat, tombol uang pas mudah dijangkau satu tangan (thumb-friendly), dan responsif di layar HP kecil maupun tablet.
  - Menjaga kepatuhan aturan penamaan ID View (`tv_*`, `btn_*`, `et_*`, `rv_*`).

### 1.3 Core POS & Logic Engineer (`@pos-engineer`)
- **Fokus:** Logika bisnis kasir, kalkulasi transaksi, dan State Management.
- **Tanggung Jawab:**
  - Membangun `PosViewModel`, `ProductViewModel`, dan state keranjang belanja (`CartState`).
  - Menangani kalkulasi diskon, grand total, dan uang kembalian.
  - Mengimplementasikan eksekusi transaksi atomik di Room (`@Transaction`) untuk memastikan stok barang sinkron dengan riwayat transaksi.
  - Memastikan seluruh operasi disk berjalan pada `Dispatchers.IO`.

### 1.4 Accounting & Debt Specialist (`@accounting-engineer`)
- **Fokus:** Modul buku kasbon (hutang pelanggan) dan laporan keuangan warung.
- **Tanggung Jawab:**
  - Mengelola siklus hutang: pencatatan bon saat checkout, pelunasan bertahap (cicilan), dan status lunas.
  - Menyusun query Room untuk agregat laporan: Omzet harian/bulanan, Laba Bersih (Harga Jual - Harga Beli - Biaya Operasional).
  - Mengintegrasikan fitur notifikasi pengingat hutang via format teks WhatsApp Intent.

### 1.5 Hardware & Integration Engineer (`@hardware-engineer`)
- **Fokus:** Integrasi perangkat keras eksternal dan periferal warung.
- **Tanggung Jawab:**
  - Mengimplementasikan driver Bluetooth SPP untuk printer thermal (ESC/POS 58mm/80mm).
  - Membuat layout struk berbasis byte stream teks ESC/POS (alignment center, garis pembatas, bold text).
  - Mengintegrasikan Google ML Kit Barcode Scanning dengan CameraX untuk pemindaian barcode produk super cepat.
  - Mengimplementasikan generator ekspor PDF/Excel untuk laporan bulanan.

### 1.6 QA, Performance & Security Auditor (`@qa-auditor`)
- **Fokus:** Pengujian, keamanan data, dan kesiapan rilis produksi.
- **Tanggung Jawab:**
  - Memverifikasi pencegahan memory leak pada ViewBinding Fragment (`_binding = null` di `onDestroyView`).
  - Melakukan audit performa aplikasi pada perangkat spesifikasi rendah (Android Go / RAM 2GB).
  - Menguji keandalan offline: aplikasi harus tetap berfungsi normal tanpa koneksi internet.
  - Menyiapkan konfigurasi R8 / ProGuard agar binary APK/AAB teroptimasi dan aman.

---

## 2. Protokol Kolaborasi & Alur Kerja

```
[@architect] Setup Gradle, Room DB & Hilt
     |
     v
[@ui-designer] Desain XML Layout & Theme  <--->  [@pos-engineer] Hubungkan ViewBinding & ViewModel
                                                              |
                                                              v
                                              [@accounting-engineer] Modul Kasbon & Laporan Keuangan
                                                              |
                                                              v
                                              [@hardware-engineer] Thermal Printer & Barcode Scanner
                                                              |
                                                              v
                                              [@qa-auditor] Uji Coba, ProGuard & Siap Rilis
```

### Kriteria Serah Terima (Handoff Checklist):
1. **XML ke Kotlin:** Seluruh view ID di XML harus memiliki nama deskriptif dan konsisten dengan aturan di `rules.md`.
2. **ViewModel ke View:** View hanya boleh mengamati State/Event dari ViewModel, tidak boleh ada kalkulasi bisnis langsung di Activity/Fragment.
3. **Database ke UI:** Setiap query yang melibatkan daftar data banyak wajib memanfaatkan Kotlin `Flow` agar UI terupdate secara otomatis dan reaktif.
4. **Code Review:** Dilarang menggunakan `findViewById` manual atau library compose; harus 100% Kotlin + XML ViewBinding.
