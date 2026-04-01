USE SEMO;

-- ============================================================
-- Semo dues feature seed SQL
-- This rollout has no separate apply SQL because `club_dues_invoice`
-- already exists in the live schema baseline.
-- Run this file only after confirming the target DB already has
-- the `club_dues_invoice` table and related baseline catalogs.
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
    'DUES',
    '회비관리',
    '월별 회비 청구와 납부 현황을 관리합니다.',
    'payments',
    'USER_AND_ADMIN',
    1,
    27,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'DUES'
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
SELECT
    'DUES_VIEW',
    'DUES',
    '회비 조회',
    '회비 운영 화면과 청구 현황을 조회합니다.',
    'CLUB',
    1,
    10,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_permission_catalog
    WHERE permission_key = 'DUES_VIEW'
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
SELECT
    'DUES_ISSUE',
    'DUES',
    '회비 발행',
    '특정 월의 회비 청구서를 일괄 발행합니다.',
    'CLUB',
    1,
    20,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_permission_catalog
    WHERE permission_key = 'DUES_ISSUE'
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
SELECT
    'DUES_MARK_PAID',
    'DUES',
    '회비 납부 처리',
    '회비를 납부 완료 상태로 변경합니다.',
    'CLUB',
    1,
    30,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_permission_catalog
    WHERE permission_key = 'DUES_MARK_PAID'
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
SELECT
    'DUES_MARK_WAIVED',
    'DUES',
    '회비 면제 처리',
    '회비를 면제 상태로 변경합니다.',
    'CLUB',
    1,
    40,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_permission_catalog
    WHERE permission_key = 'DUES_MARK_WAIVED'
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
SELECT
    'DUES_STATUS',
    'Dues Status',
    'My pending dues and latest payment status.',
    'payments',
    'DUES',
    'USER_HOME',
    1,
    1,
    42,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM dashboard_widget_catalog
    WHERE widget_key = 'DUES_STATUS'
);
