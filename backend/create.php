<?php
header('Content-Type: application/json');

// --- UNTUK DEBUGGING (SANGAT PENTING) ---
// Jika nanti semua fitur sudah berjalan lancar, Anda bisa mengubahnya kembali ke error_reporting(0);
error_reporting(E_ALL);
ini_set('display_errors', 1);
// ----------------------------------------

require_once 'db_config.php';

// Mendapatkan data body raw JSON dari Retrofit Android
$data = json_decode(file_get_contents("php://input"), true);

$user_id = $data['user_id'] ?? 0;
$category = $data['category'] ?? '';
$description = $data['description'] ?? '';
$location = $data['location'] ?? '';
$latitude = isset($data['latitude']) && $data['latitude'] !== '' ? (float)$data['latitude'] : null;
$longitude = isset($data['longitude']) && $data['longitude'] !== '' ? (float)$data['longitude'] : null;
$photo_url = $data['photo_url'] ?? null; // Membaca data path foto yang telah diupload
$status = $data['status'] ?? 'new';

// Validasi kolom wajib
if (empty($category) || empty($description) || empty($location)) {
    echo json_encode(["success" => false, "message" => "Required fields missing"]);
    exit;
}

// Konversi string kosong menjadi NULL agar data di database tetap bersih dan konsisten
if ($photo_url === '') {
    $photo_url = null;
}

// Memasukkan data ke dalam tabel database reports
$stmt = $conn->prepare(
    "INSERT INTO reports (user_id, category, description, location, latitude, longitude, photo_url, status)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
);

// PROTEKSI SANGAT PENTING (Mencegah Crash Fatal):
// Jika tabel di MySQL belum dibuat, atau ada kolom yang typo/tidak cocok, 
// block ini akan mengembalikan JSON error yang sangat jelas ke Android, bukan membuat PHP crash.
if (!$stmt) {
    echo json_encode([
        "success" => false,
        "message" => "Gagal menyiapkan query database (SQL Prepare failed). Silakan cek apakah tabel 'reports' atau nama kolomnya di MySQL sudah dibuat dengan benar. Error: " . $conn->error
    ]);
    exit;
}

$stmt->bind_param(
    "isssddss",
    $user_id,
    $category,
    $description,
    $location,
    $latitude,
    $longitude,
    $photo_url,
    $status
);

if ($stmt->execute()) {
    echo json_encode([
        "success" => true,
        "message" => "Report created successfully",
        "id" => $stmt->insert_id
    ]);
} else {
    echo json_encode(["success" => false, "message" => "Failed to create report: " . $conn->error]);
}
?>