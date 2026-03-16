USE SEMO;

CREATE TABLE IF NOT EXISTS dashboard_widget_catalog (
    widget_key VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    icon_name VARCHAR(50) NOT NULL,
    required_feature_key VARCHAR(50) NULL,
    default_visibility_scope VARCHAR(20) NOT NULL DEFAULT 'USER_HOME',
    default_column_span INT NOT NULL DEFAULT 1,
    default_row_span INT NOT NULL DEFAULT 1,
    default_sort_order INT NOT NULL DEFAULT 0,
    active TINYINT(1) NOT NULL DEFAULT 1,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'dashboard_widget_catalog'
          AND COLUMN_NAME = 'required_feature_key'
    ),
    'SELECT 1',
    'ALTER TABLE dashboard_widget_catalog ADD COLUMN required_feature_key VARCHAR(50) NULL AFTER icon_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'dashboard_widget_catalog'
          AND COLUMN_NAME = 'default_visibility_scope'
    ),
    'SELECT 1',
    'ALTER TABLE dashboard_widget_catalog ADD COLUMN default_visibility_scope VARCHAR(20) NOT NULL DEFAULT ''USER_HOME'' AFTER required_feature_key'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'dashboard_widget_catalog'
          AND COLUMN_NAME = 'default_column_span'
    ),
    'SELECT 1',
    'ALTER TABLE dashboard_widget_catalog ADD COLUMN default_column_span INT NOT NULL DEFAULT 1 AFTER default_visibility_scope'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'dashboard_widget_catalog'
          AND COLUMN_NAME = 'default_row_span'
    ),
    'SELECT 1',
    'ALTER TABLE dashboard_widget_catalog ADD COLUMN default_row_span INT NOT NULL DEFAULT 1 AFTER default_column_span'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'dashboard_widget_catalog'
          AND COLUMN_NAME = 'default_sort_order'
    ),
    'SELECT 1',
    'ALTER TABLE dashboard_widget_catalog ADD COLUMN default_sort_order INT NOT NULL DEFAULT 0 AFTER default_row_span'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE dashboard_widget_catalog
SET default_visibility_scope = 'USER_HOME'
WHERE UPPER(default_visibility_scope) IN ('USER', 'ADMIN');

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'dashboard_widget_catalog'
          AND INDEX_NAME = 'idx_dashboard_widget_catalog_scope'
    ),
    'SELECT 1',
    'CREATE INDEX idx_dashboard_widget_catalog_scope ON dashboard_widget_catalog (default_visibility_scope, active, default_sort_order)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'dashboard_widget_catalog'
          AND CONSTRAINT_NAME = 'fk_dashboard_widget_required_feature'
    ),
    'SELECT 1',
    'ALTER TABLE dashboard_widget_catalog ADD CONSTRAINT fk_dashboard_widget_required_feature FOREIGN KEY (required_feature_key) REFERENCES feature_catalog(feature_key)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS club_dashboard_widget (
    club_dashboard_widget_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    widget_key VARCHAR(50) NOT NULL,
    title_override VARCHAR(100) NULL,
    column_span INT NOT NULL DEFAULT 1,
    row_span INT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    visibility_scope VARCHAR(20) NOT NULL DEFAULT 'USER_HOME',
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_club_dashboard_widget_key UNIQUE (club_id, widget_key),
    CONSTRAINT fk_club_dashboard_widget_club FOREIGN KEY (club_id) REFERENCES club(club_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_dashboard_widget'
          AND COLUMN_NAME = 'visibility_scope'
    ),
    'SELECT 1',
    'ALTER TABLE club_dashboard_widget ADD COLUMN visibility_scope VARCHAR(20) NOT NULL DEFAULT ''USER_HOME'' AFTER enabled'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE club_dashboard_widget
SET visibility_scope = 'USER_HOME'
WHERE visibility_scope IS NULL
   OR TRIM(visibility_scope) = ''
   OR UPPER(visibility_scope) IN ('ADMIN', 'USER');

ALTER TABLE club_dashboard_widget
MODIFY COLUMN visibility_scope VARCHAR(20) NOT NULL DEFAULT 'USER_HOME';

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_dashboard_widget'
          AND INDEX_NAME = 'idx_club_dashboard_widget_sort'
    ),
    'SELECT 1',
    'CREATE INDEX idx_club_dashboard_widget_sort ON club_dashboard_widget (club_id, visibility_scope, enabled, sort_order)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_dashboard_widget'
          AND CONSTRAINT_NAME = 'fk_club_dashboard_widget_catalog'
    ),
    'SELECT 1',
    'ALTER TABLE club_dashboard_widget ADD CONSTRAINT fk_club_dashboard_widget_catalog FOREIGN KEY (widget_key) REFERENCES dashboard_widget_catalog(widget_key)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'BOARD_NOTICE', 'Board Notice', 'Latest announcements from your board.', 'forum', NULL, 'USER_HOME', 2, 1, 10, 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'BOARD_NOTICE'
);

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'SCHEDULE_OVERVIEW', 'Schedule Overview', 'Upcoming schedules and next events.', 'calendar_month', NULL, 'USER_HOME', 1, 1, 20, 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'SCHEDULE_OVERVIEW'
);

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'PROFILE_SUMMARY', 'My Profile', 'Quick access to your club profile.', 'person', NULL, 'USER_HOME', 1, 1, 30, 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'PROFILE_SUMMARY'
);

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'ATTENDANCE_STATUS', 'Attendance Check', 'Check in and review attendance status.', 'fact_check', 'ATTENDANCE', 'USER_HOME', 1, 1, 40, 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'ATTENDANCE_STATUS'
);
