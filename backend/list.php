<?php
header('Content-Type: application/json');
error_reporting(0); // biar warning PHP gak ikut kecetak dan merusak format JSON
require_once 'db_config.php';

$result = $conn->query("SELECT * FROM reports ORDER BY report_date DESC");
$reports = [];

while ($row = $result->fetch_assoc()) {
    // pastikan tipe angka tetap angka (bukan string) waktu jadi JSON
    $row['latitude'] = $row['latitude'] !== null ? (float)$row['latitude'] : null;
    $row['longitude'] = $row['longitude'] !== null ? (float)$row['longitude'] : null;
    $reports[] = $row;
}

echo json_encode($reports);
?>
