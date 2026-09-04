<div align="center">

# 🛡️ DataSaver Shield

**Firewall Lokal & Pembatas Kuota Internet Android Tanpa Root**

[![Android Build & Package APK](https://github.com/berusigma/Data-Saver/actions/workflows/build.yml/badge.svg)](https://github.com/berusigma/Data-Saver/actions/workflows/build.yml)
![Android SDK](https://img.shields.io/badge/Android-SDK%2024%2B-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose)
![License](https://img.shields.io/badge/License-MIT-blue)
![No Root Required](https://img.shields.io/badge/Root-Not%20Required-success)

*Hemat kuota data internet Anda dan amankan privasi perangkat dengan memblokir akses internet aplikasi latar belakang secara selektif tanpa akses Root.*

---

</div>

## 📌 Tentang DataSaver Shield

**DataSaver Shield** adalah aplikasi Android berbasis **Jetpack Compose** & **Kotlin** yang berfungsi sebagai Firewall lokal dan pengendali penggunaan data internet. Aplikasi ini memungkinkan pengguna untuk memblokir atau mengizinkan akses internet (baik data seluler maupun Wi-Fi) untuk setiap aplikasi yang terpasang di HP secara individu.

Dengan memanfaatkan API bawaan **Android VpnService**, DataSaver Shield bekerja sebagai *packet sink / blackhole* lokal langsung di dalam HP Anda. **Tidak ada server VPN pihak ketiga, tidak ada data yang dikirim keluar, dan 100% aman offline.**

---

## ✨ Fitur Utama

- 🛡️ **Master Shield Toggle**: Aktifkan atau nonaktifkan perlindungan firewall global hanya dengan satu sentuhan.
- 🚫 **Pembatasan Internet Per Aplikasi (Tanpa Root)**: Blokir koneksi latar belakang aplikasi boros kuota seperti media sosial, game, atau iklan tanpa memerlukan akses Root.
- ⚡ **Aksi Massal (Batch Control)**:
  - **Blokir Semua Aplikasi Pengguna**: Matikan akses internet semua aplikasi terinstal sekaligus.
  - **Izinkan Semua**: Buka kembali akses internet untuk seluruh aplikasi.
- 🔍 **Pencarian & Filter Pintar**:
  - Filter aplikasi berdasarkan status: **Semua**, **Diblokir**, **Diizinkan**.
  - Filter kategori: **Aplikasi Pengguna (User Apps)** vs **Aplikasi Sistem (System Apps)**.
  - Pencarian instan berdasarkan nama aplikasi atau nama paket (*package name*).
- 📊 **Statistik Real-time**: Pantau jumlah aplikasi terblokir, aplikasi diizinkan, dan jumlah paket internet yang berhasil dihentikan.
- 🔒 **100% Menjaga Privasi & Hemat Baterai**: Berjalan sepenuhnya di perangkat lokal. Zero server logs, zero analytics tracking.
- 🎨 **Antarmuka Modern Jetpack Compose**: Tampilan Cyber-Neon Dark Theme dengan Material Design 3 yang futuristik dan responsif.
- 💾 **Penyimpanan Permanen (Room Database)**: Seluruh aturan blokir tersimpan aman di database lokal Room.

---

## 🛠️ Teknologi & Arsitektur

DataSaver Shield dibangun dengan standar pengembangan Android modern:

- **Bahasa**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) dengan [Material Design 3](https://m3.material.io/)
- **Arsitektur**: MVVM (Model-View-ViewModel) + StateFlow & Coroutines
- **Layanan Jaringan**: Android `VpnService` (Local Loopback TUN Interface)
- **Database Lokal**: [Room Database](https://developer.android.com/training/data-storage/room)
- **Build System**: Gradle dengan Kotlin DSL (`build.gradle.kts`)
- **CI/CD**: GitHub Actions (Otomatis kompilasi APK)

---

## 🚀 Cara Kerja (VpnService Blackhole)

```
[ Aplikasi Terpasang ] ---> ( Internet Request )
                                  │
                                  ▼
                   [ DataSaver Shield VpnService ]
                                  │
                 ┌────────────────┴────────────────┐
                 ▼                                 ▼
         [ Status: Diblokir ]            [ Status: Diizinkan ]
                 │                                 │
                 ▼                                 ▼
      ( Packet Dropped / Sink )            ( Akses Internet Normal )
```

1. Ketika Master Shield diaktifkan, DataSaver Shield mendaftarkan interface VPN lokal (`10.120.0.1`).
2. Hanya aplikasi yang ditandai **Diblokir** yang akan diarahkan ke interface VPN lokal ini.
3. Paket data dari aplikasi terblokir akan langsung dibuang (*packet drop*), mencegah penggunaan kuota data.
4. Aplikasi yang **Diizinkan** langsung terhubung ke internet tanpa melalui VPN, menjaga kecepatan internet tetap maksimal.

---

## ⚙️ Kompilasi & Build

### 1. Build Otomatis via GitHub Actions (Rekomendasi)
Setiap kali ada commit/push ke repository ini atau trigger manual via tab **Actions**, GitHub Actions akan mengompilasi kode dan menghasilkan file **APK Debug** yang siap didownload pada bagian **Artifacts**.

### 2. Build Lokal dengan Gradle / Android Studio

#### Prasyarat:
- JDK 17 atau lebih baru
- Android Studio Ladybug / ME / Iguana (atau yang lebih baru)
- Android SDK 36 (minSdk 24)

#### Langkah-langkah:
```bash
# Clone repository
git clone https://github.com/berusigma/Data-Saver.git
cd Data-Saver

# Berikan izin eksekusi gradlew (Linux/macOS)
chmod +x gradlew

# Build APK Debug
./gradlew assembleDebug
```
File APK hasil kompilasi dapat ditemukan di:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🤖 GitHub Actions Workflow (CI/CD)

Repository ini telah dilengkapi dengan workflow CI/CD otomatis di `.github/workflows/build.yml`:

- **Triggers**: Push ke branch `main`, Pull Request, atau manual via `workflow_dispatch`.
- **Proses**:
  1. Checkout kode sumber.
  2. Setup Java JDK 17 (Temurin).
  3. Cache & Setup Gradle Environment.
  4. Kompilasi aplikasi (`./gradlew assembleDebug`).
  5. Mengunggah hasil build sebagai **Artifact APK** (Dapat didownload langsung).
  6. Membuat **GitHub Release** otomatis saat push Tag `v*` (contoh: `git tag v1.0.0 && git push origin v1.0.0`).

---

## 📄 Lisensi

Proyek ini dilisensikan di bawah [MIT License](LICENSE). Bebas digunakan, dimodifikasi, dan didistribusikan.

---

<div align="center">
Dibuat dengan ❤️ untuk menghemat kuota internet dan menjaga privasi pengguna Android.
</div>
