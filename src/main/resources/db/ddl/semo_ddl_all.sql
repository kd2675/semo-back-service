-- Semo service schema
-- Generated: 2026-03-12

DROP SCHEMA IF EXISTS SEMO;
CREATE SCHEMA SEMO;
USE SEMO;

-- ============================================================
-- User-domain only
-- user_key comes from auth; profile_id is the local identifier used here.
-- Club/activity/feed domains expand from this profile mapping.
-- ============================================================
CREATE TABLE IF NOT EXISTS profile_user (
    profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    tagline VARCHAR(255) NULL,
    profile_color VARCHAR(20) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_profile_user_user_key UNIQUE (user_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_profile_user_display_name
    ON profile_user (display_name);

-- ============================================================
-- Club root
-- ============================================================
CREATE TABLE IF NOT EXISTS club (
    club_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    summary VARCHAR(255) NULL,
    description VARCHAR(2000) NULL,
    category_key VARCHAR(40) NULL,
    activity_category VARCHAR(30) NULL,
    affiliation_type VARCHAR(30) NULL,
    visibility_status VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    membership_policy VARCHAR(20) NOT NULL DEFAULT 'APPROVAL',
    region_scope VARCHAR(20) NOT NULL DEFAULT 'NATIONWIDE',
    region_depth1_code VARCHAR(10) NULL,
    region_depth2_code VARCHAR(10) NULL,
    region_depth1_name VARCHAR(60) NULL,
    region_depth2_name VARCHAR(60) NULL,
    region_label VARCHAR(140) NOT NULL DEFAULT '전국',
    image_file_name VARCHAR(255) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_category_active
    ON club (category_key, active, club_id);

CREATE TABLE IF NOT EXISTS club_activity_tag (
    club_activity_tag_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    tag_key VARCHAR(30) NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_activity_tag UNIQUE (club_id, tag_key),
    CONSTRAINT fk_club_activity_tag_club FOREIGN KEY (club_id) REFERENCES club(club_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_activity_tag_club
    ON club_activity_tag (club_id, tag_key);

-- ============================================================
-- Club feature catalog / activation
-- Features are globally catalogued and enabled per club.
-- User more/admin more menus expand from these rows.
-- ============================================================
CREATE TABLE IF NOT EXISTS feature_catalog (
    feature_key VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    icon_name VARCHAR(50) NOT NULL,
    navigation_scope VARCHAR(20) NOT NULL DEFAULT 'USER_AND_ADMIN',
    active TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS feature_activation (
    feature_activation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    feature_key VARCHAR(50) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    enabled_by_club_profile_id BIGINT NULL,
    enabled_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_feature_activation_key UNIQUE (club_id, feature_key),
    CONSTRAINT fk_feature_activation_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_feature_activation_catalog FOREIGN KEY (feature_key) REFERENCES feature_catalog(feature_key),
    CONSTRAINT fk_feature_activation_enabled_by FOREIGN KEY (enabled_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_feature_activation_enabled
    ON feature_activation (club_id, enabled, feature_key);

CREATE TABLE IF NOT EXISTS club_more_preference (
    club_more_preference_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    feature_key VARCHAR(50) NOT NULL,
    favorite TINYINT(1) NOT NULL DEFAULT 0,
    last_used_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_more_preference UNIQUE (club_id, club_profile_id, feature_key),
    CONSTRAINT fk_club_more_preference_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_more_preference_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_more_preference_feature FOREIGN KEY (feature_key) REFERENCES feature_catalog(feature_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_more_preference_recent
    ON club_more_preference (club_id, club_profile_id, last_used_at, favorite);

CREATE TABLE IF NOT EXISTS feature_permission_catalog (
    permission_key VARCHAR(80) PRIMARY KEY,
    feature_key VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    ownership_scope VARCHAR(20) NOT NULL DEFAULT 'CLUB',
    active TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_feature_permission_catalog_feature FOREIGN KEY (feature_key) REFERENCES feature_catalog(feature_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_feature_permission_catalog_feature
    ON feature_permission_catalog (feature_key, active, sort_order);

CREATE TABLE IF NOT EXISTS club_position (
    club_position_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    position_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    icon_name VARCHAR(50) NULL,
    color_hex VARCHAR(20) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_by_club_profile_id BIGINT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_position_code UNIQUE (club_id, position_code),
    CONSTRAINT fk_club_position_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_position_created_by FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_position_club_active
    ON club_position (club_id, active, display_name);

CREATE TABLE IF NOT EXISTS club_position_permission (
    club_position_permission_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_position_id BIGINT NOT NULL,
    permission_key VARCHAR(80) NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_position_permission UNIQUE (club_position_id, permission_key),
    CONSTRAINT fk_club_position_permission_position FOREIGN KEY (club_position_id) REFERENCES club_position(club_position_id),
    CONSTRAINT fk_club_position_permission_catalog FOREIGN KEY (permission_key) REFERENCES feature_permission_catalog(permission_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_position_permission_position
    ON club_position_permission (club_position_id, permission_key);

CREATE TABLE IF NOT EXISTS club_member_position (
    club_member_position_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_member_id BIGINT NOT NULL,
    club_position_id BIGINT NOT NULL,
    assigned_by_club_profile_id BIGINT NULL,
    assigned_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_member_position UNIQUE (club_member_id, club_position_id),
    CONSTRAINT fk_club_member_position_member FOREIGN KEY (club_member_id) REFERENCES club_member(club_member_id),
    CONSTRAINT fk_club_member_position_position FOREIGN KEY (club_position_id) REFERENCES club_position(club_position_id),
    CONSTRAINT fk_club_member_position_assigned_by FOREIGN KEY (assigned_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_member_position_member
    ON club_member_position (club_member_id, club_position_id);

CREATE TABLE IF NOT EXISTS club_member_position_history (
    club_member_position_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    club_member_id BIGINT NOT NULL,
    club_profile_id BIGINT NULL,
    club_position_id BIGINT NOT NULL,
    position_code_snapshot VARCHAR(50) NOT NULL,
    position_display_name_snapshot VARCHAR(100) NOT NULL,
    started_at DATETIME NOT NULL,
    ended_at DATETIME NULL,
    assigned_by_club_profile_id BIGINT NULL,
    ended_by_club_profile_id BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_by_club_profile_id BIGINT NULL,
    deleted_at DATETIME NULL,
    delete_reason VARCHAR(500) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_club_member_position_history_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_member_position_history_member FOREIGN KEY (club_member_id) REFERENCES club_member(club_member_id),
    CONSTRAINT fk_club_member_position_history_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_member_position_history_assigned_by FOREIGN KEY (assigned_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_member_position_history_ended_by FOREIGN KEY (ended_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_member_position_history_deleted_by FOREIGN KEY (deleted_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_member_position_history_member
    ON club_member_position_history (club_id, club_member_id, deleted, started_at, ended_at);

CREATE INDEX idx_club_member_position_history_position
    ON club_member_position_history (club_id, club_position_id, deleted, started_at, ended_at);

-- ============================================================
-- Club membership / role
-- USER joins club through this table.
-- One profile can have at most one current membership row per club.
-- ============================================================
CREATE TABLE IF NOT EXISTS club_member (
    club_member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    profile_id BIGINT NOT NULL,
    role_code VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    membership_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    join_message VARCHAR(500) NULL,
    invited_by_profile_id BIGINT NULL,
    joined_at DATETIME NULL,
    last_activity_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_member_club_profile UNIQUE (club_id, profile_id),
    CONSTRAINT fk_club_member_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_member_profile FOREIGN KEY (profile_id) REFERENCES profile_user(profile_id),
    CONSTRAINT fk_club_member_invited_by FOREIGN KEY (invited_by_profile_id) REFERENCES profile_user(profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_member_profile_status
    ON club_member (profile_id, membership_status);

CREATE INDEX idx_club_member_club_role
    ON club_member (club_id, role_code, membership_status);

-- ============================================================
-- Club profile
-- Club-scoped identity/profile data lives here.
-- App profile (profile_user) stays global; club activity views can expand from
-- profile_user -> club_member -> club_profile.
-- ============================================================
CREATE TABLE IF NOT EXISTS club_profile (
    club_profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_member_id BIGINT NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    tagline VARCHAR(255) NULL,
    intro_text VARCHAR(1000) NULL,
    avatar_file_name VARCHAR(255) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_profile_member UNIQUE (club_member_id),
    CONSTRAINT fk_club_profile_member FOREIGN KEY (club_member_id) REFERENCES club_member(club_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_profile_display_name
    ON club_profile (display_name);

CREATE TABLE IF NOT EXISTS member_directory_setting (
    member_directory_setting_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    show_positions TINYINT(1) NOT NULL DEFAULT 1,
    show_tagline TINYINT(1) NOT NULL DEFAULT 1,
    show_recent_activity TINYINT(1) NOT NULL DEFAULT 1,
    updated_by_club_profile_id BIGINT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_member_directory_setting_club UNIQUE (club_id),
    CONSTRAINT fk_member_directory_setting_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_member_directory_setting_updated_by FOREIGN KEY (updated_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS club_feedback (
    feedback_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    submitter_club_profile_id BIGINT NOT NULL,
    feedback_type VARCHAR(40) NOT NULL,
    visibility_scope VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    status_code VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    anonymous TINYINT(1) NOT NULL DEFAULT 0,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    admin_answer TEXT NULL,
    answered_by_club_profile_id BIGINT NULL,
    answered_at DATETIME NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_club_feedback_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_feedback_submitter FOREIGN KEY (submitter_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_feedback_answered_by FOREIGN KEY (answered_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_feedback_feed
    ON club_feedback (club_id, deleted, visibility_scope, status_code, create_date, feedback_id);

CREATE INDEX idx_club_feedback_submitter
    ON club_feedback (club_id, submitter_club_profile_id, deleted, create_date, feedback_id);

CREATE TABLE IF NOT EXISTS todo_item (
    todo_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    created_by_club_profile_id BIGINT NOT NULL,
    assigned_club_profile_id BIGINT NULL,
    assigned_by_club_profile_id BIGINT NULL,
    todo_type VARCHAR(30) NOT NULL,
    assignment_mode VARCHAR(30) NOT NULL DEFAULT 'DIRECT_ASSIGN',
    status_code VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    title VARCHAR(150) NOT NULL,
    description VARCHAR(2000) NULL,
    due_at DATETIME NULL,
    completed_by_club_profile_id BIGINT NULL,
    completed_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_todo_item_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_todo_item_created_by FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_todo_item_assigned_profile FOREIGN KEY (assigned_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_todo_item_assigned_by FOREIGN KEY (assigned_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_todo_item_completed_by FOREIGN KEY (completed_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_todo_item_status
    ON todo_item (club_id, status_code, due_at, todo_item_id);

CREATE INDEX idx_todo_item_assignment
    ON todo_item (club_id, assignment_mode, status_code, todo_item_id);

CREATE TABLE IF NOT EXISTS todo_item_application (
    todo_item_application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    todo_item_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    application_status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    application_note VARCHAR(500) NULL,
    review_note VARCHAR(500) NULL,
    reviewed_by_club_profile_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_todo_item_application UNIQUE (todo_item_id, club_profile_id),
    CONSTRAINT fk_todo_item_application_todo FOREIGN KEY (todo_item_id) REFERENCES todo_item(todo_item_id),
    CONSTRAINT fk_todo_item_application_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_todo_item_application_reviewed_by FOREIGN KEY (reviewed_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_todo_item_application_status
    ON todo_item_application (todo_item_id, application_status, create_date, todo_item_application_id);

CREATE INDEX idx_todo_item_application_profile
    ON todo_item_application (club_profile_id, application_status, create_date, todo_item_application_id);

-- ============================================================
-- Join request
-- Supports recommendation/join flows before membership is approved.
-- ============================================================
CREATE TABLE IF NOT EXISTS club_join_request (
    club_join_request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    profile_id BIGINT NOT NULL,
    request_message VARCHAR(500) NULL,
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by_profile_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_join_request_club_profile UNIQUE (club_id, profile_id),
    CONSTRAINT fk_club_join_request_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_join_request_profile FOREIGN KEY (profile_id) REFERENCES profile_user(profile_id),
    CONSTRAINT fk_club_join_request_reviewed_by FOREIGN KEY (reviewed_by_profile_id) REFERENCES profile_user(profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_join_request_status
    ON club_join_request (club_id, request_status, create_date);

-- ============================================================
-- Notice board
-- Club activity records are club-profile scoped.
-- ============================================================
CREATE TABLE IF NOT EXISTS club_notice (
    notice_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    author_club_profile_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    image_file_name VARCHAR(255) NULL,
    location_label VARCHAR(200) NULL,
    schedule_at DATETIME NULL,
    schedule_end_at DATETIME NULL,
    schedule_time_enabled TINYINT(1) NOT NULL DEFAULT 1,
    shared_to_board TINYINT(1) NOT NULL DEFAULT 1,
    shared_to_calendar TINYINT(1) NOT NULL DEFAULT 0,
    pinned TINYINT(1) NOT NULL DEFAULT 0,
    published_at DATETIME NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_club_notice_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_notice_author FOREIGN KEY (author_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_notice_feed
    ON club_notice (club_id, deleted, published_at);

CREATE INDEX idx_club_notice_pinned
    ON club_notice (club_id, deleted, pinned, published_at, notice_id);

CREATE INDEX idx_club_notice_schedule
    ON club_notice (club_id, schedule_at, deleted);

CREATE TABLE IF NOT EXISTS club_board_item (
    board_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    content_type VARCHAR(30) NOT NULL,
    content_id BIGINT NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_board_item UNIQUE (club_id, content_type, content_id),
    CONSTRAINT fk_club_board_item_club FOREIGN KEY (club_id) REFERENCES club(club_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_board_item_feed
    ON club_board_item (club_id, content_type, content_id);

CREATE TABLE IF NOT EXISTS club_calendar_item (
    calendar_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    content_type VARCHAR(30) NOT NULL,
    content_id BIGINT NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_calendar_item UNIQUE (club_id, content_type, content_id),
    CONSTRAINT fk_club_calendar_item_club FOREIGN KEY (club_id) REFERENCES club(club_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_calendar_item_feed
    ON club_calendar_item (club_id, content_type, content_id);

CREATE TABLE IF NOT EXISTS club_board_item_read (
    club_board_item_read_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    board_item_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    first_read_at DATETIME NOT NULL,
    last_read_at DATETIME NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_board_item_read UNIQUE (board_item_id, club_profile_id),
    CONSTRAINT fk_club_board_item_read_item FOREIGN KEY (board_item_id) REFERENCES club_board_item(board_item_id),
    CONSTRAINT fk_club_board_item_read_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_board_item_read_item
    ON club_board_item_read (board_item_id, club_profile_id);

-- ============================================================
-- Tournament
-- Tournament data is club-scoped.
-- ============================================================
CREATE TABLE IF NOT EXISTS tournament_record (
    tournament_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    author_club_profile_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary_text VARCHAR(500) NULL,
    detail_text TEXT NULL,
    tournament_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by_club_profile_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    rejection_reason VARCHAR(500) NULL,
    application_start_at DATETIME NOT NULL,
    application_end_at DATETIME NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    location_label VARCHAR(200) NULL,
    match_format VARCHAR(20) NOT NULL DEFAULT 'SINGLE',
    team_member_limit INT NULL,
    participant_limit INT NULL,
    fee_required TINYINT(1) NOT NULL DEFAULT 0,
    fee_amount INT NULL,
    fee_currency_code VARCHAR(10) NOT NULL DEFAULT 'KRW',
    shared_to_board TINYINT(1) NOT NULL DEFAULT 0,
    shared_to_calendar TINYINT(1) NOT NULL DEFAULT 0,
    pinned TINYINT(1) NOT NULL DEFAULT 0,
    cancelled_at DATETIME NULL,
    cancel_reason VARCHAR(500) NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_tournament_record_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_tournament_record_author FOREIGN KEY (author_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_tournament_record_reviewed_by FOREIGN KEY (reviewed_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_tournament_record_home
    ON tournament_record (club_id, deleted, pinned, start_date, tournament_record_id);

CREATE INDEX idx_tournament_record_calendar
    ON tournament_record (club_id, deleted, start_date, end_date);

CREATE INDEX idx_tournament_record_approval
    ON tournament_record (club_id, deleted, approval_status, create_date);

CREATE TABLE IF NOT EXISTS tournament_application (
    tournament_application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_record_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    application_status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    application_note VARCHAR(500) NULL,
    reviewed_by_club_profile_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_tournament_application UNIQUE (tournament_record_id, club_profile_id),
    CONSTRAINT fk_tournament_application_tournament FOREIGN KEY (tournament_record_id) REFERENCES tournament_record(tournament_record_id),
    CONSTRAINT fk_tournament_application_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_tournament_application_reviewed_by FOREIGN KEY (reviewed_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_tournament_application_status
    ON tournament_application (tournament_record_id, application_status, create_date);

-- ============================================================
-- Bracket
-- User-created bracket drafts can optionally import approved tournament
-- participants, then submit for admin approval.
-- ============================================================
CREATE TABLE IF NOT EXISTS bracket_record (
    bracket_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    author_club_profile_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary_text VARCHAR(500) NULL,
    bracket_type VARCHAR(30) NOT NULL DEFAULT 'SINGLE_ELIMINATION',
    participant_type VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    source_type VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    source_tournament_record_id BIGINT NULL,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    reviewed_by_club_profile_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    rejection_reason VARCHAR(500) NULL,
    participant_count INT NOT NULL DEFAULT 0,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_bracket_record_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_bracket_record_author FOREIGN KEY (author_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_bracket_record_reviewed_by FOREIGN KEY (reviewed_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_bracket_record_tournament FOREIGN KEY (source_tournament_record_id) REFERENCES tournament_record(tournament_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_bracket_record_approval
    ON bracket_record (club_id, deleted, approval_status, create_date);

CREATE INDEX idx_bracket_record_author
    ON bracket_record (club_id, author_club_profile_id, deleted, create_date);

CREATE TABLE IF NOT EXISTS bracket_participant (
    bracket_participant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bracket_record_id BIGINT NOT NULL,
    seed_number INT NOT NULL,
    club_profile_id BIGINT NULL,
    display_name VARCHAR(100) NOT NULL,
    participant_role VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
    entry_source_type VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    source_tournament_application_id BIGINT NULL,
    guest_entry TINYINT(1) NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_bracket_participant_seed UNIQUE (bracket_record_id, seed_number),
    CONSTRAINT fk_bracket_participant_record FOREIGN KEY (bracket_record_id) REFERENCES bracket_record(bracket_record_id),
    CONSTRAINT fk_bracket_participant_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_bracket_participant_application FOREIGN KEY (source_tournament_application_id) REFERENCES tournament_application(tournament_application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_bracket_participant_record
    ON bracket_participant (bracket_record_id, seed_number, bracket_participant_id);

-- ============================================================
-- Schedule / events
-- Event authors and participants are also club-profile scoped.
-- ============================================================
CREATE TABLE IF NOT EXISTS club_schedule_event (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    author_club_profile_id BIGINT NOT NULL,
    linked_notice_id BIGINT NULL,
    shared_to_board TINYINT(1) NOT NULL DEFAULT 0,
    shared_to_calendar TINYINT(1) NOT NULL DEFAULT 1,
    pinned TINYINT(1) NOT NULL DEFAULT 0,
    category_key VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NULL,
    location_label VARCHAR(200) NULL,
    participation_condition_text VARCHAR(1000) NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NULL,
    attendee_limit INT NULL,
    participation_enabled TINYINT(1) NOT NULL DEFAULT 0,
    fee_required TINYINT(1) NOT NULL DEFAULT 0,
    fee_amount INT NULL,
    fee_amount_undecided TINYINT(1) NOT NULL DEFAULT 0,
    fee_n_way_split TINYINT(1) NOT NULL DEFAULT 0,
    visibility_status VARCHAR(20) NOT NULL DEFAULT 'CLUB',
    event_status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_club_schedule_event_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_schedule_event_author FOREIGN KEY (author_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_schedule_event_notice FOREIGN KEY (linked_notice_id) REFERENCES club_notice(notice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_schedule_event_club_start
    ON club_schedule_event (club_id, start_at);

CREATE INDEX idx_club_schedule_event_status
    ON club_schedule_event (club_id, event_status, start_at);

CREATE TABLE IF NOT EXISTS club_event_participant (
    club_event_participant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    participation_status VARCHAR(20) NOT NULL DEFAULT 'GOING',
    attendance_status VARCHAR(20) NULL,
    checked_in_at DATETIME NULL,
    verified_by_club_profile_id BIGINT NULL,
    attendance_note VARCHAR(500) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_event_participant_event_profile UNIQUE (event_id, club_profile_id),
    CONSTRAINT fk_club_event_participant_event FOREIGN KEY (event_id) REFERENCES club_schedule_event(event_id),
    CONSTRAINT fk_club_event_participant_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_event_participant_verifier FOREIGN KEY (verified_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_event_participant_profile
    ON club_event_participant (club_profile_id, participation_status);

CREATE INDEX idx_club_event_participant_attendance
    ON club_event_participant (event_id, attendance_status);

CREATE TABLE IF NOT EXISTS club_schedule_vote (
    vote_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    author_club_profile_id BIGINT NOT NULL,
    linked_notice_id BIGINT NULL,
    shared_to_board TINYINT(1) NOT NULL DEFAULT 0,
    shared_to_calendar TINYINT(1) NOT NULL DEFAULT 0,
    pinned TINYINT(1) NOT NULL DEFAULT 0,
    title VARCHAR(200) NOT NULL,
    vote_start_date DATE NOT NULL,
    vote_end_date DATE NOT NULL,
    vote_start_time TIME NULL,
    vote_end_time TIME NULL,
    closed_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_club_schedule_vote_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_schedule_vote_author FOREIGN KEY (author_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_schedule_vote_notice FOREIGN KEY (linked_notice_id) REFERENCES club_notice(notice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_schedule_vote_club
    ON club_schedule_vote (club_id, create_date);

CREATE TABLE IF NOT EXISTS club_schedule_vote_option (
    vote_option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vote_id BIGINT NOT NULL,
    option_label VARCHAR(120) NOT NULL,
    sort_order INT NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_club_schedule_vote_option_vote FOREIGN KEY (vote_id) REFERENCES club_schedule_vote(vote_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_schedule_vote_option_vote
    ON club_schedule_vote_option (vote_id, sort_order);

CREATE TABLE IF NOT EXISTS club_schedule_vote_selection (
    vote_selection_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vote_id BIGINT NOT NULL,
    vote_option_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_schedule_vote_selection_vote_profile UNIQUE (vote_id, club_profile_id),
    CONSTRAINT fk_club_schedule_vote_selection_vote FOREIGN KEY (vote_id) REFERENCES club_schedule_vote(vote_id),
    CONSTRAINT fk_club_schedule_vote_selection_option FOREIGN KEY (vote_option_id) REFERENCES club_schedule_vote_option(vote_option_id),
    CONSTRAINT fk_club_schedule_vote_selection_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_schedule_vote_selection_vote
    ON club_schedule_vote_selection (vote_id, vote_option_id);

-- ============================================================
-- Member stats / profile widgets
-- Dashboard cards, top rankings, profile summary all expand from club_profile.
-- ============================================================
CREATE TABLE IF NOT EXISTS club_member_stat (
    club_member_stat_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    ranking_points INT NOT NULL DEFAULT 0,
    rank_position INT NULL,
    matches_played INT NOT NULL DEFAULT 0,
    wins INT NOT NULL DEFAULT 0,
    losses INT NOT NULL DEFAULT 0,
    win_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    streak_count INT NOT NULL DEFAULT 0,
    best_streak_count INT NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_member_stat_club_profile UNIQUE (club_id, club_profile_id),
    CONSTRAINT fk_club_member_stat_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_member_stat_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_member_stat_rank
    ON club_member_stat (club_id, rank_position, ranking_points);

CREATE TABLE IF NOT EXISTS finance_obligation (
    finance_obligation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    created_by_club_profile_id BIGINT NULL,
    obligation_type_code VARCHAR(30) NOT NULL DEFAULT 'FEE',
    title VARCHAR(150) NOT NULL,
    target_scope_code VARCHAR(30) NOT NULL DEFAULT 'ALL_ACTIVE_MEMBERS',
    amount DECIMAL(12,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL DEFAULT 'KRW',
    due_at DATETIME NULL,
    status_code VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    note VARCHAR(500) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_finance_obligation_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_finance_obligation_created_by FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_finance_obligation_club_sort
    ON finance_obligation (club_id, create_date, finance_obligation_id);

CREATE INDEX idx_finance_obligation_status
    ON finance_obligation (club_id, status_code, finance_obligation_id);

CREATE TABLE IF NOT EXISTS finance_payment (
    finance_payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    finance_obligation_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL DEFAULT 'KRW',
    payment_status_code VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at DATETIME NULL,
    note VARCHAR(500) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_finance_payment_obligation_profile UNIQUE (finance_obligation_id, club_profile_id),
    CONSTRAINT fk_finance_payment_obligation FOREIGN KEY (finance_obligation_id) REFERENCES finance_obligation(finance_obligation_id),
    CONSTRAINT fk_finance_payment_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_finance_payment_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_finance_payment_status
    ON finance_payment (club_id, payment_status_code, finance_obligation_id);

CREATE TABLE IF NOT EXISTS finance_request (
    finance_request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    requester_club_profile_id BIGINT NOT NULL,
    request_type_code VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL DEFAULT 'KRW',
    related_event_name VARCHAR(120) NULL,
    note VARCHAR(1000) NULL,
    status_code VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    reviewed_by_club_profile_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    review_note VARCHAR(1000) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_finance_request_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_finance_request_requester FOREIGN KEY (requester_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_finance_request_reviewed_by FOREIGN KEY (reviewed_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_finance_request_club_status
    ON finance_request (club_id, status_code, finance_request_id);

CREATE INDEX idx_finance_request_requester_status
    ON finance_request (requester_club_profile_id, status_code, finance_request_id);

CREATE TABLE IF NOT EXISTS finance_expense (
    finance_expense_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    entered_by_club_profile_id BIGINT NOT NULL,
    source_finance_request_id BIGINT NULL,
    expense_type_code VARCHAR(30) NOT NULL,
    category_code VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL DEFAULT 'KRW',
    spent_at DATETIME NOT NULL,
    related_event_name VARCHAR(120) NULL,
    note VARCHAR(1000) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_finance_expense_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_finance_expense_entered_by FOREIGN KEY (entered_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_finance_expense_source_request FOREIGN KEY (source_finance_request_id) REFERENCES finance_request(finance_request_id),
    CONSTRAINT uk_finance_expense_source_request UNIQUE (source_finance_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_finance_expense_club_spent
    ON finance_expense (club_id, spent_at, finance_expense_id);

-- ============================================================
-- Dashboard widget catalog / layout
-- Widget catalog is global and activated/ordered per club.
-- USER_HOME widgets can be edited by admin and exposed to all members.
-- ============================================================
CREATE TABLE IF NOT EXISTS dashboard_widget_catalog (
    widget_key VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    icon_name VARCHAR(50) NOT NULL,
    required_feature_key VARCHAR(50) NULL,
    default_visibility_scope VARCHAR(20) NOT NULL DEFAULT 'USER_HOME',
    default_column_span INT NOT NULL DEFAULT 1,
    default_row_span INT NOT NULL DEFAULT 1,
    default_sort_order INT NOT NULL DEFAULT 0,
    active TINYINT(1) NOT NULL DEFAULT 1,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_dashboard_widget_required_feature
      FOREIGN KEY (required_feature_key) REFERENCES feature_catalog(feature_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS club_dashboard_widget (
    club_dashboard_widget_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    widget_key VARCHAR(50) NOT NULL,
    title_override VARCHAR(100) NULL,
    column_span INT NOT NULL DEFAULT 1,
    row_span INT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    visibility_scope VARCHAR(20) NOT NULL DEFAULT 'USER_HOME',
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_dashboard_widget_key UNIQUE (club_id, widget_key),
    CONSTRAINT fk_club_dashboard_widget_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_dashboard_widget_catalog FOREIGN KEY (widget_key) REFERENCES dashboard_widget_catalog(widget_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_dashboard_widget_sort
    ON club_dashboard_widget (club_id, visibility_scope, enabled, sort_order);

-- ============================================================
-- Club activity log
-- Recent activity is stored as an append-only stream for admin home
-- and future audit-style views. Actor is the user who triggered the
-- action, detail is a human-readable sentence, and status stores outcome.
-- ============================================================
CREATE TABLE IF NOT EXISTS club_activity_log (
    club_activity_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    actor_club_member_id BIGINT NULL,
    actor_club_profile_id BIGINT NULL,
    actor_display_name VARCHAR(100) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    detail_text VARCHAR(500) NOT NULL,
    status_code VARCHAR(20) NOT NULL,
    error_message VARCHAR(500) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_club_activity_log_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_activity_log_member FOREIGN KEY (actor_club_member_id) REFERENCES club_member(club_member_id),
    CONSTRAINT fk_club_activity_log_profile FOREIGN KEY (actor_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_activity_log_recent
    ON club_activity_log (club_id, create_date, club_activity_log_id);

CREATE INDEX idx_club_activity_log_status
    ON club_activity_log (club_id, status_code, create_date);
