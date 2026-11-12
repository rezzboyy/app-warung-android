# Design & Architecture Document (App Warung)

Dokumen ini menjelaskan arsitektur perangkat lunak, skema basis data, dan perancangan antarmuka pengguna (UI/UX) untuk **App Warung** dengan stack **Kotlin + XML** pada platform Android Native.

---

## 1. Arsitektur Sistem

Aplikasi dibangun menggunakan prinsip **Clean Architecture** yang dipadukan dengan pola **MVVM (Model-View-ViewModel)**.

```
+--------------------------------------------------------------+
|                    PRESENTATION LAYER                        |
|   Activities / Fragments (XML Layouts + ViewBinding)        |
|                               ^                              |
|                               | Observes (StateFlow/LiveData)|
|                               v                              |
|                          ViewModels                          |
+------------------------------+-------------------------------+
                               |
                               v Calls
+--------------------------------------------------------------+
|                       DOMAIN LAYER                           |
|       Use Cases (KasirUseCase, StockUseCase, DebtUseCase)    |
|                Business Models & Formatters                  |
+------------------------------+-------------------------------+
                               |
                               v Implements
+--------------------------------------------------------------+
|                        DATA LAYER                            |
|                     Repository Pattern                       |
|          +-------------------+--------------------+          |
|          | Local Data Source | Hardware / IO      |          |
|          | (Room DB / SQLite)| (Thermal ESC/POS)  |          |
|          | SharedPreferences | ML Kit Barcode     |          |
|          +-------------------+--------------------+          |
+--------------------------------------------------------------+
```

---

## 2. Skema Basis Data (Room Database)

Aplikasi beroperasi secara *offline-first* dengan basis data lokal Room. Berikut adalah struktur tabel utama:

### 2.1 Entity Relationship Diagram (ERD) Ringkas

```
[categories] 1 --- * [products]
                         |
                         *
             [transaction_items] * --- 1 [transactions]
                                              |
                                              *
[customers] 1 ------------------------- * [debts]
```

### 2.2 Rincian Tabel

1. **`categories`**
   - `id`: Long (PK, AutoGenerate)
   - `name`: String
   - `icon_name`: String?

2. **`products`**
   - `id`: Long (PK, AutoGenerate)
   - `barcode`: String? (Indexed, Unique jika ada)
   - `name`: String
   - `category_id`: Long (FK ke `categories.id`, ON DELETE SET NULL)
   - `buy_price`: Double (Harga Modal/Beli)
   - `sell_price`: Double (Harga Jual)
   - `stock`: Int
   - `unit`: String (contoh: "Pcs", "Kg", "Bungkus", "Renceng")
   - `image_path`: String?
   - `is_active`: Boolean

3. **`customers`**
   - `id`: Long (PK, AutoGenerate)
   - `name`: String
   - `phone_number`: String?
   - `address`: String?
   - `total_debt`: Double (Agregat sisa hutang)

4. **`transactions`**
   - `id`: Long (PK, AutoGenerate)
   - `invoice_number`: String (Unique, e.g. `TRX-20261020-001`)
   - `created_at`: Long (Timestamp epoch)
   - `customer_id`: Long? (FK ke `customers.id`)
   - `subtotal`: Double
   - `discount`: Double
   - `grand_total`: Double
   - `payment_method`: String (`CASH`, `QRIS`, `DEBT`)
   - `paid_amount`: Double
   - `change_amount`: Double
   - `status`: String (`COMPLETED`, `VOID`, `UNPAID`)

5. **`transaction_items`**
   - `id`: Long (PK, AutoGenerate)
   - `transaction_id`: Long (FK ke `transactions.id`, ON DELETE CASCADE)
   - `product_id`: Long
   - `product_name`: String (Snapshot nama saat transaksi)
   - `buy_price`: Double (Snapshot harga modal)
   - `sell_price`: Double (Snapshot harga jual)
   - `quantity`: Int
   - `subtotal`: Double

6. **`debts` (Buku Kasbon / Hutang Pelanggan)**
   - `id`: Long (PK, AutoGenerate)
   - `customer_id`: Long (FK ke `customers.id`)
   - `transaction_id`: Long? (FK ke `transactions.id`)
   - `amount`: Double (Jumlah pinjaman/bon awal)
   - `remaining_amount`: Double (Sisa yang belum dibayar)
   - `due_date`: Long?
   - `status`: String (`UNPAID`, `PARTIAL`, `PAID`)
   - `notes`: String?
   - `updated_at`: Long

