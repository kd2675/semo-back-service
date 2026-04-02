-- SEMO member directory rollback SQL for production environments.
-- Run this only when you intentionally want to remove the member directory feature
-- and related dashboard widget from an existing database.

USE SEMO;

-- Remove per-club dashboard widget rows first because they reference dashboard_widget_catalog.
DELETE FROM club_dashboard_widget
WHERE widget_key = 'MEMBER_DIRECTORY_HIGHLIGHT';

-- Remove per-club feature activation rows before deleting feature_catalog.
DELETE FROM feature_activation
WHERE feature_key = 'MEMBER_DIRECTORY';

-- Remove the widget catalog entry introduced by the rollout.
DELETE FROM dashboard_widget_catalog
WHERE widget_key = 'MEMBER_DIRECTORY_HIGHLIGHT';

-- Remove persisted member directory settings.
DROP TABLE IF EXISTS member_directory_setting;

-- Remove the feature catalog entry last.
DELETE FROM feature_catalog
WHERE feature_key = 'MEMBER_DIRECTORY';
