-- SEMO todo collaboration and decision log migration.
-- Prerequisites:
--   1. Confirm the target is the SEMO public database and take a verified backup.
--   2. Apply semo_handover_apply.sql first so club_operating_term exists.
-- This is a one-time migration. Do not rerun after it succeeds.

USE SEMO;

INSERT INTO feature_catalog (
    feature_key, display_name, description, icon_name, navigation_scope,
    active, sort_order, create_date, update_date
)
SELECT
    'DECISION_LOG', '회의록·결정',
    '회의 배경과 결정 이유, 참여자, 관련 운영 항목과 후속 업무를 기록합니다.',
    'gavel', 'USER_AND_ADMIN', 1, 90, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_catalog WHERE feature_key = 'DECISION_LOG');

UPDATE feature_catalog
SET display_name = '회의록·결정',
    description = '회의 배경과 결정 이유, 참여자, 관련 운영 항목과 후속 업무를 기록합니다.',
    icon_name = 'gavel',
    navigation_scope = 'USER_AND_ADMIN',
    active = 1,
    sort_order = 90,
    update_date = NOW()
WHERE feature_key = 'DECISION_LOG';

INSERT INTO feature_permission_catalog (
    permission_key, feature_key, display_name, description, ownership_scope,
    active, sort_order, create_date, update_date
)
SELECT
    'DECISION_VIEW', 'DECISION_LOG', '운영 결정 조회',
    '운영진 공개 회의록, 결정 초안과 검토 일정을 조회합니다.',
    'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'DECISION_VIEW');

INSERT INTO feature_permission_catalog (
    permission_key, feature_key, display_name, description, ownership_scope,
    active, sort_order, create_date, update_date
)
SELECT
    'DECISION_MANAGE', 'DECISION_LOG', '운영 결정 관리',
    '회의록과 결정 초안을 작성하고 확정, 대체, 보관합니다.',
    'CLUB', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'DECISION_MANAGE');

INSERT INTO dashboard_widget_catalog (
    widget_key, display_name, description, icon_name, required_feature_key,
    default_visibility_scope, default_column_span, default_row_span,
    default_sort_order, active, create_date, update_date
)
SELECT
    'DECISION_LATEST', '최근 운영 결정',
    '최근 확정된 회의록과 운영 결정을 확인합니다.',
    'gavel', 'DECISION_LOG', 'USER_HOME', 1, 1, 48, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'DECISION_LATEST');

