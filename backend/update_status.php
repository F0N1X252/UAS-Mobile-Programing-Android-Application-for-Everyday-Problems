<?php
header('Content-Type: application/json');
error_reporting(0);
require_once 'db_config.php';

$data = json_decode(file_get_contents("php://input"), true);
$id = $data['id'] ?? 0;
$status = $data['status'] ?? '';

if (empty($id) || empty($status)) {
    echo json_encode(["success" => false, "message" => "ID and status required"]);
    exit;
}

$stmt = $conn->prepare("UPDATE reports SET status = ? WHERE id = ?");
$stmt->bind_param("si", $status, $id);

if ($stmt->execute()) {
    echo json_encode(["success" => true, "message" => "Status updated successfully"]);
} else {
    echo json_encode(["success" => false, "message" => "Failed to update status: " . $conn->error]);
}
?>
