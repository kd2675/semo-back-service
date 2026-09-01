-- SEMO mandatory growth core - operational apply and backfill
-- Policy V1: the application recalculates scores from canonical domain rows.
-- Run against the SEMO schema before deploying the application code.

CREATE TABLE IF NOT EXISTS club_growth_core (
    club_id BIGINT PRIMARY KEY,
    tier_level INT NOT NULL DEFAULT 0,
    together_score BIGINT NOT NULL DEFAULT 0,
    operations_score BIGINT NOT NULL DEFAULT 0,
    continuity_score BIGINT NOT NULL DEFAULT 0,
    recent_activity_count INT NOT NULL DEFAULT 0,
    policy_version INT NOT NULL DEFAULT 1,
    last_projected_at DATETIME(6) NULL,
    tier_changed_at DATETIME(6) NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    create_date DATETIME(6) NOT NULL,
    update_date DATETIME(6) NOT NULL,
    CONSTRAINT fk_club_growth_core_club
        FOREIGN KEY (club_id) REFERENCES club(club_id) ON DELETE CASCADE,
    CONSTRAINT chk_club_growth_core_tier
        CHECK (tier_level BETWEEN 0 AND 6),
    CONSTRAINT chk_club_growth_core_scores
        CHECK (together_score >= 0 AND operations_score >= 0 AND continuity_score >= 0),
    INDEX idx_club_growth_core_stale (last_projected_at, club_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Keep an already-created pre-release table aligned with the JPA int field.
ALTER TABLE club_growth_core
    MODIFY COLUMN tier_level INT NOT NULL DEFAULT 0;

INSERT IGNORE INTO club_growth_core (
    club_id,
    tier_level,
    together_score,
    operations_score,
    continuity_score,
    recent_activity_count,
    policy_version,
    last_projected_at,
    tier_changed_at,
    row_version,
    create_date,
    update_date
)
SELECT
    club.club_id,
    0,
    0,
    0,
    0,
    0,
    1,
    NULL,
    NULL,
    0,
    NOW(6),
    NOW(6)
FROM club club;

-- Verification: both values must be 0 after backfill.
SELECT COUNT(*) AS missing_growth_core_count
FROM club club
LEFT JOIN club_growth_core core ON core.club_id = club.club_id
WHERE core.club_id IS NULL;

SELECT COUNT(*) AS orphan_growth_core_count
FROM club_growth_core core
LEFT JOIN club club ON club.club_id = core.club_id
WHERE club.club_id IS NULL;

-- The application reconciler projects existing history in bounded batches.
-- Confirm last_projected_at is populated before treating the backfill as complete.
SELECT
    COUNT(*) AS total_core_count,
    SUM(CASE WHEN last_projected_at IS NULL THEN 1 ELSE 0 END) AS pending_projection_count,
    MIN(last_projected_at) AS oldest_projection_at,
    MAX(last_projected_at) AS newest_projection_at
FROM club_growth_core;

-- Rollback (manual, destructive):
-- DROP TABLE club_growth_core;
