-- ============================================================
-- Semo finance migration apply script
-- Purpose:
--   1. Seed the new FINANCE catalog/permission/widget keys
--   2. Migrate legacy DUES feature activations, role permissions,
--      dashboard widgets, and dues data into finance tables
-- Re-runnable.
-- If cleanup already removed legacy DUES rows/tables, this script
-- bootstraps FINANCE schema/catalog only and skips legacy data migration.
-- ============================================================

USE SEMO;

CREATE TABLE IF NOT EXISTS finance_obligation (
    finance_obligation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    created_by_club_profile_id BIGINT NULL,
    obligation_type_code VARCHAR(30) NOT NULL DEFAULT 'FEE',
    title VARCHAR(150) NOT NULL,
    target_scope_code VARCHAR(30) NOT NULL DEFAULT 'ALL_ACTIVE_MEMBERS',
    amount DECIMAL(12,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL DEFAULT 'KRW',
    due_at DATETIME NULL,
    status_code VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    note VARCHAR(500) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    KEY idx_finance_obligation_club_sort (club_id, create_date, finance_obligation_id),
    KEY idx_finance_obligation_status (club_id, status_code, finance_obligation_id),
    CONSTRAINT fk_finance_obligation_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_finance_obligation_created_by FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS finance_payment (
    finance_payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    finance_obligation_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL DEFAULT 'KRW',
    payment_status_code VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at DATETIME NULL,
    note VARCHAR(500) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    KEY idx_finance_payment_status (club_id, payment_status_code, finance_obligation_id),
    CONSTRAINT uk_finance_payment_obligation_profile UNIQUE (finance_obligation_id, club_profile_id),
    CONSTRAINT fk_finance_payment_obligation FOREIGN KEY (finance_obligation_id) REFERENCES finance_obligation(finance_obligation_id),
    CONSTRAINT fk_finance_payment_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_finance_payment_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS finance_request (
    finance_request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    requester_club_profile_id BIGINT NOT NULL,
    request_type_code VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL DEFAULT 'KRW',
    related_event_name VARCHAR(120) NULL,
    note VARCHAR(1000) NULL,
    status_code VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    reviewed_by_club_profile_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    review_note VARCHAR(1000) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    KEY idx_finance_request_club_status (club_id, status_code, finance_request_id),
    KEY idx_finance_request_requester_status (requester_club_profile_id, status_code, finance_request_id),
    CONSTRAINT fk_finance_request_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_finance_request_requester FOREIGN KEY (requester_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_finance_request_reviewed_by FOREIGN KEY (reviewed_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS finance_expense (
    finance_expense_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    entered_by_club_profile_id BIGINT NOT NULL,
    expense_type_code VARCHAR(30) NOT NULL,
    category_code VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL DEFAULT 'KRW',
    spent_at DATETIME NOT NULL,
    related_event_name VARCHAR(120) NULL,
    note VARCHAR(1000) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    KEY idx_finance_expense_club_spent (club_id, spent_at, finance_expense_id),
    CONSTRAINT fk_finance_expense_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_finance_expense_entered_by FOREIGN KEY (entered_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'FINANCE', '재정관리', '커스텀 재정 항목을 발행하고 멤버별 납부 상태를 운영합니다.', 'payments', 'USER_AND_ADMIN', 1, 58, NOW(), NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'FINANCE'
);

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'FINANCE_VIEW', 'FINANCE', '재정 조회', '재정 운영 화면과 납부 현황을 조회합니다.', 'CLUB', 1, 10, NOW(), NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_permission_catalog
    WHERE permission_key = 'FINANCE_VIEW'
);

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'FINANCE_ISSUE', 'FINANCE', '재정 항목 발행', '재정 항목을 생성하고 대상 멤버에게 발행합니다.', 'CLUB', 1, 20, NOW(), NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_permission_catalog
    WHERE permission_key = 'FINANCE_ISSUE'
);

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'FINANCE_MARK_PAID', 'FINANCE', '재정 납부 처리', '재정 항목을 납부 완료 상태로 변경합니다.', 'CLUB', 1, 30, NOW(), NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_permission_catalog
    WHERE permission_key = 'FINANCE_MARK_PAID'
);

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'FINANCE_MARK_WAIVED', 'FINANCE', '재정 면제 처리', '재정 항목을 면제 상태로 변경합니다.', 'CLUB', 1, 40, NOW(), NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_permission_catalog
    WHERE permission_key = 'FINANCE_MARK_WAIVED'
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
SELECT 'FINANCE_STATUS', 'Finance Status', 'My pending finance items and latest payment status.', 'payments', 'FINANCE', 'USER_HOME', 1, 1, 42, 1, NOW(), NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM dashboard_widget_catalog
    WHERE widget_key = 'FINANCE_STATUS'
);

INSERT INTO feature_activation (
    club_id,
    feature_key,
    enabled,
    sort_order,
    enabled_by_club_profile_id,
    enabled_at,
    create_date,
    update_date
)
SELECT
    legacy.club_id,
    'FINANCE',
    legacy.enabled,
    legacy.sort_order,
    legacy.enabled_by_club_profile_id,
    legacy.enabled_at,
    legacy.create_date,
    NOW()
FROM feature_activation legacy
WHERE legacy.feature_key = 'DUES'
  AND NOT EXISTS (
      SELECT 1
      FROM feature_activation finance
      WHERE finance.club_id = legacy.club_id
        AND finance.feature_key = 'FINANCE'
  );

INSERT INTO club_position_permission (
    club_position_id,
    permission_key,
    create_date,
    update_date
)
SELECT legacy.club_position_id, 'FINANCE_VIEW', NOW(), NOW()
FROM club_position_permission legacy
WHERE legacy.permission_key = 'DUES_VIEW'
  AND NOT EXISTS (
      SELECT 1
      FROM club_position_permission finance
      WHERE finance.club_position_id = legacy.club_position_id
        AND finance.permission_key = 'FINANCE_VIEW'
  );

INSERT INTO club_position_permission (
    club_position_id,
    permission_key,
    create_date,
    update_date
)
SELECT legacy.club_position_id, 'FINANCE_ISSUE', NOW(), NOW()
FROM club_position_permission legacy
WHERE legacy.permission_key = 'DUES_ISSUE'
  AND NOT EXISTS (
      SELECT 1
      FROM club_position_permission finance
      WHERE finance.club_position_id = legacy.club_position_id
        AND finance.permission_key = 'FINANCE_ISSUE'
  );

INSERT INTO club_position_permission (
    club_position_id,
    permission_key,
    create_date,
    update_date
)
SELECT legacy.club_position_id, 'FINANCE_MARK_PAID', NOW(), NOW()
FROM club_position_permission legacy
WHERE legacy.permission_key = 'DUES_MARK_PAID'
  AND NOT EXISTS (
      SELECT 1
      FROM club_position_permission finance
      WHERE finance.club_position_id = legacy.club_position_id
        AND finance.permission_key = 'FINANCE_MARK_PAID'
  );

INSERT INTO club_position_permission (
    club_position_id,
    permission_key,
    create_date,
    update_date
)
SELECT legacy.club_position_id, 'FINANCE_MARK_WAIVED', NOW(), NOW()
FROM club_position_permission legacy
WHERE legacy.permission_key = 'DUES_MARK_WAIVED'
  AND NOT EXISTS (
      SELECT 1
      FROM club_position_permission finance
      WHERE finance.club_position_id = legacy.club_position_id
        AND finance.permission_key = 'FINANCE_MARK_WAIVED'
  );

INSERT INTO club_dashboard_widget (
    club_id,
    widget_key,
    title_override,
    column_span,
    row_span,
    sort_order,
    enabled,
    visibility_scope,
    create_date,
    update_date
)
SELECT
    legacy.club_id,
    'FINANCE_STATUS',
    legacy.title_override,
    legacy.column_span,
    legacy.row_span,
    legacy.sort_order,
    legacy.enabled,
    legacy.visibility_scope,
    legacy.create_date,
    NOW()
FROM club_dashboard_widget legacy
WHERE legacy.widget_key = 'DUES_STATUS'
  AND NOT EXISTS (
      SELECT 1
      FROM club_dashboard_widget finance
      WHERE finance.club_id = legacy.club_id
        AND finance.widget_key = 'FINANCE_STATUS'
  );

SET @has_dues_charge := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'dues_charge'
);

SET @finance_obligation_migration_sql := IF(
    @has_dues_charge = 1,
    'INSERT INTO finance_obligation (finance_obligation_id, club_id, created_by_club_profile_id, obligation_type_code, title, target_scope_code, amount, currency_code, due_at, status_code, note, create_date, update_date) SELECT legacy.dues_charge_id, legacy.club_id, legacy.created_by_club_profile_id, ''FEE'', legacy.title, COALESCE(legacy.target_scope, ''ALL_ACTIVE_MEMBERS''), legacy.amount, COALESCE(legacy.currency_code, ''KRW''), legacy.due_at, ''OPEN'', legacy.note, legacy.create_date, legacy.update_date FROM dues_charge legacy WHERE NOT EXISTS (SELECT 1 FROM finance_obligation finance WHERE finance.finance_obligation_id = legacy.dues_charge_id)',
    'SELECT 1'
);

PREPARE finance_obligation_migration_stmt FROM @finance_obligation_migration_sql;
EXECUTE finance_obligation_migration_stmt;
DEALLOCATE PREPARE finance_obligation_migration_stmt;

SET @has_dues_invoice := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'dues_invoice'
);

SET @finance_payment_migration_sql := IF(
    @has_dues_invoice = 1 AND @has_dues_charge = 1,
    'INSERT INTO finance_payment (finance_payment_id, finance_obligation_id, club_id, club_profile_id, amount, currency_code, payment_status_code, paid_at, note, create_date, update_date) SELECT legacy.dues_invoice_id, legacy.dues_charge_id, legacy.club_id, legacy.club_profile_id, legacy.amount, COALESCE(legacy.currency_code, ''KRW''), legacy.payment_status, legacy.paid_at, legacy.note, legacy.create_date, legacy.update_date FROM dues_invoice legacy WHERE EXISTS (SELECT 1 FROM finance_obligation obligation WHERE obligation.finance_obligation_id = legacy.dues_charge_id) AND NOT EXISTS (SELECT 1 FROM finance_payment finance WHERE finance.finance_payment_id = legacy.dues_invoice_id)',
    'SELECT 1'
);

PREPARE finance_payment_migration_stmt FROM @finance_payment_migration_sql;
EXECUTE finance_payment_migration_stmt;
DEALLOCATE PREPARE finance_payment_migration_stmt;

UPDATE finance_obligation finance
SET finance.status_code = 'CLOSED',
    finance.update_date = NOW()
WHERE EXISTS (
    SELECT 1
    FROM finance_payment payment
    WHERE payment.finance_obligation_id = finance.finance_obligation_id
)
  AND NOT EXISTS (
      SELECT 1
      FROM finance_payment payment
      WHERE payment.finance_obligation_id = finance.finance_obligation_id
        AND payment.payment_status_code IN ('PENDING', 'OVERDUE')
  );

UPDATE finance_obligation finance
SET finance.status_code = 'OPEN',
    finance.update_date = NOW()
WHERE EXISTS (
    SELECT 1
    FROM finance_payment payment
    WHERE payment.finance_obligation_id = finance.finance_obligation_id
      AND payment.payment_status_code IN ('PENDING', 'OVERDUE')
);
