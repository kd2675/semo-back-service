-- SEMO member directory rollout SQL for existing production environments.
-- Includes:
-- 1. member_directory_setting table
-- 2. feature_catalog seed for MEMBER_DIRECTORY
-- 3. dashboard_widget_catalog seed for MEMBER_DIRECTORY_HIGHLIGHT

USE SEMO;

CREATE TABLE IF NOT EXISTS member_directory_setting (
    member_directory_setting_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    show_positions BIT(1) NOT NULL DEFAULT b'1',
    show_tagline BIT(1) NOT NULL DEFAULT b'1',
    show_recent_activity BIT(1) NOT NULL DEFAULT b'1',
    updated_by_club_profile_id BIGINT NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_member_directory_setting_club UNIQUE (club_id),
    CONSTRAINT fk_member_directory_setting_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_member_directory_setting_updated_by FOREIGN KEY (updated_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    'MEMBER_DIRECTORY',
    '회원 디렉터리',
    '다른 회원의 직책, 한줄소개, 최근 활동을 한 화면에서 조회합니다.',
    'group_search',
    'USER_AND_ADMIN',
    1,
    59,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'MEMBER_DIRECTORY'
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
    'MEMBER_DIRECTORY_HIGHLIGHT',
    'Member Directory',
    'Recently active members and quick access to the member directory.',
    'group_search',
    'MEMBER_DIRECTORY',
    'USER_HOME',
    1,
    1,
    38,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM dashboard_widget_catalog
    WHERE widget_key = 'MEMBER_DIRECTORY_HIGHLIGHT'
);
