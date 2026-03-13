USE SEMO;

CREATE TABLE IF NOT EXISTS club_notice (
    notice_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    author_club_profile_id BIGINT NOT NULL,
    category_key VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    location_label VARCHAR(200) NULL,
    schedule_at DATETIME NULL,
    schedule_end_at DATETIME NULL,
    pinned TINYINT(1) NOT NULL DEFAULT 0,
    published_at DATETIME NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_club_notice_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_notice_author FOREIGN KEY (author_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_notice'
          AND COLUMN_NAME = 'location_label'
    ),
    'SELECT 1',
    'ALTER TABLE club_notice ADD COLUMN location_label VARCHAR(200) NULL AFTER content'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_notice'
          AND COLUMN_NAME = 'schedule_at'
    ),
    'SELECT 1',
    'ALTER TABLE club_notice ADD COLUMN schedule_at DATETIME NULL AFTER location_label'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_notice'
          AND COLUMN_NAME = 'schedule_end_at'
    ),
    'SELECT 1',
    'ALTER TABLE club_notice ADD COLUMN schedule_end_at DATETIME NULL AFTER schedule_at'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_notice'
          AND INDEX_NAME = 'idx_club_notice_feed'
    ),
    'SELECT 1',
    'CREATE INDEX idx_club_notice_feed ON club_notice (club_id, category_key, deleted, published_at)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_notice'
          AND INDEX_NAME = 'idx_club_notice_pinned'
    ),
    'SELECT 1',
    'CREATE INDEX idx_club_notice_pinned ON club_notice (club_id, pinned, published_at)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_notice'
          AND INDEX_NAME = 'idx_club_notice_schedule'
    ),
    'SELECT 1',
    'CREATE INDEX idx_club_notice_schedule ON club_notice (club_id, schedule_at, deleted)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
