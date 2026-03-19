use SEMO;

SET @has_sort_order := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'feature_activation'
      AND COLUMN_NAME = 'sort_order'
);

SET @ddl_sql := IF(
    @has_sort_order = 0,
    'ALTER TABLE feature_activation ADD COLUMN sort_order INT NOT NULL DEFAULT 0 AFTER enabled',
    'SELECT 1'
);

PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE feature_activation fa
JOIN feature_catalog fc
  ON fc.feature_key = fa.feature_key
SET fa.sort_order = CASE
    WHEN fa.enabled = 1 THEN fc.sort_order
    ELSE 1000 + fc.sort_order
END
WHERE fa.sort_order = 0;
