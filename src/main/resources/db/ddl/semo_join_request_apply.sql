-- SEMO join request inbox rollout SQL for existing production environments.
-- Includes:
-- 1. feature_catalog seed for JOIN_REQUEST

USE SEMO;

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
    'JOIN_REQUEST',
    '신규가입',
    '가입 신청 대기열을 보고 승인 흐름을 운영합니다.',
    'group_add',
    'USER_AND_ADMIN',
    1,
    58,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'JOIN_REQUEST'
);
