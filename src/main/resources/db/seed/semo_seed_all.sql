-- SEMO global seed
-- Safe to run multiple times.

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
    'NOTICE',
    '공지관리',
    '공지 콘텐츠를 작성하고 게시판/캘린더에 공유합니다.',
    'campaign',
    'USER_AND_ADMIN',
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
    navigation_scope,
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
    'USER_AND_ADMIN',
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
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'TIMELINE',
    '타임라인',
    '모임 전체 활동을 시간순 타임라인으로 확인합니다.',
    'timeline',
    'USER_AND_ADMIN',
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
    navigation_scope,
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
    'USER_AND_ADMIN',
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
    navigation_scope,
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
    'USER_AND_ADMIN',
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
SELECT 'TIMELINE_VIEW', 'TIMELINE', '타임라인 조회', '타임라인 화면을 확인합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TIMELINE_VIEW');

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
SELECT 'POLL_STATUS', 'Poll Status', 'Latest ongoing poll for your club.', 'poll', 'POLL', 'USER_HOME', 1, 1, 25, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'POLL_STATUS');

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
