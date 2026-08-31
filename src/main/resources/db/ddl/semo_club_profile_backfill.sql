-- GET 요청에서 club_profile을 생성하던 과거 동작을 제거하기 위한 데이터 보정입니다.
-- 적용 중에는 가입, 가입 승인, 멤버 상태 변경 등 club_member 쓰기를 중지해야 합니다.
-- 첫 조회로 보정 범위를 기록하고, 마지막 누락 건수가 0인지 확인한 뒤 애플리케이션을 배포합니다.

SELECT COUNT(*) AS missing_club_profile_count_before
FROM club_member cm
JOIN profile_user pu
    ON pu.profile_id = cm.profile_id
LEFT JOIN club_profile cp
    ON cp.club_member_id = cm.club_member_id
WHERE cm.membership_status IN ('ACTIVE', 'DORMANT')
  AND cp.club_profile_id IS NULL;

START TRANSACTION;

INSERT INTO club_profile (
    club_member_id,
    display_name,
    tagline,
    intro_text,
    avatar_file_name,
    create_date,
    update_date
)
SELECT
    cm.club_member_id,
    COALESCE(NULLIF(TRIM(pu.display_name), ''), 'SEMO Member'),
    NULLIF(TRIM(pu.tagline), ''),
    NULL,
    NULL,
    NOW(),
    NOW()
FROM club_member cm
JOIN profile_user pu
    ON pu.profile_id = cm.profile_id
LEFT JOIN club_profile cp
    ON cp.club_member_id = cm.club_member_id
WHERE cm.membership_status IN ('ACTIVE', 'DORMANT')
  AND cp.club_profile_id IS NULL;

SELECT ROW_COUNT() AS inserted_club_profile_count;

COMMIT;

SELECT COUNT(*) AS missing_club_profile_count_after
FROM club_member cm
JOIN profile_user pu
    ON pu.profile_id = cm.profile_id
LEFT JOIN club_profile cp
    ON cp.club_member_id = cm.club_member_id
WHERE cm.membership_status IN ('ACTIVE', 'DORMANT')
  AND cp.club_profile_id IS NULL;
