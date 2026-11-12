# =====================================================================
# App Warung - One-Click Auto-Release Script for GitHub Releases (OTA)
# =====================================================================
param(
    [string]$Version = "",
    [string]$Notes = "Pembaruan sistem, peningkatan performa, dan pembaruan fitur kasir App Warung."
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  App Warung Android - GitHub Release & OTA Publisher     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Read app/build.gradle.kts
$gradleFile = Join-Path $scriptDir "app\build.gradle.kts"
$content = Get-Content $gradleFile -Raw

# Extract current versionCode and versionName
if ($content -match 'versionCode\s*=\s*(\d+)') {
    $currentCode = [int]$matches[1]
} else {
    $currentCode = 1
}

if ($content -match 'versionName\s*=\s*"([^"]+)"') {
    $currentName = $matches[1]
} else {
    $currentName = "1.0.0"
}

# Determine new version
if ([string]::IsNullOrWhiteSpace($Version)) {
    # Auto-bump patch version (e.g. 1.0.0 -> 1.0.1)
    $parts = $currentName.Split('.')
    if ($parts.Length -ge 3) {
        $patch = [int]$parts[2] + 1
        $targetVersion = "$($parts[0]).$($parts[1]).$patch"
    } else {
        $targetVersion = "$currentName.1"
    }
} else {
    $targetVersion = $Version.Trim().TrimStart('v').TrimStart('V')
}

$newCode = $currentCode + 1

Write-Host "[1/5] Memperbarui Versi Aplikasi:" -ForegroundColor Yellow
Write-Host "      Version Code: $currentCode -> $newCode" -ForegroundColor Gray
Write-Host "      Version Name: $currentName -> $targetVersion" -ForegroundColor Gray

# Update build.gradle.kts
$updatedContent = $content -replace 'versionCode\s*=\s*\d+', "versionCode = $newCode"
$updatedContent = $updatedContent -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$targetVersion`""
Set-Content -Path $gradleFile -Value $updatedContent -NoNewline

# 2. Build Release APK
Write-Host "`n[2/5] Mengompilasi APK Rilis (assembleRelease)..." -ForegroundColor Yellow
$gradlew = Join-Path $scriptDir "gradlew.bat"
& $gradlew assembleRelease

$apkPath = Join-Path $scriptDir "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host "Error: File app-release.apk tidak ditemukan di $apkPath!" -ForegroundColor Red
    exit 1
}

$apkSizeMb = [math]::Round((Get-Item $apkPath).Length / 1MB, 2)
Write-Host "      APK berhasil dibuat: $apkSizeMb MB" -ForegroundColor Green

# 3. Commit version bump to Git
Write-Host "`n[3/5] Melakukan Commit dan Push Versi ke Git..." -ForegroundColor Yellow
git add -A
git commit -m "chore(release): bump version to v$targetVersion (code $newCode)"
git push origin master

# 4. Publish Release via GitHub CLI
Write-Host "`n[4/5] Mempublikasikan Rilis ke GitHub Releases..." -ForegroundColor Yellow
$tag = "v$targetVersion"
$releaseTitle = "App Warung v$targetVersion"

$releaseNotes = @"
## Pembaruan App Warung Android ($tag)

$Notes

---
### Informasi Berkas:
- **Paket**: com.warung.app
- **Version Code**: $newCode
- **Version Name**: $targetVersion
- **Ukuran Berkas**: $apkSizeMb MB
- **Signature**: Verified SHA-256 (warung_release.jks)

*Unduhan otomatis didukung langsung melalui menu aplikasi pada seluruh perangkat HP pengguna.*
"@

# Create GitHub release and upload APK
& gh release create $tag $apkPath --title $releaseTitle --notes $releaseNotes

Write-Host "`n[5/5] RILIS SUKSES DIPUBLIKASIKAN!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  Tag Versi   : $tag" -ForegroundColor Cyan
Write-Host "  URL Rilis   : https://github.com/rezzboyy/app-warung-android/releases/tag/$tag" -ForegroundColor Cyan
Write-Host "  Status OTA  : Seluruh HP yang terpasang aplikasi akan otomatis mendeteksi update ini saat dibuka!" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Green
