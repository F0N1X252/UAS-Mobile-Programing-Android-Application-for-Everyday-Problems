# 📱 LaporRT - Aplikasi Pelaporan Masalah Lingkungan

> **Android Application for Everyday Problems**  
> Proyek Ujian Akhir Semester - *Mobile Programming (PG119)*  
> **Fakultas Teknologi Informasi, Universitas Budi Luhur**

<hr>

## 📌 Daftar Isi
* [a. Deskripsi Aplikasi](#a-deskripsi-aplikasi)
* [b. Fitur Utama](#b-daftar-fitur)
* [c. Struktur Activity](#c-daftar-activity)
* [d. Alur Intent](#d-daftar-intent)
* [e. Komponen UI / Widget](#e-daftar-widget)
* [f. Ekosistem Library](#f-daftar-library)
* [g. Desain Database & ERD](#g-database-erd)
* [h. Dokumentasi REST API](#h-rest-api-list)
* [🛠️ Panduan Instalasi & Pengujian](#-panduan-instalasi--pengujian)

<hr>

## a. Deskripsi Aplikasi

*   **Nama Aplikasi:** LaporRT
*   **Latar Belakang:** Alur pelaporan masalah fasilitas umum di tingkat RT/RW seringkali lambat ditangani karena masih dilakukan secara manual dan tidak terdokumentasi dengan baik. 
*   **Tujuan Solusi:** LaporRT hadir sebagai jembatan komunikasi digital yang cepat, transparan, dan terorganisir antara warga dengan pengurus RT. Warga dapat mengirim laporan infrastruktur yang rusak secara langsung dari ponsel mereka untuk kemudian ditinjau dan diperbarui status penanganannya oleh pengurus RT.

<hr>

## b. Daftar Fitur

*   🔑 **Multi-Role Authentication (Login):** Sistem masuk yang memisahkan hak akses antara **Warga (Resident)** untuk melapor dan **Admin (RT)** untuk memperbarui status penanganan aduan.
*   📋 **RecyclerView Dinamis:** Daftar pengaduan terupdate yang disajikan secara dinamis lengkap dengan gambar bukti fisik dari server.
*   🔍 **Filter Kategori Cepat:** Penyaringan laporan instan menggunakan sistem *Chips* untuk mengelompokkan kategori masalah (Jalan Rusak, Lampu Mati, Sampah).
*   ✍️ **Pembuatan Laporan Terbimbing:** Formulir pelaporan dilengkapi validasi input yang ketat, pengambilan koordinat peta, dan kompresi unggah foto.
*   📍 **Pilih Lokasi Interaktif (OSMDroid):** Peta berbasis OpenStreetMap dengan fungsionalitas pin terpusat dan pencarian alamat otomatis (*Reverse Geocoding*).
*   🗺️ **Detail Laporan & Navigasi Luar:** Informasi rinci laporan beserta foto, serta tombol integrasi implicit intent untuk membuka lokasi aduan di aplikasi peta seperti Google Maps atau Waze.
*   ⚙️ **Manajemen Status & Data (Admin):** Dialog eksklusif bagi Admin untuk memperbarui status (*Baru -> Diproses -> Selesai*) serta fitur penghapusan aduan yang tidak valid.

<hr>

## c. Daftar Activity

Aplikasi ini mengimplementasikan **5 Activity** terpisah untuk mengontrol alur kerja aplikasi secara modular:

1.  **`LoginActivity`**  
    Mengelola autentikasi akun. Memiliki dialog pengaturan IP dinamis tersembunyi (melalui *long-click* tombol masuk) untuk memudahkan koneksi server lokal.
2.  **`MainActivity`**  
    Menampilkan dasbor laporan warga, kontrol filter kategori, dan tombol mengarah ke form laporan baru.
3.  **`FormLaporanActivity`**  
    Mengendalikan input formulir pelaporan, pengunggahan foto ke server, serta penyimpanan data laporan ke API database.
4.  **`LocationPickerActivity`**  
    Menyediakan peta interaktif berbasis OpenStreetMap (OSMDroid) bagi pengguna untuk mencari titik lokasi pengaduan secara visual.
5.  **`ReportDetailActivity`**  
    Menampilkan detail data laporan secara lengkap, rute peta eksternal, aksi ubah status (Admin), dan penghapusan data.

<hr>

## d. Daftar Intent

Aplikasi menerapkan komunikasi data antar-komponen menggunakan dua skema Intent:

### **1. Explicit Intent (Navigasi Internal)**
*   **`LoginActivity` $\rightarrow$ `MainActivity`**  
    Membawa data login sukses ke halaman utama.
*   **`MainActivity` $\rightarrow$ `FormLaporanActivity`**  
    Membuka formulir pengaduan baru melalui penekanan tombol (*FAB*).
*   **`FormLaporanActivity` $\rightarrow$ `LocationPickerActivity`**  
    Membuka peta pencari lokasi. Menggunakan `ActivityResultLauncher` untuk mengembalikan koordinat `latitude`, `longitude`, dan alamat teks kembali ke formulir utama.
*   **`MainActivity` $\rightarrow$ `ReportDetailActivity`**  
    Mengirimkan parameter tambahan berupa `REPORT_ID` yang diklik dari daftar agar halaman detail memuat data yang sesuai dari API.
*   **`FormLaporanActivity` $\rightarrow$ `MainActivity`**  
    Mengarahkan kembali pengguna ke dasbor utama secara otomatis setelah aduan sukses dikirim.

### **2. Implicit Intent (Integrasi Sistem Android)**
*   **Kamera & Galeri Intent:** Membuka kamera bawaan perangkat atau pemilih file galeri guna menangkap gambar bukti laporan aduan.
*   **Intent Navigasi Luar:** Membuka koordinat GPS laporan di aplikasi peta luar (seperti Google Maps atau Waze) menggunakan protokol URI `geo:latitude,longitude?q=...` yang didukung oleh query manifes Package Visibility pada Android 11+.

<hr>

## e. Daftar Widget

Aplikasi memanfaatkan komponen UI **Material Design** berikut untuk menyajikan antarmuka yang modern dan intuitif:

*   `RecyclerView` : Merender daftar kartu laporan secara efisien.
*   `MaterialCardView` : Membuat bayangan (*elevation*) dan sudut tumpul pada kartu laporan aduan.
*   `TextInputEditText` & `TextInputLayout` : Menyediakan kolom input (Email, Password, Deskripsi) dengan validasi error langsung di dalam layout.
*   `AutoCompleteTextView` : Dropdown pilihan kategori pengaduan yang ramah pengguna.
*   `ImageView` : Menampilkan preview foto lokal sebelum diunggah serta menampilkan gambar server hasil unduhan Glide.
*   `ChipGroup` : Tombol filter kategori horizontal yang interaktif.
*   `MapView` (OSMDroid) : Komponen penampil peta OpenStreetMap.
*   `FloatingActionButton` : Tombol bulat melayang untuk memicu proses tambah laporan dan penguncian lokasi GPS.
*   `ProgressBar` : Indikator loading melingkar saat proses komputasi latar belakang berjalan.

<hr>

## f. Daftar Library

Pengembangan aplikasi ini didukung oleh ekosistem library Android berikut:

| Nama Library | Versi & Maven | Alasan Penggunaan |
| :--- | :--- | :--- |
| **Retrofit 2** | `com.squareup.retrofit2:retrofit` | Pustaka utama komunikasi client-server REST API secara asinkron. |
| **Gson** | `com.google.code.gson:gson` | Serialisasi data JSON dari backend PHP menjadi representasi objek Java OOP (*POJO*). |
| **OkHttp Logging Interceptor** | `com.squareup.okhttp3:logging-interceptor` | Memantau seluruh aktivitas request jaringan di Logcat guna pembuktian pengiriman data REST API. |
| **Glide** | `com.github.bumptech.glide:glide` | Pengunduhan gambar dinamis secara asinkron dan manajemen cache memori perangkat. |
| **OSMDroid** | `org.osmdroid:osmdroid-android` | Peta open-source OpenStreetMap interaktif bebas lisensi berbayar. |
| **Play Services Location** | `play-services-location` | Layanan penguncian koordinat posisi GPS pengguna secara akurat (*FusedLocationProvider*). |

<hr>

## g. Database (ERD)

Database menggunakan RDBMS MySQL/MariaDB dengan relasi **Satu-ke-Banyak (One-to-Many Relationship)**:

<hr>

## h. REST API List

> **PENTING:** Seluruh berkas file backend PHP diletakkan **flat / sejajar** langsung di dalam folder root `laporrt/` pada web server Anda (`xampp/htdocs/laporrt/`), bukan di dalam subfolder `api/`.

Klik untuk melihat struktur detail pengoperasian endpoint API di bawah ini:

<details>
<summary>🔑 <b>1. POST login.php</b></summary>

*   **Request Body (JSON):**
    ```json
    {
      "email": "budi@gmail.com",
      "password": "123456"
    }
    ```
*   **Response Sukses (200 OK):**
    ```json
    {
      "success": true,
      "message": "Login Berhasil",
      "token": "token_xxx",
      "user": {
        "id": 1,
        "name": "Budi",
        "email": "budi@gmail.com",
        "role": "resident",
        "phone": "08123456789"
      }
    }
    ```
</details>

<details>
<summary>📋 <b>2. GET list.php</b></summary>

*   **Response Sukses (200 OK):**
    ```json
    [
      {
        "id": 1,
        "user_id": 2,
        "category": "Jalan Rusak",
        "description": "Ada lubang yang cukup dalam di tengah jalan",
        "location": "Jl. Mawar No. 10",
        "latitude": -6.208763,
        "longitude": 106.845599,
        "photo_url": "uploads/report_123.jpg",
        "status": "new",
        "report_date": "2026-07-04 10:00:00"
      }
    ]
    ```
</details>

<details>
<summary>✍️ <b>3. POST create.php</b></summary>

*   **Request Body (JSON):**
    ```json
    {
      "user_id": "2",
      "category": "Jalan Rusak",
      "description": "Jalan berlubang cukup dalam",
      "location": "Jl. Mawar No. 10",
      "latitude": "-6.208763",
      "longitude": "106.845599",
      "photo_url": "uploads/report_123.jpg",
      "status": "new"
    }
    ```
*   **Response Sukses (200 OK):**
    ```json
    {
      "success": true,
      "message": "Report created successfully",
      "id": 5
    }
    ```
</details>

<details>
<summary>🔍 <b>4. GET detail.php?id={id}</b></summary>

*   **Response Sukses (200 OK):**
    ```json
    {
      "id": 1,
      "category": "Jalan Rusak",
      "description": "Ada lubang yang cukup dalam di tengah jalan",
      "location": "Jl. Mawar No. 10",
      "latitude": -6.208763,
      "longitude": 106.845599,
      "photo_url": "uploads/report_123.jpg",
      "status": "new",
      "report_date": "2026-07-04 10:00:00"
    }
    ```
</details>

<details>
<summary>⚙️ <b>5. PUT update_status.php</b></summary>

*   **Request Body (JSON):**
    ```json
    {
      "id": 1,
      "status": "processing"
    }
    ```
*   **Response Sukses (200 OK):**
    ```json
    {
      "success": true,
      "message": "Status updated successfully"
    }
    ```
</details>

<details>
<summary>🗑️ <b>6. DELETE delete.php?id={id}</b></summary>

*   **Response Sukses (200 OK):**
    ```json
    {
      "success": true,
      "message": "Report deleted successfully"
    }
    ```
</details>

<details>
<summary>📸 <b>7. POST upload_photo.php</b></summary>

*   **Request (Multipart Form-Data):**  
    *   Key: `photo` (File Gambar: jpg/png/webp, maks 5MB)
*   **Response Sukses (200 OK):**
    ```json
    {
      "success": true,
      "message": "Foto berhasil diunggah",
      "photo_url": "uploads/report_1720000000_a1b2c3d4.jpg"
    }
    ```
</details>

<hr>

## 🛠️ Panduan Instalasi & Pengujian

### **A. Sisi Server (Backend & Database)**
1. Pindahkan folder `laporrt/` berisi file PHP (`db_config.php`, `login.php`, `list.php`, dll.) ke dalam direktori server Anda (contoh: `C:/xampp/htdocs/laporrt/`).
2. Import file database SQL (struktur database `db_laporrt`) melalui menu phpMyAdmin.
3. Pastikan Anda telah menjalankan perintah SQL migrasi penambahan kolom koordinat GPS pada tabel `reports`:
   ```sql
   ALTER TABLE reports 
   ADD COLUMN latitude DOUBLE NULL AFTER location, 
   ADD COLUMN longitude DOUBLE NULL AFTER latitude;
