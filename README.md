# 📄 Foxit Rider — PDF Editor Profesional

![Build Status](https://github.com/YOUR_USERNAME/FoxitRider/actions/workflows/build.yml/badge.svg)
![Min SDK](https://img.shields.io/badge/minSdk-24-blue)
![Target SDK](https://img.shields.io/badge/targetSdk-34-green)

Foxit Rider adalah aplikasi Android PDF editor lengkap dengan fitur-fitur profesional yang dapat diakses **gratis** dengan menonton iklan singkat.

---

## 🚀 Fitur Utama

| Fitur | Keterangan | Status |
|-------|------------|--------|
| 📂 Buka PDF | Buka file PDF dari penyimpanan | ✅ Gratis |
| ✏️ Edit PDF | Anotasi, highlight, gambar, teks | ✅ Gratis |
| 🔗 Gabung PDF | Satukan beberapa PDF | 🎬 Pro (iklan) |
| ✂️ Pisah PDF | Pisahkan halaman tertentu | 🎬 Pro (iklan) |
| 📦 Kompres PDF | Kecilkan ukuran file | 🎬 Pro (iklan) |
| 💧 Watermark | Tambah tanda air | 🎬 Pro (iklan) |
| 🔒 Enkripsi | Tambah kata sandi | 🎬 Pro (iklan) |
| 🔓 Dekripsi | Hapus proteksi | 🎬 Pro (iklan) |
| 🔄 Konversi | Ubah format file | 🎬 Pro (iklan) |
| ✍️ Tanda Tangan | Tanda tangan digital | 🎬 Pro (iklan) |
| 📊 Nomor Halaman | Tambah nomor otomatis | 🎬 Pro (iklan) |

> **🎬 Pro (iklan)** = Gratis dengan menonton iklan 30 detik. Akses berlaku 24 jam.

---

## 💰 Model Monetisasi (AdMob)

- **Banner Ad** — Kecil di atas layar, tidak mengganggu
- **Rewarded Ad** — Tonton iklan → buka fitur Pro gratis
- **Interstitial Ad** — Muncul saat beralih ke mode Edit

**AdMob App ID:** `ca-app-pub-4744122948371705~2731100577`  
**Ad Unit ID:** `ca-app-pub-4744122948371705/5604675924`

---

## 🔨 Cara Build

### Prasyarat
- **Android Studio** Hedgehog (2023.1.1) atau lebih baru
- **JDK 17**
- **Android SDK** API 34

### Build via Android Studio
```
1. Clone repo ini
2. Buka Android Studio → Open → pilih folder FoxitRider
3. Tunggu Gradle sync selesai
4. Klik Build → Build Bundle(s)/APK(s) → Build APK(s)
5. APK tersedia di: app/build/outputs/apk/debug/app-debug.apk
```

### Build via Terminal / Command Line
```bash
# Clone project
git clone https://github.com/YOUR_USERNAME/FoxitRider.git
cd FoxitRider

# Build debug APK
./gradlew assembleDebug

# APK output:
# app/build/outputs/apk/debug/app-debug.apk
```

### Build Release (perlu keystore)
```bash
# Generate keystore (sekali saja)
keytool -genkey -v -keystore foxitrider.jks \
    -alias foxitrider -keyalg RSA -keysize 2048 -validity 10000

# Build release
./gradlew assembleRelease
```

---

## 🤖 Build Otomatis via GitHub Actions

Project ini sudah dikonfigurasi untuk build otomatis di GitHub Actions.

### Langkah Setup:
```
1. Push project ke GitHub
2. GitHub Actions akan otomatis build APK setiap push
3. Download APK dari tab "Actions" → pilih workflow run → "Artifacts"
```

### Cara Download APK dari GitHub Actions:
```
1. Buka repository di GitHub
2. Klik tab "Actions"
3. Klik workflow run terbaru
4. Scroll ke bawah ke bagian "Artifacts"
5. Download "FoxitRider-Debug-APK"
```

### Release dengan Tag:
```bash
# Buat tag versi baru
git tag v1.0.0
git push origin v1.0.0

# GitHub Actions akan otomatis:
# - Build APK
# - Buat GitHub Release
# - Upload APK ke Release
```

---

## 📁 Struktur Project

```
FoxitRider/
├── app/
│   ├── src/main/
│   │   ├── java/com/foxitrider/
│   │   │   ├── ads/
│   │   │   │   └── AdManager.kt          ← Manajemen semua iklan
│   │   │   ├── ui/
│   │   │   │   ├── activities/
│   │   │   │   │   ├── SplashActivity.kt
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── PdfViewerActivity.kt
│   │   │   │   │   ├── PdfEditorActivity.kt
│   │   │   │   │   ├── ProFeaturesActivity.kt
│   │   │   │   │   └── FileManagerActivity.kt
│   │   │   │   ├── fragments/
│   │   │   │   │   ├── HomeFragment.kt
│   │   │   │   │   ├── RecentFragment.kt
│   │   │   │   │   ├── ToolsFragment.kt
│   │   │   │   │   └── SettingsFragment.kt
│   │   │   │   └── adapters/
│   │   │   │       └── FileAdapter.kt
│   │   │   └── utils/
│   │   │       ├── PdfUtils.kt            ← Semua operasi PDF
│   │   │       └── ProFeatureManager.kt   ← Manajemen unlock fitur
│   │   └── res/
│   │       ├── layout/                    ← Semua layout XML
│   │       ├── drawable/                  ← Icon & drawable
│   │       ├── values/                    ← Colors, strings, themes
│   │       └── mipmap-*/                  ← Launcher icons
│   └── build.gradle
├── .github/workflows/
│   └── build.yml                          ← GitHub Actions CI/CD
├── build.gradle
├── settings.gradle
└── gradlew
```

---

## 🛠️ Teknologi

- **Bahasa:** Kotlin
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **PDF Library:** AndroidPdfViewer + PdfBox-Android
- **Ads:** Google AdMob SDK 23.0.0
- **UI:** Material Design 3
- **Architecture:** MVVM + Coroutines

---

## 📝 Catatan Penting

1. **Ganti AdMob ID** — Untuk production, pastikan menggunakan ID iklan yang benar
2. **Test Mode** — Saat development, emulator secara otomatis mendapat iklan test
3. **Perizinan Penyimpanan** — Aplikasi memerlukan izin untuk mengakses file PDF
4. **AdMob Policy** — Pastikan konten aplikasi sesuai kebijakan AdMob

---

## 📞 Kontak

- **Nama Aplikasi:** Foxit Rider
- **Package:** `com.foxitrider`
- **Version:** 1.0.0
