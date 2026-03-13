USE SEMO;

CREATE TABLE IF NOT EXISTS club_schedule_event (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    author_club_profile_id BIGINT NOT NULL,
    linked_notice_id BIGINT NULL,
    category_key VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NULL,
    location_label VARCHAR(200) NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NULL,
    attendee_limit INT NULL,
    visibility_status VARCHAR(20) NOT NULL DEFAULT 'CLUB',
    event_status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_club_schedule_event_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_schedule_event_author FOREIGN KEY (author_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_schedule_event_notice FOREIGN KEY (linked_notice_id) REFERENCES club_notice(notice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_event'
          AND COLUMN_NAME = 'linked_notice_id'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_event ADD COLUMN linked_notice_id BIGINT NULL AFTER author_club_profile_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_event'
          AND INDEX_NAME = 'idx_club_schedule_event_club_start'
    ),
    'SELECT 1',
    'CREATE INDEX idx_club_schedule_event_club_start ON club_schedule_event (club_id, start_at)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_event'
          AND INDEX_NAME = 'idx_club_schedule_event_status'
    ),
    'SELECT 1',
    'CREATE INDEX idx_club_schedule_event_status ON club_schedule_event (club_id, event_status, start_at)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
