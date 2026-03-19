USE SEMO;

SET @has_idx_club_notice_feed := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_notice'
      AND INDEX_NAME = 'idx_club_notice_feed'
);

SET @drop_idx_club_notice_feed_sql := IF(
    @has_idx_club_notice_feed > 0,
    'DROP INDEX idx_club_notice_feed ON club_notice',
    'SELECT 1'
);

PREPARE stmt FROM @drop_idx_club_notice_feed_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_notice_category_key := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_notice'
      AND COLUMN_NAME = 'category_key'
);

SET @drop_notice_category_key_sql := IF(
    @has_notice_category_key > 0,
    'ALTER TABLE club_notice DROP COLUMN category_key',
    'SELECT 1'
);

PREPARE stmt FROM @drop_notice_category_key_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_club_notice_feed_after := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_notice'
      AND INDEX_NAME = 'idx_club_notice_feed'
);

SET @create_idx_club_notice_feed_sql := IF(
    @has_idx_club_notice_feed_after = 0,
    'CREATE INDEX idx_club_notice_feed ON club_notice (club_id, deleted, published_at)',
    'SELECT 1'
);

PREPARE stmt FROM @create_idx_club_notice_feed_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP TABLE IF EXISTS club_notice_category_setting;
DROP TABLE IF EXISTS notice_category_catalog;
