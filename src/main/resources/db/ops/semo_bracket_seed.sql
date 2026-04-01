USE SEMO;

-- ============================================================
-- Semo bracket feature seed SQL
-- Run after: semo_bracket_apply.sql
-- ============================================================

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
    'BRACKET',
    '대진표',
    '직접 작성하거나 대회 참가자를 불러와 대진표 초안을 만들고 관리자 승인을 받습니다.',
    'account_tree',
    'USER_AND_ADMIN',
    1,
    57,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'BRACKET'
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
SELECT 'BRACKET_CREATE', 'BRACKET', '대진표 작성', '대진표 초안을 새로 생성합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'BRACKET_CREATE');

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
SELECT 'BRACKET_UPDATE_SELF', 'BRACKET', '대진표 수정', '본인이 작성한 대진표 초안을 수정합니다.', 'SELF', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'BRACKET_UPDATE_SELF');

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
SELECT 'BRACKET_REVIEW', 'BRACKET', '대진표 승인', '제출된 대진표를 승인 또는 반려합니다.', 'CLUB', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'BRACKET_REVIEW');

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
SELECT 'BRACKET_DELETE_ANY', 'BRACKET', '대진표 삭제', '운영자 화면에서 대진표를 삭제합니다.', 'CLUB', 1, 40, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'BRACKET_DELETE_ANY');

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
SELECT
    'BRACKET_LATEST',
    'Bracket Board',
    'Approved brackets and my latest draft.',
    'account_tree',
    'BRACKET',
    'USER_HOME',
    1,
    1,
    37,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM dashboard_widget_catalog
    WHERE widget_key = 'BRACKET_LATEST'
);
