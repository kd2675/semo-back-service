USE SEMO;

SET @notice_shared_to_schedule_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_notice'
      AND COLUMN_NAME = 'shared_to_schedule'
);

SET @notice_shared_to_schedule_sql := IF(
    @notice_shared_to_schedule_exists = 0,
    'ALTER TABLE club_notice ADD COLUMN shared_to_schedule TINYINT(1) NOT NULL DEFAULT 0 AFTER schedule_end_at',
    'SELECT 1'
);
PREPARE stmt FROM @notice_shared_to_schedule_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @event_shared_to_notice_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_schedule_event'
      AND COLUMN_NAME = 'shared_to_notice'
);

SET @event_shared_to_notice_sql := IF(
    @event_shared_to_notice_exists = 0,
    'ALTER TABLE club_schedule_event ADD COLUMN shared_to_notice TINYINT(1) NOT NULL DEFAULT 0 AFTER linked_notice_id',
    'SELECT 1'
);
PREPARE stmt FROM @event_shared_to_notice_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @vote_shared_to_notice_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_schedule_vote'
      AND COLUMN_NAME = 'shared_to_notice'
);

SET @vote_shared_to_notice_sql := IF(
    @vote_shared_to_notice_exists = 0,
    'ALTER TABLE club_schedule_vote ADD COLUMN shared_to_notice TINYINT(1) NOT NULL DEFAULT 0 AFTER linked_notice_id',
    'SELECT 1'
);
PREPARE stmt FROM @vote_shared_to_notice_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
