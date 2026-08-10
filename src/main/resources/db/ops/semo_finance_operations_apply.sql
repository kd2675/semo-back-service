-- SEMO finance operations migration.
-- Prerequisites:
--   1. Confirm the target is the SEMO public database and take a verified backup.
--   2. Apply semo_finance_request_expense_apply.sql if source_finance_request_id is absent.
--   3. Apply semo_handover_apply.sql so club_operating_term exists.
-- This is a one-time migration. Do not rerun after it succeeds.

USE SEMO;

CREATE TABLE finance_account (
    finance_account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    created_by_club_profile_id BIGINT NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    account_type_code VARCHAR(20) NOT NULL,
    provider_name VARCHAR(100) NULL,
    masked_identifier VARCHAR(100) NULL,
    holder_name VARCHAR(100) NULL,
    usage_scope_code VARCHAR(20) NOT NULL DEFAULT 'BOTH',
    active TINYINT(1) NOT NULL DEFAULT 1,
    default_collection TINYINT(1) NOT NULL DEFAULT 0,
    default_expense TINYINT(1) NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_finance_account_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_finance_account_created_by FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    KEY idx_finance_account_club_active (club_id, active, display_name, finance_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE finance_period (
    finance_period_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    club_operating_term_id BIGINT NULL,
    created_by_club_profile_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status_code VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opening_balance DECIMAL(14,2) NOT NULL DEFAULT 0,
    closing_balance DECIMAL(14,2) NULL,
    closed_by_club_profile_id BIGINT NULL,
    closed_at DATETIME NULL,
    note VARCHAR(500) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_finance_period_club_dates UNIQUE (club_id, start_date, end_date),
    CONSTRAINT fk_finance_period_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_finance_period_term FOREIGN KEY (club_operating_term_id) REFERENCES club_operating_term(club_operating_term_id),
    CONSTRAINT fk_finance_period_created_by FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_finance_period_closed_by FOREIGN KEY (closed_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    KEY idx_finance_period_club_status (club_id, status_code, start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE finance_budget (
    finance_budget_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    finance_period_id BIGINT NOT NULL,
    category_code VARCHAR(40) NOT NULL,
    allocated_amount DECIMAL(14,2) NOT NULL,
    note VARCHAR(500) NULL,
    created_by_club_profile_id BIGINT NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_finance_budget_period_category UNIQUE (finance_period_id, category_code),
    CONSTRAINT fk_finance_budget_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_finance_budget_period FOREIGN KEY (finance_period_id) REFERENCES finance_period(finance_period_id),
    CONSTRAINT fk_finance_budget_created_by FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    KEY idx_finance_budget_club_period (club_id, finance_period_id, category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE finance_obligation
    ADD COLUMN finance_period_id BIGINT NULL AFTER created_by_club_profile_id,
    ADD COLUMN finance_account_id BIGINT NULL AFTER finance_period_id,
    ADD COLUMN linked_schedule_event_id BIGINT NULL AFTER finance_account_id,
    ADD COLUMN recurrence_frequency VARCHAR(20) NOT NULL DEFAULT 'NONE' AFTER note,
    ADD COLUMN recurrence_interval INT NOT NULL DEFAULT 1 AFTER recurrence_frequency,
    ADD COLUMN recurrence_end_date DATE NULL AFTER recurrence_interval,
    ADD COLUMN recurrence_source_finance_obligation_id BIGINT NULL AFTER recurrence_end_date,
    ADD CONSTRAINT fk_finance_obligation_period FOREIGN KEY (finance_period_id) REFERENCES finance_period(finance_period_id),
    ADD CONSTRAINT fk_finance_obligation_account FOREIGN KEY (finance_account_id) REFERENCES finance_account(finance_account_id),
    ADD CONSTRAINT fk_finance_obligation_schedule_event FOREIGN KEY (linked_schedule_event_id) REFERENCES club_schedule_event(event_id),
    ADD CONSTRAINT fk_finance_obligation_recurrence_source FOREIGN KEY (recurrence_source_finance_obligation_id) REFERENCES finance_obligation(finance_obligation_id),
    ADD CONSTRAINT uk_finance_obligation_recurrence_source UNIQUE (recurrence_source_finance_obligation_id);

CREATE INDEX idx_finance_obligation_period
    ON finance_obligation (finance_period_id, status_code, finance_obligation_id);
CREATE INDEX idx_finance_obligation_recurrence
    ON finance_obligation (club_id, recurrence_frequency, recurrence_end_date, finance_obligation_id);

ALTER TABLE finance_payment
    ADD COLUMN finance_account_id BIGINT NULL AFTER club_profile_id,
    ADD COLUMN payment_method_code VARCHAR(20) NULL AFTER paid_at,
    ADD CONSTRAINT fk_finance_payment_account FOREIGN KEY (finance_account_id) REFERENCES finance_account(finance_account_id);

ALTER TABLE finance_request
    ADD COLUMN linked_schedule_event_id BIGINT NULL AFTER currency_code,
    ADD CONSTRAINT fk_finance_request_schedule_event FOREIGN KEY (linked_schedule_event_id) REFERENCES club_schedule_event(event_id);

ALTER TABLE finance_expense
    ADD COLUMN finance_period_id BIGINT NULL AFTER source_finance_request_id,
    ADD COLUMN finance_account_id BIGINT NULL AFTER finance_period_id,
    ADD COLUMN linked_schedule_event_id BIGINT NULL AFTER finance_account_id,
    ADD COLUMN status_code VARCHAR(20) NOT NULL DEFAULT 'POSTED' AFTER note,
    ADD COLUMN voided_by_club_profile_id BIGINT NULL AFTER status_code,
    ADD COLUMN voided_at DATETIME NULL AFTER voided_by_club_profile_id,
    ADD COLUMN void_reason VARCHAR(1000) NULL AFTER voided_at,
    ADD CONSTRAINT fk_finance_expense_period FOREIGN KEY (finance_period_id) REFERENCES finance_period(finance_period_id),
    ADD CONSTRAINT fk_finance_expense_account FOREIGN KEY (finance_account_id) REFERENCES finance_account(finance_account_id),
    ADD CONSTRAINT fk_finance_expense_schedule_event FOREIGN KEY (linked_schedule_event_id) REFERENCES club_schedule_event(event_id),
    ADD CONSTRAINT fk_finance_expense_voided_by FOREIGN KEY (voided_by_club_profile_id) REFERENCES club_profile(club_profile_id);

CREATE INDEX idx_finance_expense_period_category
    ON finance_expense (finance_period_id, status_code, category_code, spent_at);

CREATE TABLE finance_expense_revision (
    finance_expense_revision_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    finance_expense_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    revised_by_club_profile_id BIGINT NOT NULL,
    revision_type_code VARCHAR(20) NOT NULL,
    previous_title VARCHAR(200) NOT NULL,
    next_title VARCHAR(200) NULL,
    previous_category_code VARCHAR(40) NOT NULL,
    next_category_code VARCHAR(40) NULL,
    previous_amount DECIMAL(14,2) NOT NULL,
    next_amount DECIMAL(14,2) NULL,
    previous_spent_at DATETIME NOT NULL,
    next_spent_at DATETIME NULL,
    previous_schedule_event_id BIGINT NULL,
    next_schedule_event_id BIGINT NULL,
    previous_finance_account_id BIGINT NULL,
    next_finance_account_id BIGINT NULL,
    previous_finance_period_id BIGINT NULL,
    next_finance_period_id BIGINT NULL,
    previous_note VARCHAR(1000) NULL,
    next_note VARCHAR(1000) NULL,
    previous_status_code VARCHAR(20) NOT NULL,
    next_status_code VARCHAR(20) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_finance_expense_revision_expense FOREIGN KEY (finance_expense_id) REFERENCES finance_expense(finance_expense_id),
    CONSTRAINT fk_finance_expense_revision_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_finance_expense_revision_revised_by FOREIGN KEY (revised_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_finance_expense_revision_previous_event FOREIGN KEY (previous_schedule_event_id) REFERENCES club_schedule_event(event_id),
    CONSTRAINT fk_finance_expense_revision_next_event FOREIGN KEY (next_schedule_event_id) REFERENCES club_schedule_event(event_id),
    CONSTRAINT fk_finance_expense_revision_previous_account FOREIGN KEY (previous_finance_account_id) REFERENCES finance_account(finance_account_id),
    CONSTRAINT fk_finance_expense_revision_next_account FOREIGN KEY (next_finance_account_id) REFERENCES finance_account(finance_account_id),
    CONSTRAINT fk_finance_expense_revision_previous_period FOREIGN KEY (previous_finance_period_id) REFERENCES finance_period(finance_period_id),
    CONSTRAINT fk_finance_expense_revision_next_period FOREIGN KEY (next_finance_period_id) REFERENCES finance_period(finance_period_id),
    KEY idx_finance_expense_revision_expense (finance_expense_id, finance_expense_revision_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO feature_permission_catalog
    (permission_key, feature_key, display_name, description, ownership_scope, active, sort_order, create_date, update_date)
SELECT 'FINANCE_BILLING_ISSUE', 'FINANCE', '청구 발행', '회비와 분담금 청구를 생성하고 대상 멤버에게 발행합니다.', 'CLUB', 1, 21, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_BILLING_ISSUE');
INSERT INTO feature_permission_catalog
    (permission_key, feature_key, display_name, description, ownership_scope, active, sort_order, create_date, update_date)
SELECT 'FINANCE_REQUEST_REVIEW', 'FINANCE', '재정 요청 검토', '회원의 선지출, 환불, 정산 요청을 승인하거나 반려합니다.', 'CLUB', 1, 22, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_REQUEST_REVIEW');
INSERT INTO feature_permission_catalog
    (permission_key, feature_key, display_name, description, ownership_scope, active, sort_order, create_date, update_date)
SELECT 'FINANCE_EXPENSE_CREATE', 'FINANCE', '지출 전표 관리', '지출 전표를 입력하고 정정 또는 취소합니다.', 'CLUB', 1, 23, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_EXPENSE_CREATE');
INSERT INTO feature_permission_catalog
    (permission_key, feature_key, display_name, description, ownership_scope, active, sort_order, create_date, update_date)
SELECT 'FINANCE_PAYMENT_UPDATE', 'FINANCE', '납부 상태 변경', '멤버별 납부 상태와 결제 수단을 변경합니다.', 'CLUB', 1, 24, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_PAYMENT_UPDATE');
INSERT INTO feature_permission_catalog
    (permission_key, feature_key, display_name, description, ownership_scope, active, sort_order, create_date, update_date)
SELECT 'FINANCE_EXPORT', 'FINANCE', '재정 내보내기', '재정 원장을 CSV로 내보냅니다.', 'CLUB', 1, 25, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_EXPORT');
INSERT INTO feature_permission_catalog
    (permission_key, feature_key, display_name, description, ownership_scope, active, sort_order, create_date, update_date)
SELECT 'FINANCE_PERIOD_CLOSE', 'FINANCE', '예산과 기간 마감', '재정 기간, 예산, 계좌를 설정하고 기간을 마감합니다.', 'CLUB', 1, 26, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_PERIOD_CLOSE');

-- Post-apply verification. Every query must return the expected object/column/index.
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('finance_account', 'finance_period', 'finance_budget', 'finance_expense_revision');

SELECT table_name, column_name
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'finance_obligation' AND column_name IN ('finance_period_id', 'finance_account_id', 'linked_schedule_event_id', 'recurrence_frequency', 'recurrence_source_finance_obligation_id'))
    OR (table_name = 'finance_payment' AND column_name IN ('finance_account_id', 'payment_method_code'))
    OR (table_name = 'finance_request' AND column_name = 'linked_schedule_event_id')
    OR (table_name = 'finance_expense' AND column_name IN ('finance_period_id', 'finance_account_id', 'linked_schedule_event_id', 'status_code', 'void_reason')))
ORDER BY table_name, column_name;

SELECT permission_key
FROM feature_permission_catalog
WHERE permission_key IN (
    'FINANCE_BILLING_ISSUE', 'FINANCE_REQUEST_REVIEW', 'FINANCE_EXPENSE_CREATE',
    'FINANCE_PAYMENT_UPDATE', 'FINANCE_EXPORT', 'FINANCE_PERIOD_CLOSE'
)
ORDER BY permission_key;
