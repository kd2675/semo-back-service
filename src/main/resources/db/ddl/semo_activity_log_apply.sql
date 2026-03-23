-- Semo activity log apply script
-- Created: 2026-03-23
-- Purpose: add append-only club activity log table for admin recent activity

USE SEMO;

CREATE TABLE IF NOT EXISTS club_activity_log (
    club_activity_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    actor_club_member_id BIGINT NULL,
    actor_club_profile_id BIGINT NULL,
    actor_display_name VARCHAR(100) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    detail_text VARCHAR(500) NOT NULL,
    status_code VARCHAR(20) NOT NULL,
    error_message VARCHAR(500) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_club_activity_log_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_activity_log_member FOREIGN KEY (actor_club_member_id) REFERENCES club_member(club_member_id),
    CONSTRAINT fk_club_activity_log_profile FOREIGN KEY (actor_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_idx_club_activity_log_recent := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_activity_log'
      AND INDEX_NAME = 'idx_club_activity_log_recent'
);
SET @create_idx_club_activity_log_recent_sql := IF(
    @has_idx_club_activity_log_recent = 0,
    'CREATE INDEX idx_club_activity_log_recent ON club_activity_log (club_id, create_date, club_activity_log_id)',
    'SELECT 1'
);
PREPARE create_idx_club_activity_log_recent_stmt FROM @create_idx_club_activity_log_recent_sql;
EXECUTE create_idx_club_activity_log_recent_stmt;
DEALLOCATE PREPARE create_idx_club_activity_log_recent_stmt;

SET @has_idx_club_activity_log_status := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_activity_log'
      AND INDEX_NAME = 'idx_club_activity_log_status'
);
SET @create_idx_club_activity_log_status_sql := IF(
    @has_idx_club_activity_log_status = 0,
    'CREATE INDEX idx_club_activity_log_status ON club_activity_log (club_id, status_code, create_date)',
    'SELECT 1'
);
PREPARE create_idx_club_activity_log_status_stmt FROM @create_idx_club_activity_log_status_sql;
EXECUTE create_idx_club_activity_log_status_stmt;
DEALLOCATE PREPARE create_idx_club_activity_log_status_stmt;
