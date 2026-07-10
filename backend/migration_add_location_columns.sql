-- =========================================
-- MIGRATION: jalankan file ini di phpMyAdmin/HeidiSQL
-- kalau database `db_laporrt` sudah ada sebelumnya (jangan run database.sql lagi
-- karena akan CREATE TABLE dari nol dan datamu bisa error "table already exists").
--
-- File ini menambahkan kolom yang dibutuhkan untuk:
-- 1. Fitur pilih lokasi di peta (latitude & longitude)
-- 2. Memastikan kolom photo_url benar-benar ada (kalau belum, akan ditambahkan)
-- =========================================

USE db_laporrt;

ALTER TABLE reports
    ADD COLUMN IF NOT EXISTS latitude DOUBLE NULL AFTER location,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE NULL AFTER latitude;

ALTER TABLE reports
    MODIFY COLUMN photo_url VARCHAR(255) NULL;
