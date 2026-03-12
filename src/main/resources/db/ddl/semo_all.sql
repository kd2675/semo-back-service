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
    slug VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    short_code VARCHAR(20) NOT NULL,
    summary VARCHAR(255) NULL,
    description VARCHAR(2000) NULL,
    category_key VARCHAR(40) NULL,
    visibility_status VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    membership_policy VARCHAR(20) NOT NULL DEFAULT 'APPROVAL',
    cover_image_url VARCHAR(2048) NULL,
    profile_image_url VARCHAR(2048) NULL,
    primary_color VARCHAR(20) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_category_active
    ON club (category_key, active, club_id);

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
-- ============================================================
CREATE TABLE IF NOT EXISTS club_notice (
    notice_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    author_profile_id BIGINT NOT NULL,
    category_key VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    pinned TINYINT(1) NOT NULL DEFAULT 0,
    published_at DATETIME NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_club_notice_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_notice_author FOREIGN KEY (author_profile_id) REFERENCES profile_user(profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_notice_feed
    ON club_notice (club_id, category_key, deleted, published_at);

CREATE INDEX idx_club_notice_pinned
    ON club_notice (club_id, pinned, published_at);

-- ============================================================
-- Schedule / events
-- ============================================================
CREATE TABLE IF NOT EXISTS club_schedule_event (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    author_profile_id BIGINT NOT NULL,
    category_key VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NULL,
    location_label VARCHAR(200) NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NULL,
    attendee_limit INT NULL,
    visibility_status VARCHAR(20) NOT NULL DEFAULT 'CLUB',
    event_status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_club_schedule_event_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_schedule_event_author FOREIGN KEY (author_profile_id) REFERENCES profile_user(profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_schedule_event_club_start
    ON club_schedule_event (club_id, start_at);

CREATE INDEX idx_club_schedule_event_status
    ON club_schedule_event (club_id, event_status, start_at);

CREATE TABLE IF NOT EXISTS club_event_participant (
    club_event_participant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    profile_id BIGINT NOT NULL,
    participation_status VARCHAR(20) NOT NULL DEFAULT 'GOING',
    checked_in_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_event_participant_event_profile UNIQUE (event_id, profile_id),
    CONSTRAINT fk_club_event_participant_event FOREIGN KEY (event_id) REFERENCES club_schedule_event(event_id),
    CONSTRAINT fk_club_event_participant_profile FOREIGN KEY (profile_id) REFERENCES profile_user(profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_event_participant_profile
    ON club_event_participant (profile_id, participation_status);

-- ============================================================
-- Member stats / profile widgets
-- Dashboard cards, top rankings, profile summary all expand from here.
-- ============================================================
CREATE TABLE IF NOT EXISTS club_member_stat (
    club_member_stat_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    profile_id BIGINT NOT NULL,
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
    CONSTRAINT uk_club_member_stat_club_profile UNIQUE (club_id, profile_id),
    CONSTRAINT fk_club_member_stat_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_member_stat_profile FOREIGN KEY (profile_id) REFERENCES profile_user(profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_member_stat_rank
    ON club_member_stat (club_id, rank_position, ranking_points);

CREATE TABLE IF NOT EXISTS club_attendance_record (
    club_attendance_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    profile_id BIGINT NOT NULL,
    event_id BIGINT NULL,
    attendance_date DATE NOT NULL,
    status_code VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    session_label VARCHAR(100) NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_attendance_record UNIQUE (club_id, profile_id, attendance_date, session_label),
    CONSTRAINT fk_club_attendance_record_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_attendance_record_profile FOREIGN KEY (profile_id) REFERENCES profile_user(profile_id),
    CONSTRAINT fk_club_attendance_record_event FOREIGN KEY (event_id) REFERENCES club_schedule_event(event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_attendance_record_profile_date
    ON club_attendance_record (profile_id, attendance_date);

CREATE TABLE IF NOT EXISTS club_dues_invoice (
    club_dues_invoice_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    profile_id BIGINT NOT NULL,
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
    CONSTRAINT uk_club_dues_invoice_period UNIQUE (club_id, profile_id, billing_year, billing_month),
    CONSTRAINT fk_club_dues_invoice_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_dues_invoice_profile FOREIGN KEY (profile_id) REFERENCES profile_user(profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_dues_invoice_status
    ON club_dues_invoice (club_id, billing_year, billing_month, payment_status);

-- ============================================================
-- Admin dashboard layout
-- Each club can customize widget visibility/order for admin view.
-- ============================================================
CREATE TABLE IF NOT EXISTS club_dashboard_widget (
    club_dashboard_widget_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    widget_key VARCHAR(50) NOT NULL,
    title_override VARCHAR(100) NULL,
    column_span INT NOT NULL DEFAULT 1,
    row_span INT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    visibility_scope VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_dashboard_widget_key UNIQUE (club_id, widget_key),
    CONSTRAINT fk_club_dashboard_widget_club FOREIGN KEY (club_id) REFERENCES club(club_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_dashboard_widget_sort
    ON club_dashboard_widget (club_id, visibility_scope, enabled, sort_order);