CREATE TABLE decision_record (
    decision_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    club_operating_term_id BIGINT NULL,
    record_type VARCHAR(30) NOT NULL,
    status_code VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    visibility_scope VARCHAR(20) NOT NULL DEFAULT 'OPERATORS',
    title VARCHAR(200) NOT NULL,
    decision_content TEXT NOT NULL,
    background_context TEXT NULL,
    rationale TEXT NULL,
    meeting_at DATETIME NULL,
    effective_date DATE NULL,
    review_date DATE NULL,
    supersedes_decision_record_id BIGINT NULL,
    created_by_club_profile_id BIGINT NOT NULL,
    confirmed_by_club_profile_id BIGINT NULL,
    confirmed_at DATETIME NULL,
    archived_by_club_profile_id BIGINT NULL,
    archived_at DATETIME NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_by_club_profile_id BIGINT NULL,
    deleted_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_decision_record_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_decision_record_term FOREIGN KEY (club_operating_term_id) REFERENCES club_operating_term(club_operating_term_id),
    CONSTRAINT fk_decision_record_supersedes FOREIGN KEY (supersedes_decision_record_id) REFERENCES decision_record(decision_record_id),
    CONSTRAINT fk_decision_record_created_by FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_decision_record_confirmed_by FOREIGN KEY (confirmed_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_decision_record_archived_by FOREIGN KEY (archived_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_decision_record_deleted_by FOREIGN KEY (deleted_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    KEY idx_decision_record_feed (club_id, deleted, status_code, confirmed_at, decision_record_id),
    KEY idx_decision_record_review (club_id, deleted, status_code, review_date, decision_record_id),
    KEY idx_decision_record_term (club_id, club_operating_term_id, status_code, decision_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE decision_participant (
    decision_participant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    decision_record_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    participant_role VARCHAR(20) NOT NULL,
    display_name_snapshot VARCHAR(100) NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_decision_participant_profile UNIQUE (decision_record_id, club_profile_id),
    CONSTRAINT fk_decision_participant_record FOREIGN KEY (decision_record_id) REFERENCES decision_record(decision_record_id),
    CONSTRAINT fk_decision_participant_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id),
    KEY idx_decision_participant_profile (club_profile_id, decision_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE decision_resource_link (
    decision_resource_link_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    decision_record_id BIGINT NOT NULL,
    relation_type VARCHAR(20) NOT NULL,
    resource_type VARCHAR(40) NOT NULL,
    resource_id BIGINT NOT NULL,
    resource_title_snapshot VARCHAR(200) NOT NULL,
    resource_path_snapshot VARCHAR(500) NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_decision_resource_link UNIQUE (decision_record_id, relation_type, resource_type, resource_id),
    CONSTRAINT fk_decision_resource_link_record FOREIGN KEY (decision_record_id) REFERENCES decision_record(decision_record_id),
    KEY idx_decision_resource_target (resource_type, resource_id, decision_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE todo_item
    ADD COLUMN priority_code VARCHAR(20) NOT NULL DEFAULT 'NORMAL' AFTER status_code,
    ADD COLUMN recruitment_capacity INT NOT NULL DEFAULT 1 AFTER priority_code,
    ADD COLUMN work_start_at DATETIME NULL AFTER due_at,
    ADD COLUMN work_end_at DATETIME NULL AFTER work_start_at,
    ADD COLUMN linked_schedule_event_id BIGINT NULL AFTER work_end_at,
    ADD COLUMN linked_decision_record_id BIGINT NULL AFTER linked_schedule_event_id,
    ADD COLUMN recurrence_frequency VARCHAR(20) NOT NULL DEFAULT 'NONE' AFTER linked_decision_record_id,
    ADD COLUMN recurrence_interval INT NOT NULL DEFAULT 1 AFTER recurrence_frequency,
    ADD COLUMN recurrence_end_date DATE NULL AFTER recurrence_interval,
    ADD COLUMN recurrence_source_todo_item_id BIGINT NULL AFTER recurrence_end_date,
    ADD CONSTRAINT fk_todo_item_schedule_event FOREIGN KEY (linked_schedule_event_id) REFERENCES club_schedule_event(event_id),
    ADD CONSTRAINT fk_todo_item_decision_record FOREIGN KEY (linked_decision_record_id) REFERENCES decision_record(decision_record_id),
    ADD CONSTRAINT fk_todo_item_recurrence_source FOREIGN KEY (recurrence_source_todo_item_id) REFERENCES todo_item(todo_item_id),
    ADD CONSTRAINT uk_todo_item_recurrence_source UNIQUE (recurrence_source_todo_item_id);

CREATE INDEX idx_todo_item_priority
    ON todo_item (club_id, status_code, priority_code, due_at, todo_item_id);

CREATE INDEX idx_todo_item_recurrence
    ON todo_item (club_id, recurrence_frequency, recurrence_end_date, todo_item_id);

CREATE TABLE todo_item_assignee (
    todo_item_assignee_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    todo_item_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    assigned_by_club_profile_id BIGINT NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_todo_item_assignee UNIQUE (todo_item_id, club_profile_id),
    CONSTRAINT fk_todo_item_assignee_todo FOREIGN KEY (todo_item_id) REFERENCES todo_item(todo_item_id),
    CONSTRAINT fk_todo_item_assignee_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_todo_item_assignee_actor FOREIGN KEY (assigned_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    KEY idx_todo_item_assignee_profile (club_profile_id, todo_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO todo_item_assignee (
    todo_item_id, club_profile_id, assigned_by_club_profile_id, create_date, update_date
)
SELECT
    todo_item_id,
    assigned_club_profile_id,
    COALESCE(assigned_by_club_profile_id, created_by_club_profile_id),
    NOW(),
    NOW()
FROM todo_item
WHERE assigned_club_profile_id IS NOT NULL;

CREATE TABLE todo_checklist_item (
    todo_checklist_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    todo_item_id BIGINT NOT NULL,
    content VARCHAR(300) NOT NULL,
    sort_order INT NOT NULL,
    completed TINYINT(1) NOT NULL DEFAULT 0,
    completed_by_club_profile_id BIGINT NULL,
    completed_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_todo_checklist_item_todo FOREIGN KEY (todo_item_id) REFERENCES todo_item(todo_item_id),
    CONSTRAINT fk_todo_checklist_item_completed_by FOREIGN KEY (completed_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    KEY idx_todo_checklist_item_order (todo_item_id, sort_order, todo_checklist_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE todo_comment (
    todo_comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    todo_item_id BIGINT NOT NULL,
    author_club_profile_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_by_club_profile_id BIGINT NULL,
    deleted_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_todo_comment_todo FOREIGN KEY (todo_item_id) REFERENCES todo_item(todo_item_id),
    CONSTRAINT fk_todo_comment_author FOREIGN KEY (author_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_todo_comment_deleted_by FOREIGN KEY (deleted_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    KEY idx_todo_comment_feed (todo_item_id, deleted, create_date, todo_comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Post-apply verification. All table and column rows must be present.
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'decision_record', 'decision_participant', 'decision_resource_link',
      'todo_item_assignee', 'todo_checklist_item', 'todo_comment'
  )
ORDER BY table_name;

SELECT column_name
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'todo_item'
  AND column_name IN (
      'priority_code', 'recruitment_capacity', 'work_start_at', 'work_end_at',
      'linked_schedule_event_id', 'linked_decision_record_id', 'recurrence_frequency',
      'recurrence_interval', 'recurrence_end_date', 'recurrence_source_todo_item_id'
  )
ORDER BY column_name;

SELECT todo_item_id, assigned_club_profile_id
FROM todo_item
WHERE assigned_club_profile_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM todo_item_assignee assignee
      WHERE assignee.todo_item_id = todo_item.todo_item_id
        AND assignee.club_profile_id = todo_item.assigned_club_profile_id
  );

SELECT feature_key
FROM feature_catalog
WHERE feature_key = 'DECISION_LOG';

SELECT permission_key
FROM feature_permission_catalog
WHERE permission_key IN ('DECISION_VIEW', 'DECISION_MANAGE')
ORDER BY permission_key;

SELECT widget_key
FROM dashboard_widget_catalog
WHERE widget_key = 'DECISION_LATEST';
