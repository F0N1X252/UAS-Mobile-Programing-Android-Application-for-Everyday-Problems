CREATE DATABASE IF NOT EXISTS db_laporrt;
USE db_laporrt;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('resident', 'admin') DEFAULT 'resident',
    phone VARCHAR(20)
);

CREATE TABLE reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    category VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(255) NOT NULL,
    latitude DOUBLE NULL,
    longitude DOUBLE NULL,
    photo_url VARCHAR(255),
    status ENUM('new', 'processing', 'completed') DEFAULT 'new',
    report_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Sample Data
INSERT INTO users (name, email, password, role, phone) VALUES
('Warga Test', 'warga@test.com', '123456', 'resident', '08123456789'),
('Admin RT', 'admin@test.com', '123456', 'admin', '08987654321');