7. **`cash_flows` (Arus Kas Warung / Pengeluaran Operasional)**
   - `id`: Long (PK, AutoGenerate)
   - `date`: Long
   - `type`: String (`EXPENSE` = Listrik/Beli Es/Kulakan, `INCOME` = Modal Tambahan)
   - `category`: String
   - `amount`: Double
   - `description`: String

---

## 3. UI/UX Flow & Screen Sitemap

### 3.1 Alur Navigasi Utama
```
[Splash Screen]
      |
      v
[Main Container (BottomNavigationView)]
  |-- (1) Tab Kasir (POS)
  |       |-- Grid/List Produk & Filter Kategori
  |       |-- Search & Scan Barcode (Camera Floating Icon)
  |       |-- Keranjang Belanja (Bottom Sheet / Cart Summary)
  |       |-- Checkout Dialog (Pilihan Pembayaran: Tunai, QRIS, Kasbon)
  |       `-- Struk Selesai (Print Thermal Bluetooth 58mm / Share WA)
  |
  |-- (2) Tab Produk & Stok
  |       |-- Daftar Katalog Barang (Search, Filter Stok Menipis)
  |       |-- Form Tambah/Ubah Produk
  |       `-- Manajemen Kategori & Satuan
  |
  |-- (3) Tab Catatan Hutang (Buku Bon)
  |       |-- Ringkasan Total Piutang Warung
  |       |-- Daftar Pelanggan Berhutang
  |       `-- Detail Hutang Pelanggan & Form Bayar Cicilan
  |
  `-- (4) Tab Laporan & Pengaturan
          |-- Rekap Penjualan Harian, Mingguan, Bulanan
          |-- Hitung Laba Kotor (Omzet - Modal)
          |-- Catatan Pengeluaran Operasional
          |-- Pengaturan Printer Bluetooth & Toko
          `-- Backup & Restore Database
```

---

## 4. Spesifikasi Desain XML & Theme

### 4.1 Color Palette (`res/values/colors.xml`)
- **Primary:** `#1E88E5` (Biru Terang Kasir Modern) / `#1565C0` (Dark Blue)
- **Secondary / Accent:** `#2E7D32` (Hijau Segar - Cocok untuk Keuangan/Uang)
- **Warning / Alert:** `#F57C00` (Oranye untuk Kasbon/Hutang & Stok Rendah)
- **Error / Void:** `#D32F2F` (Merah untuk Stok Habis / Hapus Transaksi)
- **Background:** `#F8F9FA` (Soft Light Gray)
- **Surface / Card:** `#FFFFFF` (Putih Bersih)
- **Text Primary:** `#212121`
- **Text Secondary:** `#757575`

### 4.2 Standard Views & Custom Drawables
1. **Card Container:**
   - Gunakan `com.google.android.material.card.MaterialCardView` dengan corner radius `12dp` dan elevation `2dp` (`app:cardElevation="2dp"`).
2. **Buttons:**
   - Primary: `MaterialButton` dengan `app:cornerRadius="8dp"`, tinggi standar `48dp` (accessible touch target).
   - Quick Cash Buttons: Button chips bulat untuk pecahan Rp 10.000, 20.000, 50.000, 100.000 di dialog kasir.
3. **RecyclerView:**
   - Menggunakan `GridLayoutManager` (2 kolom) untuk mode Grid Produk di HP atau 4 kolom di Tablet.
   - Menggunakan `LinearLayoutManager` untuk keranjang belanja dan riwayat transaksi.
4. **Bottom Sheet:**
   - `BottomSheetDialogFragment` untuk ringkasan keranjang belanja saat transaksi berlangsung agar kasir tetap bisa melihat katalog sambil mengecek item.

---

## 5. Integrasi Hardware & Ekosistem Warung

1. **Bluetooth Thermal Printer (ESC/POS 58mm):**
   - Format struk ringkas: Nama Warung, Alamat, Tanggal, No. Struk, Daftar Item (Qty x Harga), Total, Bayar, Kembalian, dan Catatan kaki ("Terima Kasih").
2. **Kamera Barcode Scanner:**
   - Menggunakan Google ML Kit Barcode Scanning via CameraX.
   - Menghasilkan beep suara saat barcode sukses terdeteksi dan langsung memasukkan produk ke keranjang.
