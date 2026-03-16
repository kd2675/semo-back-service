USE SEMO;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'vote_start_date'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_vote ADD COLUMN vote_start_date DATE NULL AFTER title'
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
          AND COLUMN_NAME = 'vote_end_date'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_vote ADD COLUMN vote_end_date DATE NULL AFTER vote_start_date'
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
          AND COLUMN_NAME = 'vote_date'
    ),
    'UPDATE club_schedule_vote SET vote_start_date = COALESCE(vote_start_date, vote_date) WHERE vote_start_date IS NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE club_schedule_vote
SET vote_start_date = DATE(create_date)
WHERE vote_start_date IS NULL;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'vote_start_date'
          AND IS_NULLABLE = 'YES'
    ),
    'ALTER TABLE club_schedule_vote MODIFY COLUMN vote_start_date DATE NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
