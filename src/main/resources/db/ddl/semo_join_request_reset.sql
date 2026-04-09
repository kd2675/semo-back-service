-- SEMO join request reset SQL.
-- Intended for pre-launch cleanup when no real applicant history must be kept.

USE SEMO;

DELETE FROM club_join_request;
ALTER TABLE club_join_request AUTO_INCREMENT = 1;
