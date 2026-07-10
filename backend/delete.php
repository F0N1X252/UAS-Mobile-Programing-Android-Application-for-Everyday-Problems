<?php
header('Content-Type: application/json');
error_reporting(0);
require_once 'db_config.php';

$id = $_GET['id'] ?? 0;

if (empty($id)) {
    echo json_encode(["success" => false, "message" => "ID required"]);
    exit;
}

$stmt = $conn->prepare("DELETE FROM reports WHERE id = ?");
$stmt->bind_param("i", $id);

if ($stmt->execute()) {
    echo json_encode(["success" => true, "message" => "Report deleted successfully"]);
} else {
    echo json_encode(["success" => false, "message" => "Failed to delete report: " . $conn->error]);
}
?>
