USE SEMO;

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'SCHEDULE_MANAGE',
    'Schedule Management',
    'Create and manage schedules and votes.',
    'edit_calendar',
    1,
    50,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'SCHEDULE_MANAGE'
);
