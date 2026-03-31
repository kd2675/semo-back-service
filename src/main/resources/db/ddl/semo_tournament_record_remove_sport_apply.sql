-- Semo tournament-record remove-sport apply script
-- Created: 2026-03-25
-- Purpose: migrate existing tournament schema from sport-based to sportless tournaments
-- Notes:
--   1. Run after tournament feature is already present in production DB
--   2. Safe to run multiple times

USE SEMO;

SET @has_fk_tournament_record_sport := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tournament_record'
      AND CONSTRAINT_NAME = 'fk_tournament_record_sport'
);
SET @drop_fk_tournament_record_sport_sql := IF(
    @has_fk_tournament_record_sport = 1,
    'ALTER TABLE tournament_record DROP FOREIGN KEY fk_tournament_record_sport',
    'SELECT 1'
);
PREPARE drop_fk_tournament_record_sport_stmt FROM @drop_fk_tournament_record_sport_sql;
EXECUTE drop_fk_tournament_record_sport_stmt;
DEALLOCATE PREPARE drop_fk_tournament_record_sport_stmt;

SET @has_sport_key := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tournament_record'
      AND COLUMN_NAME = 'sport_key'
);
SET @drop_sport_key_sql := IF(
    @has_sport_key = 1,
    'ALTER TABLE tournament_record DROP COLUMN sport_key',
    'SELECT 1'
);
PREPARE drop_sport_key_stmt FROM @drop_sport_key_sql;
EXECUTE drop_sport_key_stmt;
DEALLOCATE PREPARE drop_sport_key_stmt;

SET @has_sport_catalog := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sport_catalog'
);
SET @drop_sport_catalog_sql := IF(
    @has_sport_catalog = 1,
    'DROP TABLE sport_catalog',
    'SELECT 1'
);
PREPARE drop_sport_catalog_stmt FROM @drop_sport_catalog_sql;
EXECUTE drop_sport_catalog_stmt;
DEALLOCATE PREPARE drop_sport_catalog_stmt;
