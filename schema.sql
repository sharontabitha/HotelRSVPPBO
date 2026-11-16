-- ==============================================================
-- StayEase Hotel Management System
-- Skema Database Relasional (MySQL / MariaDB)
-- ==============================================================

CREATE DATABASE IF NOT EXISTS stayease
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE stayease;

-- --------------------------------------------------------------
-- 1. Tabel: accounts
-- Menyimpan data akun pengguna (Admin dan Tamu / Guest)
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
    username VARCHAR(30) PRIMARY KEY,
    role VARCHAR(16) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    phone VARCHAR(40) NOT NULL,
    password_salt VARBINARY(64) NOT NULL,
    password_hash VARBINARY(128) NOT NULL,
    CONSTRAINT chk_accounts_role CHECK (role IN ('ADMIN', 'GUEST'))
) ENGINE=InnoDB;

-- --------------------------------------------------------------
-- 2. Tabel: rooms
-- Menyimpan data inventaris kamar hotel (Standard, Deluxe, Suite)
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rooms (
    room_number VARCHAR(30) PRIMARY KEY,
    category VARCHAR(100) NOT NULL,
    floor VARCHAR(80) NOT NULL,
    bed_type VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    base_rate DECIMAL(19, 4) NOT NULL,
    amenities TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    image_data LONGBLOB,
    CONSTRAINT chk_rooms_capacity CHECK (capacity BETWEEN 1 AND 12),
    CONSTRAINT chk_rooms_status CHECK (status IN ('AVAILABLE', 'MAINTENANCE'))
) ENGINE=InnoDB;

-- --------------------------------------------------------------
-- 3. Tabel: reservations
-- Menyimpan riwayat reservasi kamar oleh tamu
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservations (
    id VARCHAR(36) PRIMARY KEY,
    guest_username VARCHAR(30) NOT NULL,
    guest_name VARCHAR(120) NOT NULL,
    phone VARCHAR(40) NOT NULL,
    room_number VARCHAR(30) NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guests INT NOT NULL,
    nightly_rate DECIMAL(19, 4) NOT NULL,
    payment_method VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT chk_reservations_status CHECK (status IN ('CONFIRMED', 'CANCELLED', 'CHECKED_IN', 'CHECKED_OUT')),
    CONSTRAINT fk_reservations_room FOREIGN KEY (room_number)
        REFERENCES rooms(room_number)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_reservations_room_dates (room_number, check_in, check_out, status),
    INDEX idx_reservations_guest (guest_username, check_in)
) ENGINE=InnoDB;

-- --------------------------------------------------------------
-- 4. Tabel: home_content
-- Pengaturan konten dinamis yang ditampilkan di halaman depan
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS home_content (
    id TINYINT PRIMARY KEY,
    hero_title VARCHAR(200) NOT NULL,
    welcome_message TEXT NOT NULL,
    booking_notice TEXT NOT NULL,
    show_room_catalog BOOLEAN NOT NULL,
    show_amenities BOOLEAN NOT NULL,
    show_booking_notice BOOLEAN NOT NULL
) ENGINE=InnoDB;

-- --------------------------------------------------------------
-- 5. Tabel: app_metadata
-- Penanda versi migrasi dan status aplikasi
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_metadata (
    metadata_key VARCHAR(100) PRIMARY KEY,
    metadata_value VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

-- ==============================================================
-- DATA AWAL / SEEDING (Kamar Bawaan & Konten Beranda)
-- ==============================================================

-- Data Awal Kamar Hotel (Sesuai dengan kelas StandardRoom, DeluxeRoom, SuiteRoom)
INSERT IGNORE INTO rooms (room_number, category, floor, bed_type, capacity, base_rate, amenities, status) VALUES
('101', 'Deluxe Heritage Room', 'Lantai 1', 'King Size Royal', 2, 2400000.0000, 'Wi-Fi, sarapan, city view', 'AVAILABLE'),
('102', 'Deluxe Heritage Room', 'Lantai 1', 'Twin Hollywood', 2, 2600000.0000, 'Wi-Fi, sarapan, rain shower', 'AVAILABLE'),
('202', 'Grand Executive Suite', 'Lantai 2', 'King Size Royal', 3, 3100000.0000, 'Balkon, bathtub, sarapan', 'AVAILABLE'),
('303', 'Grand Executive Suite', 'Lantai 3', 'Super King Imperial', 3, 4250000.0000, 'City view, lounge, butler', 'AVAILABLE'),
('403', 'Presidential Royal Penthouse', 'Lantai 4', 'Super King Imperial', 6, 7500000.0000, 'Private balcony, wine cellar, butler 24/7', 'AVAILABLE');

-- Data Awal Pengaturan Konten Beranda
INSERT IGNORE INTO home_content (id, hero_title, welcome_message, booking_notice, show_room_catalog, show_amenities, show_booking_notice) VALUES
(1, 'Liburan Berkesan Dimulai di StayEase', 
 'Nikmati kamar nyaman, lokasi strategis, dan layanan hangat yang membuat setiap perjalanan terasa istimewa.', 
 'Pesan langsung dengan tenang. Detail reservasi tersimpan aman di akun Anda.', 
 1, 1, 1);

-- Penanda Metadata Inisialisasi
INSERT IGNORE INTO app_metadata (metadata_key, metadata_value) VALUES
('initial-data-import-v1', 'completed');
