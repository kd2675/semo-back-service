-- SEMO global seed
-- Safe to run multiple times.

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
    'NOTICE',
    '공지관리',
    '공지 콘텐츠를 작성하고 게시판/캘린더에 공유합니다.',
    'campaign',
    1,
    30,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'NOTICE'
);

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
    'ATTENDANCE',
    '출석 체크',
    '멤버 출석을 체크하고 출석 세션을 관리합니다.',
    'fact_check',
    1,
    10,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'ATTENDANCE'
);

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
    'TIMELINE',
    '타임라인',
    '공지 기반 타임라인 카드로 모임 활동을 확인합니다.',
    'timeline',
    1,
    20,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'TIMELINE'
);

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
    'POLL',
    '투표',
    '모임 투표를 작성, 공유, 관리합니다.',
    'poll',
    1,
    40,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'POLL'
);

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
    '일정관리',
    '일정 콘텐츠를 작성하고 게시판/캘린더에 공유합니다.',
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
SELECT 'BOARD_NOTICE', 'Board Notice', 'Latest announcements from your board.', 'forum', NULL, 'USER_HOME', 2, 1, 10, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'BOARD_NOTICE');

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
SELECT 'SCHEDULE_OVERVIEW', 'Schedule Overview', 'Upcoming schedules and next events.', 'calendar_month', NULL, 'USER_HOME', 1, 1, 20, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'SCHEDULE_OVERVIEW');

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
SELECT 'PROFILE_SUMMARY', 'My Profile', 'Quick access to your club profile.', 'person', NULL, 'USER_HOME', 1, 1, 30, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'PROFILE_SUMMARY');

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
SELECT 'ATTENDANCE_STATUS', 'Attendance Check', 'Check in and review attendance status.', 'fact_check', 'ATTENDANCE', 'USER_HOME', 1, 1, 40, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'ATTENDANCE_STATUS');

-- ------------------------------------------------------------
-- Optional example: enable attendance for specific clubs.
-- Uncomment and adjust club ids only when you intentionally want
-- to turn the feature on outside the admin UI.
-- ------------------------------------------------------------
-- INSERT INTO feature_activation (
--     club_id,
--     feature_key,
--     enabled,
--     enabled_by_club_profile_id,
--     enabled_at,
--     create_date,
--     update_date
-- )
-- SELECT
--     c.club_id,
--     'ATTENDANCE',
--     1,
--     cp.club_profile_id,
--     NOW(),
--     NOW(),
--     NOW()
-- FROM club c
-- JOIN club_member cm
--   ON cm.club_id = c.club_id
--  AND cm.role_code = 'OWNER'
--  AND cm.membership_status = 'ACTIVE'
-- JOIN club_profile cp
--   ON cp.club_member_id = cm.club_member_id
-- WHERE c.club_id IN (1)
--   AND NOT EXISTS (
--       SELECT 1
--       FROM feature_activation fa
--       WHERE fa.club_id = c.club_id
--         AND fa.feature_key = 'ATTENDANCE'
--   );
