<?php
/**
 * PENTING: File ini dibuat SELF-CONTAINED (berdiri sendiri)
 * Tidak butuh db_config.php agar tidak terjadi error "Undefined constant"
 */
header('Content-Type: application/json');
error_reporting(E_ALL);
ini_set('display_errors', 1);

// Konfigurasi Folder Langsung di sini (Hardcoded untuk keamanan)
$upload_dir = __DIR__ . '/uploads/';
$upload_url = 'uploads/';

// Cek folder uploads
if (!is_dir($upload_dir)) {
    mkdir($upload_dir, 0755, true);
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !isset($_FILES['photo'])) {
    echo json_encode(["success" => false, "message" => "Tidak ada file foto yang dikirim"]);
    exit;
}

$file = $_FILES['photo'];

// Validasi dasar error unggah
if ($file['error'] !== 0) {
    echo json_encode(["success" => false, "message" => "Upload error code: " . $file['error']]);
    exit;
}

$allowedTypes = ['image/jpeg', 'image/png', 'image/jpg', 'image/webp'];
$mimeType = $file['type']; // Fallback paling aman

// Cek mime type asli jika memungkinkan
if (function_exists('finfo_open') && defined('FILEINFO_MIME_TYPE')) {
    $finfo = finfo_open(FILEINFO_MIME_TYPE);
    $mimeType = finfo_file($finfo, $file['tmp_name']);
    finfo_close($finfo);
}

if (!in_array($mimeType, $allowedTypes)) {
    echo json_encode(["success" => false, "message" => "Format file harus JPG, PNG, atau WEBP. Mime: " . $mimeType]);
    exit;
}

$ext = pathinfo($file['name'], PATHINFO_EXTENSION);
if (empty($ext)) {
    $ext = 'jpg';
}

$fileName = 'report_' . time() . '_' . uniqid() . '.' . $ext;
$destination = $upload_dir . $fileName;

if (move_uploaded_file($file['tmp_name'], $destination)) {
    echo json_encode([
        "success" => true,
        "message" => "Foto berhasil diunggah",
        "photo_url" => $upload_url . $fileName
    ]);
} else {
    echo json_encode(["success" => false, "message" => "Gagal menyimpan file ke folder " . $upload_dir]);
}
?>