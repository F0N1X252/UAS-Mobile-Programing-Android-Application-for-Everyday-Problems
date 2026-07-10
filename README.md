# LaporRT - Aplikasi Pelaporan Masalah Lingkungan

## a. Deskripsi Aplikasi
**Nama Aplikasi:** LaporRT
**Latar Belakang:** Seringkali masalah infrastruktur atau lingkungan di tingkat RT/RW lambat ditangani karena alur pelaporan yang masih manual. LaporRT hadir untuk memudahkan warga melaporkan masalah secara langsung ke pengurus RT/Kelurahan.
**Tujuan Solusi:** Memberikan platform digital bagi warga untuk melaporkan masalah infrastruktur (jalan rusak, lampu mati, dll) dengan cepat, transparan, dan terorganisir.

## b. Daftar Fitur
1. **Login Multi-Role:** Warga dapat login untuk melapor, Admin dapat login untuk mengelola laporan.
2. **List Laporan Dinamis:** Menampilkan semua laporan dengan status terbaru menggunakan RecyclerView.
3. **Filter Kategori:** Memudahkan pencarian laporan berdasarkan kategori (Jalan Rusak, Sampah, dll).
4. **Buat Laporan:** Form input laporan dengan validasi data yang ketat, lengkap dengan upload foto dan pilih lokasi di peta.
5. **Pilih Lokasi di Peta:** peta interaktif, pin diam di tengah, alamat otomatis terisi (reverse geocoding).
6. **Detail Laporan:** Informasi lengkap laporan, termasuk foto dan tombol buka lokasi di Google Maps.
7. **Update Status (Admin):** Admin dapat mengubah status laporan (Baru -> Diproses -> Selesai).
8. **Hapus Laporan:** Fitur untuk menghapus laporan yang tidak valid atau sudah tidak relevan.

## c. Daftar Activity
1. **LoginActivity:** Menangani proses otentikasi pengguna.
2. **MainActivity:** Menampilkan daftar laporan warga dan filter kategori.
3. **FormLaporanActivity:** Form untuk membuat laporan baru (kategori, deskripsi, lokasi, foto).
4. **LocationPickerActivity:** Peta interaktif untuk memilih titik lokasi laporan.
5. **ReportDetailActivity:** Menampilkan detail laporan dan menyediakan opsi Update/Delete/Buka di Maps.

## d. Daftar Intent
1. **Intent (Explicit):** Dari `LoginActivity` ke `MainActivity` setelah login sukses.
2. **Intent (Explicit):** Dari `MainActivity` ke `FormLaporanActivity` untuk membuat laporan.
3. **Intent (Explicit):** Dari `FormLaporanActivity` ke `LocationPickerActivity` untuk memilih lokasi, hasil dikembalikan lewat `ActivityResultLauncher` (alamat, latitude, longitude).
4. **Intent (Explicit):** Dari `MainActivity` ke `ReportDetailActivity` dengan membawa data `REPORT_ID`.
5. **Intent (Implicit):** Dari `ReportDetailActivity` membuka aplikasi peta (Google Maps) lewat URI `geo:` untuk menampilkan lokasi laporan.
6. **Intent (Explicit):** Kembali dari `FormLaporanActivity` ke `MainActivity` setelah submit.

## e. Daftar Widget
- `RecyclerView`: Menampilkan list laporan secara dinamis.
- `EditText` / `TextInputEditText`: Input email, password, deskripsi, lokasi.
- `Button`: Tombol login, submit, update status, hapus, pilih lokasi, buka maps.
- `AutoCompleteTextView`: Dropdown pilihan kategori.
- `ImageView`: Menampilkan foto bukti masalah & preview upload.
- `ChipGroup`: Filter kategori laporan.
- `SupportMapFragment` (openstreetmap): Peta interaktif pemilihan lokasi.
- `FloatingActionButton`: Tombol tambah laporan & gunakan lokasi saat ini.
- `ProgressBar`: Indikator loading saat memuat data/lokasi.

