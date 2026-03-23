-- Semo role management seed script
-- Created: 2026-03-23
-- Purpose: seed top-level role-management feature and child permission catalog
-- Notes:
--   1. Run after semo_role_management_apply.sql
--   2. Safe to run multiple times

USE SEMO;

UPDATE feature_catalog
SET
    navigation_scope = 'USER_AND_ADMIN',
    update_date = NOW()
WHERE feature_key IN ('ATTENDANCE', 'TIMELINE', 'NOTICE', 'POLL', 'SCHEDULE_MANAGE');

UPDATE feature_catalog
SET
    display_name = '직책관리',
    description = '직책을 생성하고 하위 권한을 연결해 멤버 권한을 세밀하게 관리합니다.',
    icon_name = 'manage_accounts',
    navigation_scope = 'ADMIN_ONLY',
    active = 1,
    sort_order = 60,
    update_date = NOW()
WHERE feature_key = 'ROLE_MANAGEMENT';

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
    'ROLE_MANAGEMENT',
    '직책관리',
    '직책을 생성하고 하위 권한을 연결해 멤버 권한을 세밀하게 관리합니다.',
    'manage_accounts',
    'ADMIN_ONLY',
    1,
    60,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'ROLE_MANAGEMENT'
);

UPDATE feature_permission_catalog
SET
    feature_key = 'NOTICE',
    display_name = '공지 작성',
    description = '공지 콘텐츠를 새로 작성합니다.',
    ownership_scope = 'CLUB',
    active = 1,
    sort_order = 10,
    update_date = NOW()
WHERE permission_key = 'NOTICE_CREATE';

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
SELECT 'NOTICE_CREATE', 'NOTICE', '공지 작성', '공지 콘텐츠를 새로 작성합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'NOTICE_CREATE');

UPDATE feature_permission_catalog
SET
    feature_key = 'NOTICE',
    display_name = '공지 수정',
    description = '본인이 작성한 공지를 수정합니다.',
    ownership_scope = 'SELF',
    active = 1,
    sort_order = 20,
    update_date = NOW()
WHERE permission_key = 'NOTICE_UPDATE_SELF';

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
SELECT 'NOTICE_UPDATE_SELF', 'NOTICE', '공지 수정', '본인이 작성한 공지를 수정합니다.', 'SELF', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'NOTICE_UPDATE_SELF');

UPDATE feature_permission_catalog
SET
    feature_key = 'NOTICE',
    display_name = '공지 삭제',
    description = '본인이 작성한 공지를 삭제합니다.',
    ownership_scope = 'SELF',
    active = 1,
    sort_order = 30,
    update_date = NOW()
WHERE permission_key = 'NOTICE_DELETE_SELF';

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
SELECT 'NOTICE_DELETE_SELF', 'NOTICE', '공지 삭제', '본인이 작성한 공지를 삭제합니다.', 'SELF', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'NOTICE_DELETE_SELF');

UPDATE feature_permission_catalog
SET
    feature_key = 'POLL',
    display_name = '투표 작성',
    description = '투표를 새로 생성합니다.',
    ownership_scope = 'CLUB',
    active = 1,
    sort_order = 10,
    update_date = NOW()
WHERE permission_key = 'POLL_CREATE';

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
SELECT 'POLL_CREATE', 'POLL', '투표 작성', '투표를 새로 생성합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'POLL_CREATE');

UPDATE feature_permission_catalog
SET
    feature_key = 'POLL',
    display_name = '투표 수정',
    description = '본인이 작성한 투표를 수정합니다.',
    ownership_scope = 'SELF',
    active = 1,
    sort_order = 20,
    update_date = NOW()
WHERE permission_key = 'POLL_UPDATE_SELF';

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
SELECT 'POLL_UPDATE_SELF', 'POLL', '투표 수정', '본인이 작성한 투표를 수정합니다.', 'SELF', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'POLL_UPDATE_SELF');

UPDATE feature_permission_catalog
SET
    feature_key = 'POLL',
    display_name = '투표 삭제',
    description = '본인이 작성한 투표를 삭제합니다.',
    ownership_scope = 'SELF',
    active = 1,
    sort_order = 30,
    update_date = NOW()
WHERE permission_key = 'POLL_DELETE_SELF';

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
SELECT 'POLL_DELETE_SELF', 'POLL', '투표 삭제', '본인이 작성한 투표를 삭제합니다.', 'SELF', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'POLL_DELETE_SELF');

UPDATE feature_permission_catalog
SET
    feature_key = 'SCHEDULE_MANAGE',
    display_name = '일정 작성',
    description = '일정을 새로 생성합니다.',
    ownership_scope = 'CLUB',
    active = 1,
    sort_order = 10,
    update_date = NOW()
WHERE permission_key = 'SCHEDULE_CREATE';

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
SELECT 'SCHEDULE_CREATE', 'SCHEDULE_MANAGE', '일정 작성', '일정을 새로 생성합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'SCHEDULE_CREATE');

