USE SEMO;

SET @event_pinned_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'club_schedule_event'
      AND column_name = 'pinned'
);

SET @event_pinned_sql = IF(
    @event_pinned_exists = 0,
    'ALTER TABLE club_schedule_event ADD COLUMN pinned TINYINT(1) NOT NULL DEFAULT 0 AFTER shared_to_calendar',
    'SELECT 1'
);

PREPARE stmt FROM @event_pinned_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @vote_pinned_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'club_schedule_vote'
      AND column_name = 'pinned'
);

SET @vote_pinned_sql = IF(
    @vote_pinned_exists = 0,
    'ALTER TABLE club_schedule_vote ADD COLUMN pinned TINYINT(1) NOT NULL DEFAULT 0 AFTER shared_to_calendar',
    'SELECT 1'
);

PREPARE stmt FROM @vote_pinned_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
