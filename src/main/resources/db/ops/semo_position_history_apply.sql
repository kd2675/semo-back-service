-- SEMO position tenure history apply script.
-- Run once on existing SEMO databases before enabling position-filtered activity logs.

USE SEMO;

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

INSERT INTO club_member_position_history (
    club_id,
    club_member_id,
    club_profile_id,
    club_position_id,
    position_code_snapshot,
    position_display_name_snapshot,
    started_at,
    ended_at,
    assigned_by_club_profile_id,
    ended_by_club_profile_id,
    deleted,
    create_date,
    update_date
)
SELECT
    cm.club_id,
    cmp.club_member_id,
    cp.club_profile_id,
    cmp.club_position_id,
    pos.position_code,
    pos.display_name,
    COALESCE(cmp.assigned_at, cmp.create_date, NOW()),
    NULL,
    cmp.assigned_by_club_profile_id,
    NULL,
    0,
    NOW(),
    NOW()
FROM club_member_position cmp
JOIN club_member cm
  ON cm.club_member_id = cmp.club_member_id
LEFT JOIN club_profile cp
  ON cp.club_member_id = cmp.club_member_id
JOIN club_position pos
  ON pos.club_position_id = cmp.club_position_id
WHERE NOT EXISTS (
    SELECT 1
    FROM club_member_position_history h
    WHERE h.club_member_id = cmp.club_member_id
      AND h.club_position_id = cmp.club_position_id
      AND h.deleted = 0
      AND h.ended_at IS NULL
);
