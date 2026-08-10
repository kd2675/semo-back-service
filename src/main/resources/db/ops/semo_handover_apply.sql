-- SEMO operating term and handover center migration.
-- Run against the confirmed SEMO public database after a verified backup.

USE SEMO;

INSERT INTO feature_catalog (
    feature_key, display_name, description, icon_name, navigation_scope,
    active, sort_order, create_date, update_date
)
SELECT
    'HANDOVER', '인수인계 센터',
    '운영 임기, 집행부 구성, 미완료 업무와 다음 담당자 메모를 한곳에서 관리합니다.',
    'move_up', 'ADMIN_ONLY', 1, 95, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_catalog WHERE feature_key = 'HANDOVER');

UPDATE feature_catalog
SET display_name = '인수인계 센터',
    description = '운영 임기, 집행부 구성, 미완료 업무와 다음 담당자 메모를 한곳에서 관리합니다.',
    icon_name = 'move_up', navigation_scope = 'ADMIN_ONLY', active = 1,
    sort_order = 95, update_date = NOW()
WHERE feature_key = 'HANDOVER';

INSERT INTO feature_permission_catalog (
    permission_key, feature_key, display_name, description, ownership_scope,
    active, sort_order, create_date, update_date
)
SELECT
    'HANDOVER_VIEW', 'HANDOVER', '인수인계 조회',
    '운영 임기, 집행부, 업무 큐와 인수인계 메모를 조회합니다.',
    'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'HANDOVER_VIEW');

INSERT INTO feature_permission_catalog (
    permission_key, feature_key, display_name, description, ownership_scope,
    active, sort_order, create_date, update_date
)
SELECT
    'HANDOVER_MANAGE', 'HANDOVER', '인수인계 관리',
    '운영 임기와 집행부 구성을 관리하고 인수인계 메모를 작성합니다.',
    'CLUB', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'HANDOVER_MANAGE');

CREATE TABLE IF NOT EXISTS club_operating_term (
    club_operating_term_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    term_name VARCHAR(100) NOT NULL,
    term_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status_code VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    description VARCHAR(1000) NULL,
    created_by_club_profile_id BIGINT NOT NULL,
    activated_by_club_profile_id BIGINT NULL,
    activated_at DATETIME NULL,
    closed_by_club_profile_id BIGINT NULL,
    closed_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_operating_term_name_start UNIQUE (club_id, term_name, start_date),
    CONSTRAINT fk_club_operating_term_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_operating_term_created_by FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_operating_term_activated_by FOREIGN KEY (activated_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_operating_term_closed_by FOREIGN KEY (closed_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    KEY idx_club_operating_term_status (club_id, status_code, start_date, club_operating_term_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS club_term_executive_assignment (
    club_term_executive_assignment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    club_operating_term_id BIGINT NOT NULL,
    club_member_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    club_position_id BIGINT NOT NULL,
    responsibility VARCHAR(1000) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_by_club_profile_id BIGINT NOT NULL,
    updated_by_club_profile_id BIGINT NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_term_executive_member_position UNIQUE (club_operating_term_id, club_member_id, club_position_id),
    CONSTRAINT fk_club_term_executive_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_term_executive_term FOREIGN KEY (club_operating_term_id) REFERENCES club_operating_term(club_operating_term_id),
    CONSTRAINT fk_club_term_executive_member FOREIGN KEY (club_member_id) REFERENCES club_member(club_member_id),
    CONSTRAINT fk_club_term_executive_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_term_executive_position FOREIGN KEY (club_position_id) REFERENCES club_position(club_position_id),
    CONSTRAINT fk_club_term_executive_created_by FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_term_executive_updated_by FOREIGN KEY (updated_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    KEY idx_club_term_executive_roster (club_operating_term_id, sort_order, club_term_executive_assignment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS club_term_carryover_item (
    club_term_carryover_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    from_term_id BIGINT NOT NULL,
    to_term_id BIGINT NOT NULL,
    resource_type VARCHAR(40) NOT NULL,
    resource_id BIGINT NOT NULL,
    title_snapshot VARCHAR(200) NOT NULL,
    status_snapshot VARCHAR(50) NOT NULL,
    target_path VARCHAR(500) NOT NULL,
    due_at DATETIME NULL,
    status_code VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    transferred_by_club_profile_id BIGINT NOT NULL,
    transferred_at DATETIME NOT NULL,
    resolved_by_club_profile_id BIGINT NULL,
    resolved_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_term_carryover_resource UNIQUE (to_term_id, resource_type, resource_id),
    CONSTRAINT fk_club_term_carryover_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_term_carryover_from_term FOREIGN KEY (from_term_id) REFERENCES club_operating_term(club_operating_term_id),
    CONSTRAINT fk_club_term_carryover_to_term FOREIGN KEY (to_term_id) REFERENCES club_operating_term(club_operating_term_id),
    CONSTRAINT fk_club_term_carryover_transferred_by FOREIGN KEY (transferred_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_term_carryover_resolved_by FOREIGN KEY (resolved_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    KEY idx_club_term_carryover_status (club_id, to_term_id, status_code, due_at, club_term_carryover_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS club_handover_note (
    club_handover_note_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    from_term_id BIGINT NULL,
    to_term_id BIGINT NULL,
    club_position_id BIGINT NULL,
    assigned_club_profile_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status_code VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    due_at DATETIME NULL,
    created_by_club_profile_id BIGINT NOT NULL,
    acknowledged_by_club_profile_id BIGINT NULL,
    acknowledged_at DATETIME NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_by_club_profile_id BIGINT NULL,
    deleted_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_club_handover_note_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_handover_note_from_term FOREIGN KEY (from_term_id) REFERENCES club_operating_term(club_operating_term_id),
    CONSTRAINT fk_club_handover_note_to_term FOREIGN KEY (to_term_id) REFERENCES club_operating_term(club_operating_term_id),
    CONSTRAINT fk_club_handover_note_position FOREIGN KEY (club_position_id) REFERENCES club_position(club_position_id),
    CONSTRAINT fk_club_handover_note_assignee FOREIGN KEY (assigned_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_handover_note_created_by FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_handover_note_acknowledged_by FOREIGN KEY (acknowledged_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_handover_note_deleted_by FOREIGN KEY (deleted_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    KEY idx_club_handover_note_feed (club_id, deleted, status_code, due_at, club_handover_note_id),
    KEY idx_club_handover_note_terms (club_id, from_term_id, to_term_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
