USE SEMO;

SET @shared_to_schedule_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_schedule_vote'
      AND COLUMN_NAME = 'shared_to_schedule'
);

SET @shared_to_schedule_sql := IF(
    @shared_to_schedule_exists = 0,
    'ALTER TABLE club_schedule_vote ADD COLUMN shared_to_schedule TINYINT(1) NOT NULL DEFAULT 0 AFTER linked_notice_id',
    'SELECT 1'
);
PREPARE stmt FROM @shared_to_schedule_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'POLL',
    '투표',
    '모임 투표를 작성, 공유, 관리합니다.',
    'poll',
    1,
    30,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'POLL'
);
