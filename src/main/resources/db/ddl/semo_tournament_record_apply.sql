-- Semo tournament-record apply script
-- Created: 2026-03-25
-- Purpose: add tournament feature schema for production rollout
-- Notes:
--   1. Run this file before semo_tournament_record_seed.sql
--   2. Safe to run multiple times

USE SEMO;

CREATE TABLE IF NOT EXISTS tournament_record (
    tournament_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    author_club_profile_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary_text VARCHAR(500) NULL,
    detail_text TEXT NULL,
    tournament_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    application_start_at DATETIME NOT NULL,
    application_end_at DATETIME NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    location_label VARCHAR(200) NULL,
    match_format VARCHAR(20) NOT NULL DEFAULT 'SINGLE',
    team_member_limit INT NULL,
    participant_limit INT NULL,
    fee_required TINYINT(1) NOT NULL DEFAULT 0,
    fee_amount INT NULL,
    fee_currency_code VARCHAR(10) NOT NULL DEFAULT 'KRW',
    shared_to_board TINYINT(1) NOT NULL DEFAULT 0,
    shared_to_calendar TINYINT(1) NOT NULL DEFAULT 0,
    pinned TINYINT(1) NOT NULL DEFAULT 0,
    bracket_mode VARCHAR(20) NOT NULL DEFAULT 'RANDOM',
    bracket_confirmed TINYINT(1) NOT NULL DEFAULT 0,
    cancelled_at DATETIME NULL,
    cancel_reason VARCHAR(500) NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_tournament_record_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_tournament_record_author FOREIGN KEY (author_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_idx_tournament_record_home := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tournament_record'
      AND INDEX_NAME = 'idx_tournament_record_home'
);
SET @create_idx_tournament_record_home_sql := IF(
    @has_idx_tournament_record_home = 0,
    'CREATE INDEX idx_tournament_record_home ON tournament_record (club_id, deleted, pinned, start_date, tournament_record_id)',
    'SELECT 1'
);
PREPARE create_idx_tournament_record_home_stmt FROM @create_idx_tournament_record_home_sql;
EXECUTE create_idx_tournament_record_home_stmt;
DEALLOCATE PREPARE create_idx_tournament_record_home_stmt;

SET @has_idx_tournament_record_calendar := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tournament_record'
      AND INDEX_NAME = 'idx_tournament_record_calendar'
);
SET @create_idx_tournament_record_calendar_sql := IF(
    @has_idx_tournament_record_calendar = 0,
    'CREATE INDEX idx_tournament_record_calendar ON tournament_record (club_id, deleted, start_date, end_date)',
    'SELECT 1'
);
PREPARE create_idx_tournament_record_calendar_stmt FROM @create_idx_tournament_record_calendar_sql;
EXECUTE create_idx_tournament_record_calendar_stmt;
DEALLOCATE PREPARE create_idx_tournament_record_calendar_stmt;

