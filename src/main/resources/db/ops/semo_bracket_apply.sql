USE SEMO;

-- ============================================================
-- Semo bracket feature apply SQL
-- Order:
-- 1. Run this file
-- 2. Run semo_bracket_seed.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS bracket_record (
    bracket_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    author_club_profile_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary_text VARCHAR(500) NULL,
    bracket_type VARCHAR(30) NOT NULL DEFAULT 'SINGLE_ELIMINATION',
    participant_type VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    source_type VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    source_tournament_record_id BIGINT NULL,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    reviewed_by_club_profile_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    rejection_reason VARCHAR(500) NULL,
    participant_count INT NOT NULL DEFAULT 0,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_bracket_record_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_bracket_record_author FOREIGN KEY (author_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_bracket_record_reviewed_by FOREIGN KEY (reviewed_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_bracket_record_tournament FOREIGN KEY (source_tournament_record_id) REFERENCES tournament_record(tournament_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bracket_participant (
    bracket_participant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bracket_record_id BIGINT NOT NULL,
    seed_number INT NOT NULL,
    club_profile_id BIGINT NULL,
    display_name VARCHAR(100) NOT NULL,
    participant_role VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
    entry_source_type VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    source_tournament_application_id BIGINT NULL,
    guest_entry TINYINT(1) NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_bracket_participant_seed UNIQUE (bracket_record_id, seed_number),
    CONSTRAINT fk_bracket_participant_record FOREIGN KEY (bracket_record_id) REFERENCES bracket_record(bracket_record_id),
    CONSTRAINT fk_bracket_participant_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_bracket_participant_application FOREIGN KEY (source_tournament_application_id) REFERENCES tournament_application(tournament_application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'bracket_record'
              AND index_name = 'idx_bracket_record_approval'
        ),
        'SELECT 1',
        'CREATE INDEX idx_bracket_record_approval ON bracket_record (club_id, deleted, approval_status, create_date)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'bracket_record'
              AND index_name = 'idx_bracket_record_author'
        ),
        'SELECT 1',
        'CREATE INDEX idx_bracket_record_author ON bracket_record (club_id, author_club_profile_id, deleted, create_date)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'bracket_participant'
              AND index_name = 'idx_bracket_participant_record'
        ),
        'SELECT 1',
        'CREATE INDEX idx_bracket_participant_record ON bracket_participant (bracket_record_id, seed_number, bracket_participant_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
