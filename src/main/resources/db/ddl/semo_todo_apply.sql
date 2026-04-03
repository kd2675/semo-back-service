-- SEMO todo rollout SQL for existing production environments.
-- Includes:
-- 1. todo_item table
-- 2. feature_catalog seed for TODO
-- 3. feature_permission_catalog seed for TODO permissions

USE SEMO;

CREATE TABLE IF NOT EXISTS todo_item (
    todo_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    created_by_club_profile_id BIGINT NOT NULL,
    assigned_club_profile_id BIGINT NULL,
    assigned_by_club_profile_id BIGINT NULL,
    todo_type VARCHAR(30) NOT NULL,
    assignment_mode VARCHAR(30) NOT NULL DEFAULT 'DIRECT_ASSIGN',
    status_code VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    title VARCHAR(150) NOT NULL,
    description VARCHAR(2000) NULL,
    due_at DATETIME NULL,
    completed_by_club_profile_id BIGINT NULL,
    completed_at DATETIME NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_todo_item_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_todo_item_created_by FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_todo_item_assigned_profile FOREIGN KEY (assigned_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_todo_item_assigned_by FOREIGN KEY (assigned_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_todo_item_completed_by FOREIGN KEY (completed_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_todo_item_status
    ON todo_item (club_id, status_code, due_at, todo_item_id);

CREATE INDEX idx_todo_item_assignment
    ON todo_item (club_id, assignment_mode, status_code, todo_item_id);

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
SELECT
    'TODO',
    '할 일',
    '담당자와 지원 가능 업무를 명확하게 관리하고 완료 상태를 추적합니다.',
    'assignment',
    'USER_AND_ADMIN',
    1,
    59,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'TODO'
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
SELECT 'TODO_VIEW', 'TODO', '할 일 조회', '할 일 운영 화면과 담당 현황을 조회합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TODO_VIEW');

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
SELECT 'TODO_CREATE', 'TODO', '할 일 생성', '새 할 일을 등록하고 기본 정보를 수정합니다.', 'CLUB', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TODO_CREATE');

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
SELECT 'TODO_ASSIGN', 'TODO', '할 일 배정', '담당자를 지정하거나 지원 가능 업무로 전환합니다.', 'CLUB', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TODO_ASSIGN');

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
SELECT 'TODO_MANAGE_STATUS', 'TODO', '할 일 상태 관리', '진행중, 완료, 취소 등 상태를 운영합니다.', 'CLUB', 1, 40, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TODO_MANAGE_STATUS');
