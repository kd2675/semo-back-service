USE SEMO;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'vote_end_date'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_vote ADD COLUMN vote_end_date DATE NULL AFTER vote_start_date'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE club_schedule_vote
SET vote_end_date = vote_start_date
WHERE vote_end_date IS NULL;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'vote_end_date'
          AND IS_NULLABLE = 'YES'
    ),
    'ALTER TABLE club_schedule_vote MODIFY COLUMN vote_end_date DATE NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'vote_start_time'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_vote ADD COLUMN vote_start_time TIME NULL AFTER vote_end_date'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'vote_end_time'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_vote ADD COLUMN vote_end_time TIME NULL AFTER vote_start_time'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'closed_at'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_vote ADD COLUMN closed_at DATETIME NULL AFTER vote_end_time'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
