-- SEMO feedback rollout SQL for existing production environments.
-- Includes:
-- 1. club_feedback table
-- 2. feature_catalog seed for FEEDBACK

USE SEMO;

CREATE TABLE IF NOT EXISTS club_feedback (
    feedback_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    submitter_club_profile_id BIGINT NOT NULL,
    feedback_type VARCHAR(40) NOT NULL,
    visibility_scope VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    status_code VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    anonymous BIT(1) NOT NULL DEFAULT b'0',
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    admin_answer TEXT NULL,
    answered_by_club_profile_id BIGINT NULL,
    answered_at DATETIME NULL,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_club_feedback_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_feedback_submitter FOREIGN KEY (submitter_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_feedback_answered_by FOREIGN KEY (answered_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_feedback_feed
    ON club_feedback (club_id, deleted, visibility_scope, status_code, create_date, feedback_id);

CREATE INDEX idx_club_feedback_submitter
    ON club_feedback (club_id, submitter_club_profile_id, deleted, create_date, feedback_id);

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
    'FEEDBACK',
    '피드백',
    '익명 또는 기명으로 건의, 불편 신고, 개선 요청을 남기고 운영 답변을 확인합니다.',
    'forum',
    'USER_AND_ADMIN',
    1,
    59,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'FEEDBACK'
);
