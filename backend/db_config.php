<?php
// =========================================
// PENTING: file ini WAJIB ada di folder yang sama dengan login.php, list.php,
// create.php, dll (root folder "laporrt" di server kalian).
// Sebelumnya file ini cuma ada di dalam folder api/, padahal login.php di root
// butuh require_once 'db_config.php' -- itu salah satu penyebab config keliru.
// =========================================

$host = "localhost";
$user = "root";
$pass = "";
$db   = "db_laporrt";

// Folder tempat menyimpan file foto yang diupload warga
if (!defined('UPLOAD_DIR')) {
    define('UPLOAD_DIR', __DIR__ . '/uploads/');
}
if (!defined('UPLOAD_URL_PATH')) {
    define('UPLOAD_URL_PATH', 'uploads/'); // path relatif
}

$conn = new mysqli($host, $user, $pass, $db);

if ($conn->connect_error) {
    header('Content-Type: application/json');
    die(json_encode(["success" => false, "message" => "Database connection failed: " . $conn->connect_error]));
}
?>
