USE SEMO;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_event'
          AND COLUMN_NAME = 'participation_enabled'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_event ADD COLUMN participation_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER attendee_limit'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
