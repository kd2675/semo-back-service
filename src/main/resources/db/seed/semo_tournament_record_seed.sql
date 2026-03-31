-- Semo tournament-record seed script
-- Created: 2026-03-25
-- Purpose: seed tournament feature catalog, permissions, and widget
-- Notes:
--   1. Run after semo_tournament_record_apply.sql
--   2. Safe to run multiple times

USE SEMO;

UPDATE feature_catalog
SET
    display_name = '대회기록',
    description = '대회를 생성하고 참가신청, 엔트리, 브래킷을 운영합니다.',
    icon_name = 'emoji_events',
    navigation_scope = 'USER_AND_ADMIN',
    active = 1,
    sort_order = 55,
    update_date = NOW()
WHERE feature_key = 'TOURNAMENT_RECORD';

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
    'TOURNAMENT_RECORD',
    '대회기록',
    '대회를 생성하고 참가신청, 엔트리, 브래킷을 운영합니다.',
    'emoji_events',
    'USER_AND_ADMIN',
    1,
    55,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'TOURNAMENT_RECORD'
);

UPDATE feature_permission_catalog
SET
    feature_key = 'TOURNAMENT_RECORD',
    display_name = '대회 작성',
    description = '대회를 새로 생성합니다.',
    ownership_scope = 'CLUB',
    active = 1,
    sort_order = 10,
    update_date = NOW()
WHERE permission_key = 'TOURNAMENT_RECORD_CREATE';

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
SELECT 'TOURNAMENT_RECORD_CREATE', 'TOURNAMENT_RECORD', '대회 작성', '대회를 새로 생성합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TOURNAMENT_RECORD_CREATE');

UPDATE feature_permission_catalog
SET
    feature_key = 'TOURNAMENT_RECORD',
    display_name = '대회 수정',
    description = '본인이 작성한 대회를 수정합니다.',
    ownership_scope = 'SELF',
    active = 1,
    sort_order = 20,
    update_date = NOW()
WHERE permission_key = 'TOURNAMENT_RECORD_UPDATE_SELF';

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
SELECT 'TOURNAMENT_RECORD_UPDATE_SELF', 'TOURNAMENT_RECORD', '대회 수정', '본인이 작성한 대회를 수정합니다.', 'SELF', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TOURNAMENT_RECORD_UPDATE_SELF');

UPDATE feature_permission_catalog
SET
    feature_key = 'TOURNAMENT_RECORD',
    display_name = '대회 고정',
    description = '대회를 게시판 상단에 고정합니다.',
    ownership_scope = 'SELF',
    active = 1,
    sort_order = 30,
    update_date = NOW()
WHERE permission_key = 'TOURNAMENT_RECORD_PIN';

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
SELECT 'TOURNAMENT_RECORD_PIN', 'TOURNAMENT_RECORD', '대회 고정', '대회를 게시판 상단에 고정합니다.', 'SELF', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TOURNAMENT_RECORD_PIN');

UPDATE feature_permission_catalog
SET
    feature_key = 'TOURNAMENT_RECORD',
    display_name = '참가신청 검토',
    description = '참가신청 승인 및 반려를 처리합니다.',
    ownership_scope = 'CLUB',
    active = 1,
    sort_order = 40,
    update_date = NOW()
WHERE permission_key = 'TOURNAMENT_RECORD_APPLICATION_REVIEW';

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
SELECT 'TOURNAMENT_RECORD_APPLICATION_REVIEW', 'TOURNAMENT_RECORD', '참가신청 검토', '참가신청 승인 및 반려를 처리합니다.', 'CLUB', 1, 40, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TOURNAMENT_RECORD_APPLICATION_REVIEW');

UPDATE feature_permission_catalog
SET
    feature_key = 'TOURNAMENT_RECORD',
    display_name = '엔트리 편성',
    description = '승인된 참가자를 엔트리로 편성합니다.',
    ownership_scope = 'CLUB',
    active = 1,
    sort_order = 50,
    update_date = NOW()
WHERE permission_key = 'TOURNAMENT_RECORD_ENTRY_MANAGE';

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
SELECT 'TOURNAMENT_RECORD_ENTRY_MANAGE', 'TOURNAMENT_RECORD', '엔트리 편성', '승인된 참가자를 엔트리로 편성합니다.', 'CLUB', 1, 50, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TOURNAMENT_RECORD_ENTRY_MANAGE');

UPDATE feature_permission_catalog
SET
    feature_key = 'TOURNAMENT_RECORD',
    display_name = '대진표 관리',
    description = '브래킷 초안 생성, 수정, 확정을 처리합니다.',
    ownership_scope = 'CLUB',
    active = 1,
    sort_order = 60,
    update_date = NOW()
WHERE permission_key = 'TOURNAMENT_RECORD_BRACKET_MANAGE';

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
SELECT 'TOURNAMENT_RECORD_BRACKET_MANAGE', 'TOURNAMENT_RECORD', '대진표 관리', '브래킷 초안 생성, 수정, 확정을 처리합니다.', 'CLUB', 1, 60, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TOURNAMENT_RECORD_BRACKET_MANAGE');

UPDATE feature_permission_catalog
SET
    feature_key = 'TOURNAMENT_RECORD',
    display_name = '대회 삭제',
    description = '운영자 화면에서 대회를 삭제합니다.',
    ownership_scope = 'CLUB',
    active = 1,
    sort_order = 70,
    update_date = NOW()
WHERE permission_key = 'TOURNAMENT_RECORD_DELETE_ANY';

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
SELECT 'TOURNAMENT_RECORD_DELETE_ANY', 'TOURNAMENT_RECORD', '대회 삭제', '운영자 화면에서 대회를 삭제합니다.', 'CLUB', 1, 70, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TOURNAMENT_RECORD_DELETE_ANY');

UPDATE dashboard_widget_catalog
SET
    display_name = 'Tournament Center',
    description = 'Featured tournament and my closest tournament.',
    icon_name = 'emoji_events',
    required_feature_key = 'TOURNAMENT_RECORD',
    default_visibility_scope = 'USER_HOME',
    default_column_span = 2,
    default_row_span = 1,
    default_sort_order = 35,
    active = 1,
    update_date = NOW()
WHERE widget_key = 'TOURNAMENT_RECORD_LATEST';

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
SELECT 'TOURNAMENT_RECORD_LATEST', 'Tournament Center', 'Featured tournament and my closest tournament.', 'emoji_events', 'TOURNAMENT_RECORD', 'USER_HOME', 2, 1, 35, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'TOURNAMENT_RECORD_LATEST');
