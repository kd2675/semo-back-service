-- Reset semo domain data only.
-- Safe to run multiple times.

USE SEMO;

SET FOREIGN_KEY_CHECKS = 0;

-- keep profile_user mapping (user_key <-> profile_id)
TRUNCATE TABLE club_dashboard_widget;
TRUNCATE TABLE club_dues_invoice;
TRUNCATE TABLE attendance_checkin;
TRUNCATE TABLE attendance_session;
TRUNCATE TABLE club_attendance_record;
TRUNCATE TABLE club_member_stat;
TRUNCATE TABLE club_event_participant;
TRUNCATE TABLE club_schedule_event;
TRUNCATE TABLE club_notice;
TRUNCATE TABLE club_join_request;
TRUNCATE TABLE feature_activation;
TRUNCATE TABLE club_profile;
TRUNCATE TABLE club_member;
TRUNCATE TABLE club;

SET FOREIGN_KEY_CHECKS = 1;
