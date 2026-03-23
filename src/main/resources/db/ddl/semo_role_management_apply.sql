-- Semo role management apply script
-- Created: 2026-03-23
-- Purpose: add role-management permission schema for top-level features and child permissions
-- Notes:
--   1. Run this file before semo_role_management_seed.sql
--   2. Existing notice/schedule/poll policy tables are intentionally left in place for safe rollout

USE SEMO;

SET @has_navigation_scope := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'feature_catalog'
      AND COLUMN_NAME = 'navigation_scope'
);
SET @alter_feature_catalog_sql := IF(
    @has_navigation_scope = 0,
    'ALTER TABLE feature_catalog ADD COLUMN navigation_scope VARCHAR(20) NOT NULL DEFAULT ''USER_AND_ADMIN'' AFTER icon_name',
    'SELECT 1'
);
PREPARE alter_feature_catalog_stmt FROM @alter_feature_catalog_sql;
EXECUTE alter_feature_catalog_stmt;
DEALLOCATE PREPARE alter_feature_catalog_stmt;

CREATE TABLE IF NOT EXISTS feature_permission_catalog (
    permission_key VARCHAR(80) NOT NULL PRIMARY KEY,
    feature_key VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    ownership_scope VARCHAR(20) NOT NULL DEFAULT 'CLUB',
    active TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_feature_permission_catalog_feature
        FOREIGN KEY (feature_key) REFERENCES feature_catalog(feature_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_idx_feature_permission_catalog_feature := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'feature_permission_catalog'
      AND INDEX_NAME = 'idx_feature_permission_catalog_feature'
);
SET @create_idx_feature_permission_catalog_feature_sql := IF(
    @has_idx_feature_permission_catalog_feature = 0,
    'CREATE INDEX idx_feature_permission_catalog_feature ON feature_permission_catalog (feature_key, active, sort_order)',
    'SELECT 1'
);
PREPARE create_idx_feature_permission_catalog_feature_stmt FROM @create_idx_feature_permission_catalog_feature_sql;
EXECUTE create_idx_feature_permission_catalog_feature_stmt;
DEALLOCATE PREPARE create_idx_feature_permission_catalog_feature_stmt;

CREATE TABLE IF NOT EXISTS club_position (
    club_position_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    position_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    icon_name VARCHAR(50) NULL,
    color_hex VARCHAR(20) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_by_club_profile_id BIGINT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_position_code UNIQUE (club_id, position_code),
    CONSTRAINT fk_club_position_club
        FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_position_created_by
        FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_idx_club_position_club_active := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_position'
      AND INDEX_NAME = 'idx_club_position_club_active'
);
SET @create_idx_club_position_club_active_sql := IF(
    @has_idx_club_position_club_active = 0,
    'CREATE INDEX idx_club_position_club_active ON club_position (club_id, active, display_name)',
    'SELECT 1'
);
PREPARE create_idx_club_position_club_active_stmt FROM @create_idx_club_position_club_active_sql;
EXECUTE create_idx_club_position_club_active_stmt;
DEALLOCATE PREPARE create_idx_club_position_club_active_stmt;

CREATE TABLE IF NOT EXISTS club_position_permission (
    club_position_permission_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_position_id BIGINT NOT NULL,
    permission_key VARCHAR(80) NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_position_permission UNIQUE (club_position_id, permission_key),
    CONSTRAINT fk_club_position_permission_position
        FOREIGN KEY (club_position_id) REFERENCES club_position(club_position_id),
    CONSTRAINT fk_club_position_permission_catalog
        FOREIGN KEY (permission_key) REFERENCES feature_permission_catalog(permission_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_idx_club_position_permission_position := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_position_permission'
      AND INDEX_NAME = 'idx_club_position_permission_position'
);
SET @create_idx_club_position_permission_position_sql := IF(
    @has_idx_club_position_permission_position = 0,
    'CREATE INDEX idx_club_position_permission_position ON club_position_permission (club_position_id, permission_key)',
    'SELECT 1'
);
PREPARE create_idx_club_position_permission_position_stmt FROM @create_idx_club_position_permission_position_sql;
EXECUTE create_idx_club_position_permission_position_stmt;
DEALLOCATE PREPARE create_idx_club_position_permission_position_stmt;

CREATE TABLE IF NOT EXISTS club_member_position (
    club_member_position_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_member_id BIGINT NOT NULL,
    club_position_id BIGINT NOT NULL,
    assigned_by_club_profile_id BIGINT NULL,
    assigned_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_member_position UNIQUE (club_member_id, club_position_id),
    CONSTRAINT fk_club_member_position_member
        FOREIGN KEY (club_member_id) REFERENCES club_member(club_member_id),
    CONSTRAINT fk_club_member_position_position
        FOREIGN KEY (club_position_id) REFERENCES club_position(club_position_id),
    CONSTRAINT fk_club_member_position_assigned_by
        FOREIGN KEY (assigned_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_idx_club_member_position_member := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_member_position'
      AND INDEX_NAME = 'idx_club_member_position_member'
);
SET @create_idx_club_member_position_member_sql := IF(
    @has_idx_club_member_position_member = 0,
    'CREATE INDEX idx_club_member_position_member ON club_member_position (club_member_id, club_position_id)',
    'SELECT 1'
);
PREPARE create_idx_club_member_position_member_stmt FROM @create_idx_club_member_position_member_sql;
EXECUTE create_idx_club_member_position_member_stmt;
DEALLOCATE PREPARE create_idx_club_member_position_member_stmt;
