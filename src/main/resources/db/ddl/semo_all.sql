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
    visibility_status VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    membership_policy VARCHAR(20) NOT NULL DEFAULT 'APPROVAL',
    image_file_name VARCHAR(255) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_category_active
    ON club (category_key, active, club_id);

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

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    active,
    sort_order,
    create_date,
    update_date
) VALUES (
    'ATTENDANCE',
    'Attendance Check',
    'Check in members and manage attendance sessions.',
    'fact_check',
    1,
    10,
    NOW(),
    NOW()
);

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
    category_key VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    location_label VARCHAR(200) NULL,
    schedule_at DATETIME NULL,
    schedule_end_at DATETIME NULL,
    pinned TINYINT(1) NOT NULL DEFAULT 0,
    published_at DATETIME NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_club_notice_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_notice_author FOREIGN KEY (author_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_notice_feed
    ON club_notice (club_id, category_key, deleted, published_at);

CREATE INDEX idx_club_notice_pinned
    ON club_notice (club_id, pinned, published_at);

CREATE INDEX idx_club_notice_schedule
    ON club_notice (club_id, schedule_at, deleted);

-- ============================================================
-- Schedule / events
-- Event authors and participants are also club-profile scoped.
-- ============================================================
CREATE TABLE IF NOT EXISTS club_schedule_event (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    author_club_profile_id BIGINT NOT NULL,
    linked_notice_id BIGINT NULL,
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
    checked_in_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_event_participant_event_profile UNIQUE (event_id, club_profile_id),
    CONSTRAINT fk_club_event_participant_event FOREIGN KEY (event_id) REFERENCES club_schedule_event(event_id),
    CONSTRAINT fk_club_event_participant_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_event_participant_profile
    ON club_event_participant (club_profile_id, participation_status);

CREATE TABLE IF NOT EXISTS club_schedule_vote (
    vote_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    author_club_profile_id BIGINT NOT NULL,
    linked_notice_id BIGINT NULL,
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
-- Attendance feature
-- Attendance is the first real club feature. Sessions are club-scoped and
-- individual check-ins are club-profile scoped.
-- ============================================================
CREATE TABLE IF NOT EXISTS attendance_session (
    attendance_session_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    created_by_club_profile_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    attendance_date DATE NOT NULL,
    open_at DATETIME NOT NULL,
    close_at DATETIME NULL,
    session_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_attendance_session_day UNIQUE (club_id, attendance_date),
    CONSTRAINT fk_attendance_session_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_attendance_session_created_by FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_attendance_session_status
    ON attendance_session (club_id, session_status, attendance_date);

CREATE TABLE IF NOT EXISTS attendance_checkin (
    attendance_checkin_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attendance_session_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    status_code VARCHAR(20) NOT NULL DEFAULT 'CHECKED_IN',
    checked_in_at DATETIME NOT NULL,
    note VARCHAR(255) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_attendance_checkin UNIQUE (attendance_session_id, club_profile_id),
    CONSTRAINT fk_attendance_checkin_session FOREIGN KEY (attendance_session_id) REFERENCES attendance_session(attendance_session_id),
    CONSTRAINT fk_attendance_checkin_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_attendance_checkin_profile
    ON attendance_checkin (club_profile_id, checked_in_at);

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
    attendance_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_member_stat_club_profile UNIQUE (club_id, club_profile_id),
    CONSTRAINT fk_club_member_stat_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_member_stat_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_member_stat_rank
    ON club_member_stat (club_id, rank_position, ranking_points);

CREATE TABLE IF NOT EXISTS club_attendance_record (
    club_attendance_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    event_id BIGINT NULL,
    attendance_date DATE NOT NULL,
    status_code VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    session_label VARCHAR(100) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_attendance_record UNIQUE (club_id, club_profile_id, attendance_date, session_label),
    CONSTRAINT fk_club_attendance_record_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_attendance_record_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_attendance_record_event FOREIGN KEY (event_id) REFERENCES club_schedule_event(event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_attendance_record_profile_date
    ON club_attendance_record (club_profile_id, attendance_date);

CREATE TABLE IF NOT EXISTS club_dues_invoice (
    club_dues_invoice_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    billing_year SMALLINT NOT NULL,
    billing_month TINYINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL DEFAULT 'KRW',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    due_at DATETIME NULL,
    paid_at DATETIME NULL,
    note VARCHAR(500) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_dues_invoice_period UNIQUE (club_id, club_profile_id, billing_year, billing_month),
    CONSTRAINT fk_club_dues_invoice_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_dues_invoice_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_dues_invoice_status
    ON club_dues_invoice (club_id, billing_year, billing_month, payment_status);

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
