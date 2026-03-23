-- Semo legacy permission policy cleanup script
-- Created: 2026-03-23
-- Purpose: archive and drop legacy club-level permission policy tables
--
-- IMPORTANT
--   1. Execute this only after the backend code no longer maps:
--      - notice_permission_policy
--      - schedule_permission_policy
--      - poll_permission_policy
--   2. Current semo-back-service uses spring.jpa.hibernate.ddl-auto=validate,
--      so dropping these tables before code cleanup will break application startup.
--   3. Run during a maintenance window after confirming no runtime path depends on these tables.

USE SEMO;

CREATE TABLE IF NOT EXISTS notice_permission_policy_legacy_20260323 LIKE notice_permission_policy;
INSERT INTO notice_permission_policy_legacy_20260323
SELECT *
FROM notice_permission_policy source
WHERE NOT EXISTS (
    SELECT 1
    FROM notice_permission_policy_legacy_20260323 backup
    WHERE backup.notice_permission_policy_id = source.notice_permission_policy_id
);

CREATE TABLE IF NOT EXISTS schedule_permission_policy_legacy_20260323 LIKE schedule_permission_policy;
INSERT INTO schedule_permission_policy_legacy_20260323
SELECT *
FROM schedule_permission_policy source
WHERE NOT EXISTS (
    SELECT 1
    FROM schedule_permission_policy_legacy_20260323 backup
    WHERE backup.schedule_permission_policy_id = source.schedule_permission_policy_id
);

CREATE TABLE IF NOT EXISTS poll_permission_policy_legacy_20260323 LIKE poll_permission_policy;
INSERT INTO poll_permission_policy_legacy_20260323
SELECT *
FROM poll_permission_policy source
WHERE NOT EXISTS (
    SELECT 1
    FROM poll_permission_policy_legacy_20260323 backup
    WHERE backup.poll_permission_policy_id = source.poll_permission_policy_id
);

DROP TABLE IF EXISTS notice_permission_policy;
DROP TABLE IF EXISTS schedule_permission_policy;
DROP TABLE IF EXISTS poll_permission_policy;