UPDATE feature_permission_catalog
SET
    feature_key = 'SCHEDULE_MANAGE',
    display_name = '일정 수정',
    description = '본인이 작성한 일정을 수정합니다.',
    ownership_scope = 'SELF',
    active = 1,
    sort_order = 20,
    update_date = NOW()
WHERE permission_key = 'SCHEDULE_UPDATE_SELF';

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
SELECT 'SCHEDULE_UPDATE_SELF', 'SCHEDULE_MANAGE', '일정 수정', '본인이 작성한 일정을 수정합니다.', 'SELF', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'SCHEDULE_UPDATE_SELF');

UPDATE feature_permission_catalog
SET
    feature_key = 'SCHEDULE_MANAGE',
    display_name = '일정 삭제',
    description = '본인이 작성한 일정을 삭제합니다.',
    ownership_scope = 'SELF',
    active = 1,
    sort_order = 30,
    update_date = NOW()
WHERE permission_key = 'SCHEDULE_DELETE_SELF';

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
SELECT 'SCHEDULE_DELETE_SELF', 'SCHEDULE_MANAGE', '일정 삭제', '본인이 작성한 일정을 삭제합니다.', 'SELF', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'SCHEDULE_DELETE_SELF');

UPDATE feature_permission_catalog
SET
    feature_key = 'TIMELINE',
    display_name = '타임라인 조회',
    description = '타임라인 콘텐츠를 조회합니다.',
    ownership_scope = 'CLUB',
    active = 1,
    sort_order = 10,
    update_date = NOW()
WHERE permission_key = 'TIMELINE_VIEW';

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
SELECT 'TIMELINE_VIEW', 'TIMELINE', '타임라인 조회', '타임라인 콘텐츠를 조회합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TIMELINE_VIEW');

UPDATE feature_permission_catalog
SET
    feature_key = 'ROLE_MANAGEMENT',
    display_name = '직책 조회',
    description = '직책 목록과 권한 구성을 조회합니다.',
    ownership_scope = 'CLUB',
    active = 1,
    sort_order = 10,
    update_date = NOW()
WHERE permission_key = 'ROLE_MANAGEMENT_VIEW';

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
SELECT 'ROLE_MANAGEMENT_VIEW', 'ROLE_MANAGEMENT', '직책 조회', '직책 목록과 권한 구성을 조회합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'ROLE_MANAGEMENT_VIEW');

UPDATE feature_permission_catalog
SET
    feature_key = 'ROLE_MANAGEMENT',
    display_name = '직책 생성',
    description = '새 직책을 생성합니다.',
    ownership_scope = 'CLUB',
    active = 1,
    sort_order = 20,
    update_date = NOW()
WHERE permission_key = 'ROLE_MANAGEMENT_CREATE';

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
SELECT 'ROLE_MANAGEMENT_CREATE', 'ROLE_MANAGEMENT', '직책 생성', '새 직책을 생성합니다.', 'CLUB', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'ROLE_MANAGEMENT_CREATE');

UPDATE feature_permission_catalog
SET
    feature_key = 'ROLE_MANAGEMENT',
    display_name = '직책 수정',
    description = '직책 정보와 권한 구성을 수정합니다.',
    ownership_scope = 'CLUB',
    active = 1,
    sort_order = 30,
    update_date = NOW()
WHERE permission_key = 'ROLE_MANAGEMENT_UPDATE';

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
SELECT 'ROLE_MANAGEMENT_UPDATE', 'ROLE_MANAGEMENT', '직책 수정', '직책 정보와 권한 구성을 수정합니다.', 'CLUB', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'ROLE_MANAGEMENT_UPDATE');

UPDATE feature_permission_catalog
SET
    feature_key = 'ROLE_MANAGEMENT',
    display_name = '직책 삭제',
    description = '직책을 삭제합니다.',
    ownership_scope = 'CLUB',
    active = 1,
    sort_order = 40,
    update_date = NOW()
WHERE permission_key = 'ROLE_MANAGEMENT_DELETE';

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
SELECT 'ROLE_MANAGEMENT_DELETE', 'ROLE_MANAGEMENT', '직책 삭제', '직책을 삭제합니다.', 'CLUB', 1, 40, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'ROLE_MANAGEMENT_DELETE');

UPDATE feature_permission_catalog
SET
    feature_key = 'ROLE_MANAGEMENT',
    display_name = '직책 할당',
    description = '멤버에게 직책을 할당하거나 해제합니다.',
    ownership_scope = 'CLUB',
    active = 1,
    sort_order = 50,
    update_date = NOW()
WHERE permission_key = 'ROLE_MANAGEMENT_ASSIGN';

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
SELECT 'ROLE_MANAGEMENT_ASSIGN', 'ROLE_MANAGEMENT', '직책 할당', '멤버에게 직책을 할당하거나 해제합니다.', 'CLUB', 1, 50, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'ROLE_MANAGEMENT_ASSIGN');
