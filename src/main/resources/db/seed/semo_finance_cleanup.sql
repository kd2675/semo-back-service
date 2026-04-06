-- ============================================================
-- Semo finance migration cleanup script
-- Run this only after semo_finance_apply.sql has been applied
-- and finance screens / APIs are verified in the target environment.
-- This permanently removes legacy DUES rows/tables. After this,
-- semo_finance_apply.sql can bootstrap FINANCE again but cannot
-- reconstruct deleted DUES activations, permissions, widgets, or data.
-- ============================================================

USE SEMO;

DELETE FROM club_dashboard_widget
WHERE widget_key = 'DUES_STATUS';

DELETE FROM dashboard_widget_catalog
WHERE widget_key = 'DUES_STATUS';

DELETE FROM club_position_permission
WHERE permission_key IN (
    'DUES_VIEW',
    'DUES_ISSUE',
    'DUES_MARK_PAID',
    'DUES_MARK_WAIVED'
);

DELETE FROM feature_permission_catalog
WHERE permission_key IN (
    'DUES_VIEW',
    'DUES_ISSUE',
    'DUES_MARK_PAID',
    'DUES_MARK_WAIVED'
);

DELETE FROM feature_activation
WHERE feature_key = 'DUES';

DELETE FROM feature_catalog
WHERE feature_key = 'DUES';

DROP TABLE IF EXISTS dues_invoice;
DROP TABLE IF EXISTS dues_charge;
