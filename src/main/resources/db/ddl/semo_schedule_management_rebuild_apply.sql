USE SEMO;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_event'
          AND COLUMN_NAME = 'participation_condition_text'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_event ADD COLUMN participation_condition_text VARCHAR(1000) NULL AFTER location_label'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_event'
          AND COLUMN_NAME = 'participation_enabled'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_event ADD COLUMN participation_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER attendee_limit'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_event'
          AND COLUMN_NAME = 'fee_required'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_event ADD COLUMN fee_required TINYINT(1) NOT NULL DEFAULT 0 AFTER participation_enabled'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_event'
          AND COLUMN_NAME = 'fee_n_way_split'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_event ADD COLUMN fee_n_way_split TINYINT(1) NOT NULL DEFAULT 0 AFTER fee_required'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS club_event_participant (
    club_event_participant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    participation_status VARCHAR(20) NOT NULL DEFAULT 'GOING',
    checked_in_at DATETIME NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_club_event_participant_event_profile UNIQUE (event_id, club_profile_id),
    CONSTRAINT fk_club_event_participant_event FOREIGN KEY (event_id) REFERENCES club_schedule_event(event_id),
    CONSTRAINT fk_club_event_participant_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_event_participant'
          AND INDEX_NAME = 'idx_club_event_participant_profile'
    ),
    'SELECT 1',
    'CREATE INDEX idx_club_event_participant_profile ON club_event_participant (club_profile_id, participation_status)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_club_schedule_vote_club FOREIGN KEY (club_id) REFERENCES club(club_id),
    CONSTRAINT fk_club_schedule_vote_author FOREIGN KEY (author_club_profile_id) REFERENCES club_profile(club_profile_id),
    CONSTRAINT fk_club_schedule_vote_notice FOREIGN KEY (linked_notice_id) REFERENCES club_notice(notice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND INDEX_NAME = 'idx_club_schedule_vote_club'
    ),
    'SELECT 1',
    'CREATE INDEX idx_club_schedule_vote_club ON club_schedule_vote (club_id, create_date)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'vote_start_date'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_vote ADD COLUMN vote_start_date DATE NULL AFTER title'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'vote_end_date'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_vote ADD COLUMN vote_end_date DATE NULL AFTER vote_start_date'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'vote_date'
    ),
    'UPDATE club_schedule_vote SET vote_start_date = COALESCE(vote_start_date, vote_date) WHERE vote_start_date IS NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE club_schedule_vote
SET vote_start_date = DATE(create_date)
WHERE vote_start_date IS NULL;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'vote_start_date'
          AND IS_NULLABLE = 'YES'
    ),
    'ALTER TABLE club_schedule_vote MODIFY COLUMN vote_start_date DATE NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE club_schedule_vote
SET vote_end_date = vote_start_date
WHERE vote_end_date IS NULL;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'vote_end_date'
          AND IS_NULLABLE = 'YES'
    ),
    'ALTER TABLE club_schedule_vote MODIFY COLUMN vote_end_date DATE NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'vote_start_time'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_vote ADD COLUMN vote_start_time TIME NULL AFTER vote_end_date'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'vote_end_time'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_vote ADD COLUMN vote_end_time TIME NULL AFTER vote_start_time'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote'
          AND COLUMN_NAME = 'closed_at'
    ),
    'SELECT 1',
    'ALTER TABLE club_schedule_vote ADD COLUMN closed_at DATETIME NULL AFTER vote_end_time'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS club_schedule_vote_option (
    vote_option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vote_id BIGINT NOT NULL,
    option_label VARCHAR(120) NOT NULL,
    sort_order INT NOT NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_club_schedule_vote_option_vote FOREIGN KEY (vote_id) REFERENCES club_schedule_vote(vote_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote_option'
          AND INDEX_NAME = 'idx_club_schedule_vote_option_vote'
    ),
    'SELECT 1',
    'CREATE INDEX idx_club_schedule_vote_option_vote ON club_schedule_vote_option (vote_id, sort_order)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS club_schedule_vote_selection (
    vote_selection_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vote_id BIGINT NOT NULL,
    vote_option_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_club_schedule_vote_selection_vote_profile UNIQUE (vote_id, club_profile_id),
    CONSTRAINT fk_club_schedule_vote_selection_vote FOREIGN KEY (vote_id) REFERENCES club_schedule_vote(vote_id),
    CONSTRAINT fk_club_schedule_vote_selection_option FOREIGN KEY (vote_option_id) REFERENCES club_schedule_vote_option(vote_option_id),
    CONSTRAINT fk_club_schedule_vote_selection_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = 'SEMO'
          AND TABLE_NAME = 'club_schedule_vote_selection'
          AND INDEX_NAME = 'idx_club_schedule_vote_selection_vote'
    ),
    'SELECT 1',
    'CREATE INDEX idx_club_schedule_vote_selection_vote ON club_schedule_vote_selection (vote_id, vote_option_id)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
