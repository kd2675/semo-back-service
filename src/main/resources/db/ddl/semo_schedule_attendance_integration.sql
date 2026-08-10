-- 일정 RSVP와 실제 출석을 club_event_participant 단일 원장으로 통합합니다.
-- 적용 전 attendance_session, attendance_checkin, club_attendance_record를 백업하고
-- 아래 마지막 검증 쿼리의 미매핑 건수를 반드시 확인해야 합니다.

ALTER TABLE club_event_participant
    ADD COLUMN attendance_status VARCHAR(20) NULL AFTER participation_status,
    ADD COLUMN verified_by_club_profile_id BIGINT NULL AFTER checked_in_at,
    ADD COLUMN attendance_note VARCHAR(500) NULL AFTER verified_by_club_profile_id;

ALTER TABLE club_event_participant
    ADD CONSTRAINT fk_club_event_participant_verifier
        FOREIGN KEY (verified_by_club_profile_id) REFERENCES club_profile(club_profile_id);

CREATE INDEX idx_club_event_participant_attendance
    ON club_event_participant (event_id, attendance_status);

UPDATE club_event_participant
SET participation_status = 'CANCELED'
WHERE participation_status = 'CANCEL';

INSERT INTO club_event_participant (
    event_id,
    club_profile_id,
    participation_status,
    attendance_status,
    checked_in_at,
    verified_by_club_profile_id,
    attendance_note,
    create_date,
    update_date
)
SELECT
    legacy.event_id,
    legacy.club_profile_id,
    'GOING',
    CASE legacy.status_code
        WHEN 'CHECKED_IN' THEN 'PRESENT'
        WHEN 'PRESENT' THEN 'PRESENT'
        WHEN 'LATE' THEN 'LATE'
        WHEN 'ABSENT' THEN 'ABSENT'
        WHEN 'EXCUSED' THEN 'EXCUSED'
    END,
    CASE
        WHEN legacy.status_code IN ('CHECKED_IN', 'PRESENT', 'LATE') THEN legacy.create_date
        ELSE NULL
    END,
    NULL,
    NULLIF(TRIM(legacy.session_label), ''),
    legacy.create_date,
    legacy.update_date
FROM club_attendance_record legacy
WHERE legacy.event_id IS NOT NULL
  AND legacy.status_code IN ('CHECKED_IN', 'PRESENT', 'LATE', 'ABSENT', 'EXCUSED')
ON DUPLICATE KEY UPDATE
    attendance_status = VALUES(attendance_status),
    checked_in_at = VALUES(checked_in_at),
    attendance_note = COALESCE(VALUES(attendance_note), club_event_participant.attendance_note),
    update_date = VALUES(update_date);

-- 날짜만 가진 기존 세션은 같은 날짜 일정이 하나뿐이어도 자동 연결하지 않습니다.
-- 운영자가 세션과 실제 일정의 관계를 확인한 뒤 아래 임시 매핑에 명시적으로 추가합니다.
CREATE TEMPORARY TABLE semo_attendance_session_event_mapping (
    attendance_session_id BIGINT NOT NULL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    CONSTRAINT uk_semo_attendance_session_event_mapping_event UNIQUE (event_id)
);

-- 예시:
-- INSERT INTO semo_attendance_session_event_mapping (attendance_session_id, event_id)
-- VALUES (3, 14), (5, 16);

-- 클럽이 다른 세션과 일정을 잘못 연결한 매핑이 없는지 먼저 확인합니다. 결과는 0이어야 합니다.
SELECT COUNT(*) AS invalid_session_event_mapping_count
FROM semo_attendance_session_event_mapping mapping
JOIN attendance_session session
  ON session.attendance_session_id = mapping.attendance_session_id
JOIN club_schedule_event event
  ON event.event_id = mapping.event_id
WHERE event.club_id <> session.club_id;

INSERT INTO club_event_participant (
    event_id,
    club_profile_id,
    participation_status,
    attendance_status,
    checked_in_at,
    verified_by_club_profile_id,
    attendance_note,
    create_date,
    update_date
)
SELECT
    mapping.event_id,
    checkin.club_profile_id,
    'GOING',
    CASE checkin.status_code
        WHEN 'CHECKED_IN' THEN 'PRESENT'
        WHEN 'PRESENT' THEN 'PRESENT'
        WHEN 'LATE' THEN 'LATE'
        WHEN 'ABSENT' THEN 'ABSENT'
        WHEN 'EXCUSED' THEN 'EXCUSED'
    END,
    CASE
        WHEN checkin.status_code IN ('CHECKED_IN', 'PRESENT', 'LATE') THEN checkin.checked_in_at
        ELSE NULL
    END,
    NULL,
    NULLIF(TRIM(checkin.note), ''),
    checkin.create_date,
    checkin.update_date
FROM attendance_checkin checkin
JOIN semo_attendance_session_event_mapping mapping
  ON mapping.attendance_session_id = checkin.attendance_session_id
WHERE checkin.status_code IN ('CHECKED_IN', 'PRESENT', 'LATE', 'ABSENT', 'EXCUSED')
ON DUPLICATE KEY UPDATE
    attendance_status = VALUES(attendance_status),
    checked_in_at = VALUES(checked_in_at),
    attendance_note = COALESCE(VALUES(attendance_note), club_event_participant.attendance_note),
    update_date = VALUES(update_date);

-- 이 값이 0이 아니면 매핑이 끝나지 않은 것이므로 레거시 테이블을 제거하지 않습니다.
SELECT COUNT(*) AS unmapped_daily_checkin_count
FROM attendance_checkin checkin
LEFT JOIN semo_attendance_session_event_mapping mapping
  ON mapping.attendance_session_id = checkin.attendance_session_id
WHERE mapping.attendance_session_id IS NULL;

-- 이 값이 0이 아니면 event_id가 없는 레거시 출석을 수동 검토해야 합니다.
SELECT COUNT(*) AS unmapped_legacy_attendance_count
FROM club_attendance_record
WHERE event_id IS NULL;

-- 두 미매핑 결과가 0이고 이관 행을 검증한 뒤에만 레거시 테이블을 제거합니다.
-- DROP TABLE attendance_checkin;
-- DROP TABLE attendance_session;
-- DROP TABLE club_attendance_record;

-- club_member_stat.attendance_rate는 일정 출석 원장에서 계산할 수 있는 중복 파생값입니다.
-- 위 백업과 이관 검증이 끝난 뒤 별도 원장처럼 남지 않도록 수동 제거합니다.
-- ALTER TABLE club_member_stat DROP COLUMN attendance_rate;

DROP TEMPORARY TABLE semo_attendance_session_event_mapping;
