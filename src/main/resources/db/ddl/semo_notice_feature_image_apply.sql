USE SEMO;

SET @has_notice_image_file_name := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_notice'
      AND COLUMN_NAME = 'image_file_name'
);

SET @notice_image_sql := IF(
    @has_notice_image_file_name = 0,
    'ALTER TABLE club_notice ADD COLUMN image_file_name VARCHAR(255) NULL AFTER content',
    'SELECT 1'
);

PREPARE notice_image_stmt FROM @notice_image_sql;
EXECUTE notice_image_stmt;
DEALLOCATE PREPARE notice_image_stmt;

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
    'NOTICE',
    '공지',
    '모임 공지를 작성, 관리, 공유합니다.',
    'campaign',
    1,
    30,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'NOTICE'
);
