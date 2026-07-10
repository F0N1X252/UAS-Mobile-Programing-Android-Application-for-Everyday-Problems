<?php
header('Content-Type: application/json');
error_reporting(0); // Prevent PHP warnings from breaking JSON output
require_once 'db_config.php';

// Support both JSON body and standard POST fields
$data = json_decode(file_get_contents("php://input"), true);
$email = $data['email'] ?? ($_POST['email'] ?? '');
$password = $data['password'] ?? ($_POST['password'] ?? '');

if (empty($email) || empty($password)) {
    echo json_encode(["success" => false, "message" => "Email dan password diperlukan"]);
    exit;
}

$stmt = $conn->prepare("SELECT id, name, email, role, phone FROM users WHERE email = ? AND password = ?");
if (!$stmt) {
    echo json_encode(["success" => false, "message" => "Database error: " . $conn->error]);
    exit;
}

$stmt->bind_param("ss", $email, $password);
$stmt->execute();
$result = $stmt->get_result();

if ($user = $result->fetch_assoc()) {
    echo json_encode([
        "success" => true,
        "message" => "Login Berhasil",
        "token" => "token_" . bin2hex(random_bytes(16)),
        "user" => $user
    ]);
} else {
    echo json_encode(["success" => false, "message" => "Email atau password salah"]);
}
?>
