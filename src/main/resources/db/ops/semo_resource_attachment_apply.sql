-- SEMO common resource attachment apply script.
-- Run against the confirmed SEMO public database after a verified backup.

CREATE TABLE resource_attachment (
    resource_attachment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    resource_type VARCHAR(40) NOT NULL,
    resource_id BIGINT NOT NULL,
    uploader_club_profile_id BIGINT NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,
    visibility_scope VARCHAR(30) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_by_club_profile_id BIGINT NULL,
    deleted_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_resource_attachment_file UNIQUE (file_name),
    CONSTRAINT fk_resource_attachment_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_resource_attachment_uploader FOREIGN KEY (uploader_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_resource_attachment_deleter FOREIGN KEY (deleted_by_club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_resource_attachment_target
    ON resource_attachment (club_id, resource_type, resource_id, deleted, resource_attachment_id);

CREATE INDEX idx_resource_attachment_uploader
    ON resource_attachment (uploader_club_profile_id, deleted, resource_attachment_id);