CREATE TABLE IF NOT EXISTS tournament_application (
    tournament_application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_record_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    application_status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    application_note VARCHAR(500) NULL,
    reviewed_by_club_profile_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_tournament_application UNIQUE (tournament_record_id, club_profile_id),
    CONSTRAINT fk_tournament_application_tournament FOREIGN KEY (tournament_record_id) REFERENCES tournament_record(tournament_record_id),
    CONSTRAINT fk_tournament_application_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_tournament_application_reviewed_by FOREIGN KEY (reviewed_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_idx_tournament_application_status := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tournament_application'
      AND INDEX_NAME = 'idx_tournament_application_status'
);
SET @create_idx_tournament_application_status_sql := IF(
    @has_idx_tournament_application_status = 0,
    'CREATE INDEX idx_tournament_application_status ON tournament_application (tournament_record_id, application_status, create_date)',
    'SELECT 1'
);
PREPARE create_idx_tournament_application_status_stmt FROM @create_idx_tournament_application_status_sql;
EXECUTE create_idx_tournament_application_status_stmt;
DEALLOCATE PREPARE create_idx_tournament_application_status_stmt;

CREATE TABLE IF NOT EXISTS tournament_entry (
    tournament_entry_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_record_id BIGINT NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    source_application_id BIGINT NULL,
    entry_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    seed_number INT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_tournament_entry_tournament FOREIGN KEY (tournament_record_id) REFERENCES tournament_record(tournament_record_id),
    CONSTRAINT fk_tournament_entry_source_application FOREIGN KEY (source_application_id) REFERENCES tournament_application(tournament_application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_idx_tournament_entry_tournament := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tournament_entry'
      AND INDEX_NAME = 'idx_tournament_entry_tournament'
);
SET @create_idx_tournament_entry_tournament_sql := IF(
    @has_idx_tournament_entry_tournament = 0,
    'CREATE INDEX idx_tournament_entry_tournament ON tournament_entry (tournament_record_id, sort_order, tournament_entry_id)',
    'SELECT 1'
);
PREPARE create_idx_tournament_entry_tournament_stmt FROM @create_idx_tournament_entry_tournament_sql;
EXECUTE create_idx_tournament_entry_tournament_stmt;
DEALLOCATE PREPARE create_idx_tournament_entry_tournament_stmt;

CREATE TABLE IF NOT EXISTS tournament_entry_member (
    tournament_entry_member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_entry_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    member_role VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
    sort_order INT NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_tournament_entry_member UNIQUE (tournament_entry_id, club_profile_id),
    CONSTRAINT fk_tournament_entry_member_entry FOREIGN KEY (tournament_entry_id) REFERENCES tournament_entry(tournament_entry_id),
    CONSTRAINT fk_tournament_entry_member_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_idx_tournament_entry_member_entry := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tournament_entry_member'
      AND INDEX_NAME = 'idx_tournament_entry_member_entry'
);
SET @create_idx_tournament_entry_member_entry_sql := IF(
    @has_idx_tournament_entry_member_entry = 0,
    'CREATE INDEX idx_tournament_entry_member_entry ON tournament_entry_member (tournament_entry_id, sort_order, tournament_entry_member_id)',
    'SELECT 1'
);
PREPARE create_idx_tournament_entry_member_entry_stmt FROM @create_idx_tournament_entry_member_entry_sql;
EXECUTE create_idx_tournament_entry_member_entry_stmt;
DEALLOCATE PREPARE create_idx_tournament_entry_member_entry_stmt;

CREATE TABLE IF NOT EXISTS tournament_round (
    tournament_round_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_record_id BIGINT NOT NULL,
    round_key VARCHAR(40) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    round_type VARCHAR(20) NOT NULL DEFAULT 'BRACKET',
    sort_order INT NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_tournament_round UNIQUE (tournament_record_id, round_key),
    CONSTRAINT fk_tournament_round_tournament FOREIGN KEY (tournament_record_id) REFERENCES tournament_record(tournament_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_idx_tournament_round_tournament := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tournament_round'
      AND INDEX_NAME = 'idx_tournament_round_tournament'
);
SET @create_idx_tournament_round_tournament_sql := IF(
    @has_idx_tournament_round_tournament = 0,
    'CREATE INDEX idx_tournament_round_tournament ON tournament_round (tournament_record_id, sort_order, tournament_round_id)',
    'SELECT 1'
);
PREPARE create_idx_tournament_round_tournament_stmt FROM @create_idx_tournament_round_tournament_sql;
EXECUTE create_idx_tournament_round_tournament_stmt;
DEALLOCATE PREPARE create_idx_tournament_round_tournament_stmt;

CREATE TABLE IF NOT EXISTS tournament_match (
    tournament_match_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_record_id BIGINT NOT NULL,
    tournament_round_id BIGINT NOT NULL,
    match_status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    title VARCHAR(150) NULL,
    scheduled_at DATETIME NULL,
    ended_at DATETIME NULL,
    location_label VARCHAR(200) NULL,
    winner_entry_id BIGINT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_tournament_match_tournament FOREIGN KEY (tournament_record_id) REFERENCES tournament_record(tournament_record_id),
    CONSTRAINT fk_tournament_match_round FOREIGN KEY (tournament_round_id) REFERENCES tournament_round(tournament_round_id),
    CONSTRAINT fk_tournament_match_winner FOREIGN KEY (winner_entry_id) REFERENCES tournament_entry(tournament_entry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_idx_tournament_match_round := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tournament_match'
      AND INDEX_NAME = 'idx_tournament_match_round'
);
SET @create_idx_tournament_match_round_sql := IF(
    @has_idx_tournament_match_round = 0,
    'CREATE INDEX idx_tournament_match_round ON tournament_match (tournament_round_id, sort_order, tournament_match_id)',
    'SELECT 1'
);
PREPARE create_idx_tournament_match_round_stmt FROM @create_idx_tournament_match_round_sql;
EXECUTE create_idx_tournament_match_round_stmt;
DEALLOCATE PREPARE create_idx_tournament_match_round_stmt;

CREATE TABLE IF NOT EXISTS tournament_match_side (
    tournament_match_side_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_match_id BIGINT NOT NULL,
    side_no INT NOT NULL,
    tournament_entry_id BIGINT NULL,
    score_summary VARCHAR(120) NULL,
    result_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_tournament_match_side UNIQUE (tournament_match_id, side_no),
    CONSTRAINT fk_tournament_match_side_match FOREIGN KEY (tournament_match_id) REFERENCES tournament_match(tournament_match_id),
    CONSTRAINT fk_tournament_match_side_entry FOREIGN KEY (tournament_entry_id) REFERENCES tournament_entry(tournament_entry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_idx_tournament_match_side_match := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tournament_match_side'
      AND INDEX_NAME = 'idx_tournament_match_side_match'
);
SET @create_idx_tournament_match_side_match_sql := IF(
    @has_idx_tournament_match_side_match = 0,
    'CREATE INDEX idx_tournament_match_side_match ON tournament_match_side (tournament_match_id, side_no)',
    'SELECT 1'
);
PREPARE create_idx_tournament_match_side_match_stmt FROM @create_idx_tournament_match_side_match_sql;
EXECUTE create_idx_tournament_match_side_match_stmt;
DEALLOCATE PREPARE create_idx_tournament_match_side_match_stmt;
