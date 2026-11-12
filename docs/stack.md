# Technology Stack (App Warung)

Dokumen ini memuat spesifikasi lengkap teknologi, pustaka (*libraries*), perkakas (*tooling*), dan versi dependensi yang digunakan dalam pengembangan **App Warung**.

---

## 1. Ringkasan Ekosistem Utama

| Kategori | Teknologi | Versi | Alasan Pemilihan |
|---|---|---|---|
| **Bahasa Pemrograman** | Kotlin | `1.9.24` | Modern, null-safety ketat, ekspresif, dan standar resmi Android. |
| **Java Development Kit** | Microsoft OpenJDK | `17.0.20.1` (LTS) | Standar LTS untuk Gradle 8.x dan Android Gradle Plugin (AGP) 8.x. |
| **Build System** | Gradle + Wrapper | `8.7` | Cepat, efisien, mendukung Kotlin DSL (`build.gradle.kts`). |
| **Android Gradle Plugin** | AGP | `8.4.2` | Kompatibilitas penuh dengan Android 14 (API 34) dan R8 compiler. |
| **UI Framework** | Native XML + ViewBinding | - | Ringan, hemat memori pada HP murah, stabil, tanpa overhead runtime compose. |
| **Design System** | Material Design 3 (MDC) | `1.12.0` | Komponen visual modern, accessible, dan konsisten. |
| **Local Database** | Room Database (SQLite) | `2.6.1` | Abstraksi SQLite yang aman (*compile-time verified SQL*), mendukung reaktif `Flow`. |
| **Asynchronous & Concurrency** | Kotlin Coroutines | `1.8.0` | Eksekusi background I/O database dan hardware non-blocking. |
| **Navigation** | Jetpack Navigation Component | `2.7.7` | Manajemen perpindahan Fragment dan navigasi back-stack terstandar. |
| **Hardware Scanning** | Google ML Kit Barcode | `17.2.0` | Pemindaian barcode produk super cepat berbasis on-device machine learning. |
| **Hardware Cetak Struk** | Bluetooth SPP + ESC/POS | Native API | Standar universal untuk printer thermal kasir 58mm / 80mm. |

---

## 2. Rincian Dependensi Proyek (`app/build.gradle.kts`)

### 2.1 AndroidX & UI Core
- `androidx.core:core-ktx:1.13.1` — Ekstensi Kotlin untuk API inti Android.
- `androidx.appcompat:appcompat:1.6.1` — Kompatibilitas mundur komponen UI.
- `com.google.android.material:material:1.12.0` — Material Components (Toolbar, BottomNavigationView, CardView, Button).
- `androidx.constraintlayout:constraintlayout:2.1.4` — Layout responsif dengan performa tinggi.
- `androidx.recyclerview:recyclerview:1.3.2` — Rendering daftar produk dan struk belanja secara efisien.

### 2.2 Lifecycle, ViewModel, & Coroutines
- `androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0` — Penyimpanan state UI yang bertahan saat rotasi layar.
- `androidx.lifecycle:lifecycle-livedata-ktx:2.7.0` — Observasi data lifecycle-aware untuk UI.
- `androidx.lifecycle:lifecycle-runtime-ktx:2.7.0` — Coroutine lifecycle scope (`lifecycleScope`).
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0` — Dukungan `Dispatchers.Main` dan `Dispatchers.IO`.

### 2.3 Basis Data Lokal (Room)
- `androidx.room:room-runtime:2.6.1` — Mesin utama Room database.
- `androidx.room:room-ktx:2.6.1` — Dukungan Coroutines dan reactive `Flow` untuk query Room.
- `androidx.room:room-compiler:2.6.1` (via `kapt`) — Code generator DAO dan Entity saat kompilasi.

### 2.4 Navigasi Layar
- `androidx.navigation:navigation-fragment-ktx:2.7.7`
- `androidx.navigation:navigation-ui-ktx:2.7.7`

### 2.5 Pengujian (Testing)
- `junit:junit:4.13.2` — Pengujian unit logika bisnis & kalkulasi kasir.
- `androidx.test.ext:junit:1.1.5` & `androidx.test.espresso:espresso-core:3.5.1` — Pengujian instrumen UI Android.

---

## 3. Target Spesifikasi Perangkat

| Parameter | Nilai | Keterangan |
|---|---|---|
| **Min SDK** | `24` | Android 7.0 Nougat (mencakup 95%+ perangkat aktif di pasar warung/UMKM). |
| **Compile SDK** | `34` | Android 14 (kompilasi fitur dan API terbaru). |
| **Target SDK** | `34` | Standar kepatuhan rilis Google Play Store saat ini. |
| **JVM Target** | `17` | Kompilasi bytecode Java 17 teroptimasi. |

---

## 4. Lingkungan Kerja & Tooling (Development Environment)

Pengembangan berjalan penuh tanpa ketergantungan pada GUI Android Studio:

- **Editor & Agen AI:** VS Code + Antigravity (`agy`) CLI.
- **Java Runtime:** Microsoft Build of OpenJDK 17 (`C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot`).
- **Android SDK:** Terpasang di `C:\Android\Sdk` (Platform API 34, Build-Tools 34.0.0, Platform-Tools `adb`).
- **Build CLI:** Gradle Wrapper (`.\gradlew.bat assembleDebug` / `.\gradlew.bat installDebug`).
- **Device Bridge:** `adb` versi `37.0.1` via USB Debugging.

---

## 5. Mengapa Kotlin + XML & Offline-First?

1. **Efisiensi Memori (Low-RAM Devices):**
   - Perangkat kasir warung sering kali berupa smartphone kelas pemula (RAM 2GB – 3GB) atau mesin POS khusus.
   - XML Layouts dengan ViewBinding memiliki footprint memori yang sangat rendah dan startup time yang cepat dibandingkan framework yang membutuhkan runtime berat.
2. **Keandalan Tanpa Internet (Offline-First):**
   - Transaksi kasir tidak boleh terhenti jika sinyal internet mati.
   - Room Database menyimpan seluruh data transaksi, stok barang, dan buku kasbon secara lokal di memori internal perangkat.
3. **Kompatibilitas Hardware:**
   - Komunikasi printer Bluetooth (ESC/POS) dan scanner kamera (ML Kit) berjalan stabil di Android native melalui Java/Kotlin APIs standar.