## f. Daftar Library
- **Retrofit 2**: Library untuk komunikasi dengan REST API secara efisien.
- **Gson**: Mengonversi format JSON dari API menjadi objek Java.
- **OkHttp Logging Interceptor**: Digunakan untuk debugging request/response jaringan (lihat di Logcat tag `okhttp.OkHttpClient`, bukti integrasi REST API saat demo).
- **Glide**: Library untuk loading dan caching gambar dari URL.
- **ViewBinding**: Memudahkan interaksi dengan UI komponen secara type-safe.
- **Play Services Maps**: Menampilkan peta interaktif Google Maps.
- **Play Services Location (FusedLocationProviderClient)**: Mengambil lokasi GPS pengguna saat ini.

## g. Database (ERD)
### Tabel `users`
- `id` (INT, PK, AI)
- `name` (VARCHAR)
- `email` (VARCHAR, Unique)
- `password` (VARCHAR)
- `role` (ENUM: 'resident', 'admin')
- `phone` (VARCHAR)

### Tabel `reports`
- `id` (INT, PK, AI)
- `user_id` (INT, FK -> users.id)
- `category` (VARCHAR)
- `description` (TEXT)
- `location` (VARCHAR) — alamat teks hasil reverse geocoding
- `latitude` (DOUBLE, nullable) — **kolom baru**
- `longitude` (DOUBLE, nullable) — **kolom baru**
- `photo_url` (VARCHAR, nullable) — path relatif file foto di server
- `status` (ENUM: 'new', 'processing', 'completed')
- `report_date` (TIMESTAMP)

```
users (1) ----< (N) reports
```

## h. REST API List
**Base URL (contoh):** `http://<IP_SERVER>/laporrt/`
> Semua file backend ditaruh **flat/rata** langsung di folder `laporrt/` pada web server kalian (htdocs), BUKAN di dalam subfolder `api/` atau `laporan/` — ini menyesuaikan `ApiClient.java` yang memanggil endpoint langsung dari root base URL.

1. **POST `login.php`**
   - Request: `{"email": "budi@gmail.com", "password": "123456"}`
   - Response sukses: `{"success": true, "message": "Login Berhasil", "token": "token_xxx", "user": {"id":1,"name":"Budi","email":"...","role":"resident","phone":"..."}}`
   - Response gagal: `{"success": false, "message": "Email atau password salah"}`

2. **GET `list.php`**
   - Response: `[{"id":1,"user_id":2,"category":"Jalan Rusak","description":"...","location":"...","latitude":-6.208763,"longitude":106.845599,"photo_url":"uploads/report_123.jpg","status":"new","report_date":"2026-07-04 10:00:00"}, ...]`

3. **POST `create.php`**
   - Request: `{"user_id":"2","category":"Jalan Rusak","description":"Jalan berlubang cukup dalam","location":"Jl. Mawar No. 10","latitude":"-6.208763","longitude":"106.845599","photo_url":"uploads/report_123.jpg","status":"new"}`
   - Response: `{"success": true, "message": "Report created successfully", "id": 5}`

4. **GET `detail.php?id={id}`**
   - Response: `{"id":1,"category":"Jalan Rusak","description":"...","location":"...","latitude":-6.208763,"longitude":106.845599,"photo_url":"uploads/report_123.jpg","status":"new","report_date":"..."}`

5. **PUT `update_status.php`**
   - Request: `{"id": 1, "status": "processing"}`
   - Response: `{"success": true, "message": "Status updated successfully"}`

6. **DELETE `delete.php?id={id}`**
   - Response: `{"success": true, "message": "Report deleted successfully"}`

7. **POST `upload_photo.php`** *(baru, sebelumnya tidak ada)*
   - Request: `multipart/form-data`, field name `photo` berisi file gambar (jpg/png/webp, maks 5MB)
   - Response sukses: `{"success": true, "message": "Foto berhasil diunggah", "photo_url": "uploads/report_1720000000_a1b2c3d4.jpg"}`
   - Response gagal: `{"success": false, "message": "Format file harus JPG, PNG, atau WEBP"}`
