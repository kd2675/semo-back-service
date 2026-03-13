-- SEMO incremental migration
-- Purpose:
-- 1) apply feature catalog / activation tables
-- 2) apply attendance feature tables
-- 3) absorb older experimental table names if they exist
-- Safe to run multiple times.

USE SEMO;

-- ============================================================
-- Rename older experimental tables to the current names when needed.
-- ============================================================
SET @has_old_club_feature := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = 'SEMO'
      AND table_name = 'club_feature'
);
SET @has_feature_activation := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = 'SEMO'
      AND table_name = 'feature_activation'
);
SET @sql := IF(
    @has_old_club_feature = 1 AND @has_feature_activation = 0,
    'RENAME TABLE club_feature TO feature_activation',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_old_attendance_session := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = 'SEMO'
      AND table_name = 'club_attendance_session'
);
SET @has_attendance_session := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = 'SEMO'
      AND table_name = 'attendance_session'
);
SET @sql := IF(
    @has_old_attendance_session = 1 AND @has_attendance_session = 0,
    'RENAME TABLE club_attendance_session TO attendance_session',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_old_attendance_checkin := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = 'SEMO'
      AND table_name = 'club_attendance_checkin'
);
SET @has_attendance_checkin := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = 'SEMO'
      AND table_name = 'attendance_checkin'
);
SET @sql := IF(
    @has_old_attendance_checkin = 1 AND @has_attendance_checkin = 0,
    'RENAME TABLE club_attendance_checkin TO attendance_checkin',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- Rename older experimental PK columns if they survived a previous apply.
-- ============================================================
SET @has_old_feature_activation_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = 'SEMO'
      AND table_name = 'feature_activation'
      AND column_name = 'club_feature_id'
);
SET @sql := IF(
    @has_old_feature_activation_id = 1,
    'ALTER TABLE feature_activation CHANGE COLUMN club_feature_id feature_activation_id BIGINT NOT NULL AUTO_INCREMENT',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_old_attendance_checkin_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = 'SEMO'
      AND table_name = 'attendance_checkin'
      AND column_name = 'club_attendance_checkin_id'
);
SET @sql := IF(
    @has_old_attendance_checkin_id = 1,
    'ALTER TABLE attendance_checkin CHANGE COLUMN club_attendance_checkin_id attendance_checkin_id BIGINT NOT NULL AUTO_INCREMENT',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- Feature catalog / activation
-- ============================================================
CREATE TABLE IF NOT EXISTS feature_catalog (
    feature_key VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    icon_name VARCHAR(50) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS feature_activation (
    feature_activation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    feature_key VARCHAR(50) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    enabled_by_club_profile_id BIGINT NULL,
    enabled_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_feature_activation_key UNIQUE (club_id, feature_key),
    CONSTRAINT fk_feature_activation_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_feature_activation_catalog FOREIGN KEY (feature_key) REFERENCES feature_catalog(feature_key),
    CONSTRAINT fk_feature_activation_enabled_by FOREIGN KEY (enabled_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_feature_activation_enabled
    ON feature_activation (club_id, enabled, feature_key);

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'ATTENDANCE',
    'Attendance Check',
    'Check in members and manage attendance sessions.',
    'fact_check',
    1,
    10,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'ATTENDANCE'
);

-- ============================================================
-- Attendance feature tables
-- ============================================================
CREATE TABLE IF NOT EXISTS attendance_session (
    attendance_session_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    created_by_club_profile_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    attendance_date DATE NOT NULL,
    open_at DATETIME NOT NULL,
    close_at DATETIME NULL,
    session_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_attendance_session_day UNIQUE (club_id, attendance_date),
    CONSTRAINT fk_attendance_session_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_attendance_session_created_by FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_attendance_session_status
    ON attendance_session (club_id, session_status, attendance_date);

CREATE TABLE IF NOT EXISTS attendance_checkin (
    attendance_checkin_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attendance_session_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    status_code VARCHAR(20) NOT NULL DEFAULT 'CHECKED_IN',
    checked_in_at DATETIME NOT NULL,
    note VARCHAR(255) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_attendance_checkin UNIQUE (attendance_session_id, club_profile_id),
    CONSTRAINT fk_attendance_checkin_session FOREIGN KEY (attendance_session_id) REFERENCES attendance_session(attendance_session_id),
    CONSTRAINT fk_attendance_checkin_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_attendance_checkin_profile
    ON attendance_checkin (club_profile_id, checked_in_at);
