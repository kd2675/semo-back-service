-- Apply once to an existing SEMO database before deploying persistent More favorites and recent usage.
CREATE TABLE club_more_preference (
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
