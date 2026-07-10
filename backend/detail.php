<?php
header('Content-Type: application/json');
error_reporting(0);
require_once 'db_config.php';

$id = $_GET['id'] ?? 0;

$stmt = $conn->prepare("SELECT * FROM reports WHERE id = ?");
$stmt->bind_param("i", $id);
$stmt->execute();
$result = $stmt->get_result();

if ($report = $result->fetch_assoc()) {
    $report['latitude'] = $report['latitude'] !== null ? (float)$report['latitude'] : null;
    $report['longitude'] = $report['longitude'] !== null ? (float)$report['longitude'] : null;
    echo json_encode($report);
} else {
    http_response_code(404);
    echo json_encode(["message" => "Report not found"]);
}
?>
