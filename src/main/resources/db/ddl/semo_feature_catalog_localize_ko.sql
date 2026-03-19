USE SEMO;

UPDATE feature_catalog
SET
    display_name = '출석 체크',
    description = '멤버 출석을 체크하고 출석 세션을 관리합니다.',
    update_date = NOW()
WHERE feature_key = 'ATTENDANCE';

UPDATE feature_catalog
SET
    display_name = '타임라인',
    description = '공지 기반 타임라인 카드로 모임 활동을 확인합니다.',
    update_date = NOW()
WHERE feature_key = 'TIMELINE';

UPDATE feature_catalog
SET
    display_name = '공지',
    description = '모임 공지를 작성, 관리, 공유합니다.',
    update_date = NOW()
WHERE feature_key = 'NOTICE';

UPDATE feature_catalog
SET
    display_name = '투표',
    description = '모임 투표를 작성, 공유, 관리합니다.',
    update_date = NOW()
WHERE feature_key = 'POLL';

UPDATE feature_catalog
SET
    display_name = '일정 관리',
    description = '일정과 투표를 작성하고 관리합니다.',
    update_date = NOW()
WHERE feature_key = 'SCHEDULE_MANAGE';
