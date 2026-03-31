-- Semo tournament-record bracket cleanup
-- Created: 2026-03-31
-- Purpose: remove bracket-manage permission after bracket moves to a separate feature

USE SEMO;

DELETE FROM club_position_permission
WHERE permission_key = 'TOURNAMENT_RECORD_BRACKET_MANAGE';

DELETE FROM feature_permission_catalog
WHERE permission_key = 'TOURNAMENT_RECORD_BRACKET_MANAGE';
