use SEMO;

SET @fee_amount_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_schedule_event'
      AND COLUMN_NAME = 'fee_amount'
);

SET @fee_amount_sql = IF(
    @fee_amount_exists = 0,
    'ALTER TABLE club_schedule_event ADD COLUMN fee_amount INT NULL AFTER fee_required',
    'SELECT 1'
);

PREPARE fee_amount_stmt FROM @fee_amount_sql;
EXECUTE fee_amount_stmt;
DEALLOCATE PREPARE fee_amount_stmt;

SET @fee_amount_undecided_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_schedule_event'
      AND COLUMN_NAME = 'fee_amount_undecided'
);

SET @fee_amount_undecided_sql = IF(
    @fee_amount_undecided_exists = 0,
    'ALTER TABLE club_schedule_event ADD COLUMN fee_amount_undecided TINYINT(1) NOT NULL DEFAULT 0 AFTER fee_amount',
    'SELECT 1'
);

PREPARE fee_amount_undecided_stmt FROM @fee_amount_undecided_sql;
EXECUTE fee_amount_undecided_stmt;
DEALLOCATE PREPARE fee_amount_undecided_stmt;
