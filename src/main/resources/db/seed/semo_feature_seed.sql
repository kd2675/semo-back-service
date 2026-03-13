-- SEMO feature catalog seed
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
    'ATTENDANCE',
    'Attendance Check',
    'Check in members and manage attendance sessions.',
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
