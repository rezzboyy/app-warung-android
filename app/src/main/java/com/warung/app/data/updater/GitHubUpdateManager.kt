package com.warung.app.data.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tagName: String,
    val releaseTitle: String,
    val changelog: String,
    val apkDownloadUrl: String,
    val apkSize: Long
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object GitHubUpdateManager {
    private const val TAG = "GitHubUpdateManager"
    const val GITHUB_OWNER = "rezzboyy"
    const val GITHUB_REPO = "app-warung-android"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    /**
     * Memeriksa rilis terbaru melalui GitHub Releases API
     */
    suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(API_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "AppWarung-Android-OTA")
            }

            val responseCode = conn.responseCode
            if (responseCode == 404) {
                // Belum ada rilis yang dipublikasikan
                return@withContext UpdateCheckResult.UpToDate
            }
            if (responseCode != 200) {
                return@withContext UpdateCheckResult.Error("HTTP $responseCode saat memeriksa pembaruan")
            }

            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)

            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", "Pembaruan App Warung").ifEmpty { "App Warung $tagName" }
            val changelog = json.optString("body", "Peningkatan performa, fitur kasir terbaru, dan stabilitas aplikasi.")

            // Temukan aset APK
            val assetsArray = json.optJSONArray("assets")
            var apkUrl: String? = null
            var apkSize: Long = 0L

            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl.isNullOrEmpty()) {
                return@withContext UpdateCheckResult.UpToDate
            }

            // Bandingkan versi: remoteTag vs currentVersion
            if (isRemoteVersionNewer(remoteTag = tagName, currentVersion = currentVersionName)) {
                UpdateCheckResult.UpdateAvailable(
                    UpdateInfo(
                        tagName = tagName,
                        releaseTitle = releaseTitle,
                        changelog = changelog,
                        apkDownloadUrl = apkUrl,
                        apkSize = apkSize
                    )
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update", e)
            UpdateCheckResult.Error(e.message ?: "Gagal terhubung ke server pembaruan")
        }
    }

    /**
     * Membandingkan semantic version (misal "v1.0.1" > "1.0.0")
     */
    fun isRemoteVersionNewer(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxParts = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxParts) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    /**
     * Mengunduh berkas APK dengan progress real-time dan langsung memicu PackageInstaller sistem Android
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val destinationFile = File(context.cacheDir, "app_warung_update.apk")
            if (destinationFile.exists()) destinationFile.delete()

            // Menangani HTTP redirect (GitHub Releases redirect ke AWS S3)
            var currentUrl = downloadUrl
            var connection: HttpURLConnection
            var redirectCount = 0

            while (true) {
                val urlObj = URL(currentUrl)
                connection = (urlObj.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "AppWarung-Android-OTA")
                }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == 307 || status == 308
                ) {
                    currentUrl = connection.getHeaderField("Location") ?: break
                    redirectCount++
                    if (redirectCount > 5) break
                } else {
                    break
                }
            }

            val totalBytes = connection.contentLength.toLong()
            var bytesRead = 0L

            connection.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        if (totalBytes > 0) {
                            val progress = bytesRead.toFloat() / totalBytes.toFloat()
                            withContext(Dispatchers.Main) {
                                onProgress(progress.coerceIn(0f, 1f))
                            }
                        }
                    }
                    output.flush()
                }
            }

            // Luncurkan package installer di main thread
            withContext(Dispatchers.Main) {
                installApkFile(context, destinationFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK", e)
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Gagal mengunduh berkas pembaruan")
            }
        }
    }

    /**
     * Memanggil native package installer sistem Android
     */
    fun installApkFile(context: Context, file: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                }
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching package installer", e)
        }
    }
}
