-- SEMO tournament operations migration.
-- Prerequisites:
--   1. Confirm the target is the SEMO public database and take a verified backup.
--   2. Apply semo_finance_operations_apply.sql first when tournament fees will use finance periods/accounts.
-- This is a one-time migration. Do not rerun after it succeeds.

USE SEMO;

ALTER TABLE tournament_application
    ADD COLUMN team_name VARCHAR(100) NULL AFTER application_note,
    ADD COLUMN waitlist_position INT NULL AFTER team_name,
    ADD COLUMN finance_payment_id BIGINT NULL AFTER waitlist_position,
    ADD COLUMN checked_in_at DATETIME NULL AFTER finance_payment_id,
    ADD COLUMN checked_in_by_club_profile_id BIGINT NULL AFTER checked_in_at,
    ADD COLUMN placement INT NULL AFTER checked_in_by_club_profile_id,
    ADD COLUMN result_note VARCHAR(1000) NULL AFTER placement,
    ADD CONSTRAINT fk_tournament_application_checked_in_by
        FOREIGN KEY (checked_in_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    ADD CONSTRAINT fk_tournament_application_finance_payment
        FOREIGN KEY (finance_payment_id) REFERENCES finance_payment(finance_payment_id),
    ADD CONSTRAINT uk_tournament_application_finance_payment UNIQUE (finance_payment_id);

CREATE INDEX idx_tournament_application_waitlist
    ON tournament_application (
        tournament_record_id,
        application_status,
        waitlist_position,
        tournament_application_id
    );

CREATE TABLE tournament_roster_member (
    tournament_roster_member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_application_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    roster_role_code VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    sort_order INT NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_tournament_roster_application_profile
        UNIQUE (tournament_application_id, club_profile_id),
    CONSTRAINT fk_tournament_roster_application
        FOREIGN KEY (tournament_application_id) REFERENCES tournament_application(tournament_application_id),
    CONSTRAINT fk_tournament_roster_profile
        FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id),
    KEY idx_tournament_roster_application (
        tournament_application_id,
        sort_order,
        tournament_roster_member_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO tournament_roster_member
    (
        tournament_application_id,
        club_profile_id,
        roster_role_code,
        sort_order,
        create_date,
        update_date
    )
SELECT
    application.tournament_application_id,
    application.club_profile_id,
    'CAPTAIN',
    0,
    NOW(),
    NOW()
FROM tournament_application application;

CREATE TABLE tournament_schedule_slot (
    tournament_schedule_slot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_record_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    court_label VARCHAR(100) NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    note VARCHAR(500) NULL,
    created_by_club_profile_id BIGINT NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_tournament_schedule_record
        FOREIGN KEY (tournament_record_id) REFERENCES tournament_record(tournament_record_id),
    CONSTRAINT fk_tournament_schedule_created_by
        FOREIGN KEY (created_by_club_profile_id) REFERENCES club_profile(club_profile_id),
    KEY idx_tournament_schedule_record (
        tournament_record_id,
        start_at,
        tournament_schedule_slot_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE finance_obligation
    ADD COLUMN source_tournament_application_id BIGINT NULL AFTER recurrence_source_finance_obligation_id,
    ADD CONSTRAINT fk_finance_obligation_tournament_application
        FOREIGN KEY (source_tournament_application_id) REFERENCES tournament_application(tournament_application_id),
    ADD CONSTRAINT uk_finance_obligation_tournament_application UNIQUE (source_tournament_application_id);

-- Post-apply verification. The result must contain all seven application columns,
-- both new tables, and the finance source column with its unique constraint.
SELECT table_name, column_name
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'tournament_application' AND column_name IN (
        'team_name', 'waitlist_position', 'finance_payment_id', 'checked_in_at',
        'checked_in_by_club_profile_id', 'placement', 'result_note'
      ))
    OR (table_name = 'finance_obligation' AND column_name = 'source_tournament_application_id'))
ORDER BY table_name, column_name;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('tournament_roster_member', 'tournament_schedule_slot')
ORDER BY table_name;

SELECT table_name, constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE constraint_schema = DATABASE()
  AND constraint_name IN (
      'uk_tournament_application_finance_payment',
      'uk_tournament_roster_application_profile',
      'uk_finance_obligation_tournament_application'
  )
ORDER BY constraint_name;
