<?php
header('Content-Type: application/json');
require_once 'db_config.php';

$response = [
    "server_status" => "online",
    "php_version" => phpversion(),
    "database_connection" => "failed"
];

if (!$conn->connect_error) {
    $response["database_connection"] = "success";
    $result = $conn->query("SELECT COUNT(*) as count FROM users");
    if ($result) {
        $row = $result->fetch_assoc();
        $response["user_count"] = $row['count'];
    }
}

echo json_encode($response);
?>
