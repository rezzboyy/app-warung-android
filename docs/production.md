# Production Readiness & Release Guide (App Warung)

Dokumen ini memuat panduan persiapan rilis produksi, optimasi performa, konfigurasi rilis, dan kepatuhan standar Google Play Store untuk **App Warung** (Android Native Kotlin + XML).

---

## 1. Konfigurasi Build & Versi

### 1.1 Spesifikasi SDK
- **`compileSdk`**: `34` (atau `35` sesuai standar rilis Play Store terbaru)
- **`minSdk`**: `24` (Android 7.0 Nougat) — memastikan kompatibilitas luas dengan perangkat HP murah dan mesin POS berbasis Android.
- **`targetSdk`**: `34` (atau `35`)

### 1.2 Skema Versi
- **`versionCode`**: Nilai integer berurutan (`1`, `2`, `3`, ...) untuk setiap rilis.
- **`versionName`**: Format Semantic Versioning `vMAJOR.MINOR.PATCH` (contoh: `v1.0.0`).

---

## 2. Keamanan & Penandatanganan Aplikasi (App Signing)

### 2.1 File Keystore
- Jangan pernah menyimpan keystore produksi dan password langsung di dalam repository Git.
- Gunakan file `keystore.properties` lokal yang dimasukkan ke dalam `.gitignore`:

```properties
# keystore.properties (JANGAN DI-COMMIT)
KEYSTORE_PATH=../keystore/app_warung_release.jks
KEYSTORE_PASSWORD=super_secret_keystore_pass
KEY_ALIAS=appwarung
KEY_PASSWORD=super_secret_key_pass
```

### 2.2 Konfigurasi `build.gradle.kts`
```kotlin
android {
    signingConfigs {
        create("release") {
            val props = Properties().apply {
                val propFile = rootProject.file("keystore.properties")
                if (propFile.exists()) load(propFile.inputStream())
            }
            storeFile = file(props.getProperty("KEYSTORE_PATH") ?: "dummy.jks")
            storePassword = props.getProperty("KEYSTORE_PASSWORD")
            keyAlias = props.getProperty("KEY_ALIAS")
            keyPassword = props.getProperty("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

## 3. Konfigurasi ProGuard / R8 (`proguard-rules.pro`)

Pastikan library Room, Hilt, dan Coroutines tidak terpotong oleh R8:

```proguard
# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Hilt / Dagger
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keep class * implements io.github.classgraph.ClassGraph

# Hardware ESC/POS Printer & Barcode Scanning
-keep class com.google.mlkit.vision.barcode.** { *; }
-keepattributes *Annotation*
```

---

## 4. Manajemen Izin (Permissions & Privacy)

Aplikasi warung beroperasi offline, namun memerlukan izin perangkat keras tertentu:

| Izin | Keperluan | Tingkat Izin |
|---|---|---|
| `android.permission.CAMERA` | Scan barcode produk saat transaksi | Runtime Permission |
| `android.permission.BLUETOOTH` | Hubungkan printer kasir (Android < 12) | Normal |
| `android.permission.BLUETOOTH_CONNECT` | Cetak struk kasir via Bluetooth (Android 12+) | Runtime Permission |
| `android.permission.BLUETOOTH_SCAN` | Cari printer thermal terdekat (Android 12+) | Runtime (`usesPermissionFlags="neverForLocation"`) |

> **Catatan Kepatuhan Play Store:** Deklarasikan bahwa Bluetooth hanya digunakan untuk periferal printer thermal dan tidak mengumpulkan data lokasi pengguna (`neverForLocation`).

---

## 5. Keamanan Data & Pemulihan (Disaster Recovery)

1. **Integritas Transaksi:**
   - Semua operasi multi-tabel (misal: Transaksi Penjualan + Pengurangan Stok + Catatan Kasbon) wajib di dalam transaksi atomik Room Database.
2. **Fitur Ekspor & Cadangan Database Lokal:**
   - Pengguna warung dapat membuat file backup `.warungdb` (file zip terenkripsi berisi file `.db` SQLite).
   - Simpan file cadangan menggunakan Android Storage Access Framework (SAF) ke folder `Documents` atau Google Drive pengguna.
3. **Migrasi Database Aman:**
   - Setiap perubahan skema tabel di Room harus menyertakan skrip `Migration` yang telah diuji (hindari penggunaan `fallbackToDestructiveMigration()` di mode produksi karena dapat menghapus seluruh riwayat penjualan warung).

---

## 6. Checklist Sebelum Rilis (Pre-Release Checklist)

- [ ] **Ukuran Aplikasi:** Ukuran download APK/AAB di bawah 15 MB (manfaatkan WebP untuk gambar dan VectorDrawable untuk ikon).
- [ ] **Mode Rilis Bersih:** `isMinifyEnabled = true` dan `isShrinkResources = true` aktif.
- [ ] **Log Dinonaktifkan:** Logging (seperti `Timber` atau `Log.d`) dimatikan untuk build rilis.
- [ ] **Uji Coba Offline 100%:** Aplikasi telah diuji dalam Airplane Mode untuk memastikan transaksi, pencarian produk, dan cetak struk tetap berfungsi tanpa internet.
- [ ] **Uji Thermal Printer:** Cetak struk telah diuji pada kertas 58mm (karakter 32 kolom) dan 80mm (karakter 48 kolom).
- [ ] **App Bundle:** Generate file `.aab` (bukan `.apk` biasa) untuk diunggah ke Google Play Console.
