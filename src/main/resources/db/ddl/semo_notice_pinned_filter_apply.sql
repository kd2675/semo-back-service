use SEMO;

SET @has_idx_club_notice_pinned := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_notice'
      AND INDEX_NAME = 'idx_club_notice_pinned'
);

SET @drop_idx_club_notice_pinned_sql := IF(
    @has_idx_club_notice_pinned > 0,
    'DROP INDEX idx_club_notice_pinned ON club_notice',
    'SELECT 1'
);

PREPARE stmt FROM @drop_idx_club_notice_pinned_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_club_notice_pinned_after := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_notice'
      AND INDEX_NAME = 'idx_club_notice_pinned'
);

SET @create_idx_club_notice_pinned_sql := IF(
    @has_idx_club_notice_pinned_after = 0,
    'CREATE INDEX idx_club_notice_pinned ON club_notice (club_id, deleted, pinned, published_at, notice_id)',
    'SELECT 1'
);

PREPARE stmt FROM @create_idx_club_notice_pinned_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
