-- SEMO persistent notification inbox apply script.
-- Run against the confirmed SEMO public database after a verified backup.

CREATE TABLE club_notification (
    club_notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    club_id BIGINT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    resource_type VARCHAR(50) NULL,
    resource_id BIGINT NULL,
    target_path VARCHAR(500) NULL,
    event_key VARCHAR(190) NOT NULL,
    read_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_notification_event UNIQUE (event_key),
    CONSTRAINT fk_club_notification_profile FOREIGN KEY (profile_id) REFERENCES profile_user(profile_id),
    CONSTRAINT fk_club_notification_club FOREIGN KEY (club_id) REFERENCES club(club_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_notification_recipient_feed
    ON club_notification (profile_id, club_notification_id);

CREATE INDEX idx_club_notification_recipient_unread
    ON club_notification (profile_id, read_at, club_notification_id);
