-- SEMO global seed
-- Safe to run multiple times.

USE SEMO;

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'JOIN_REQUEST',
    '가입 신청',
    '관리자가 가입 신청 대기열을 검토하고 승인 또는 반려합니다.',
    'group_add',
    'ADMIN_ONLY',
    1,
    10,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'JOIN_REQUEST'
);

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'NOTICE',
    '게시판 공지',
    '대표 게시판에서 공지를 작성하고 공유 범위를 관리합니다.',
    'campaign',
    'USER_AND_ADMIN',
    1,
    20,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'NOTICE'
);

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'ATTENDANCE',
    '일정 참석',
    '대표 캘린더의 일정별 참가 응답과 참석 현황을 관리합니다.',
    'fact_check',
    'USER_AND_ADMIN',
    1,
    45,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'ATTENDANCE'
);

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'TIMELINE',
    '내 활동',
    '멤버는 자신의 활동을 확인하고 운영진은 감사 로그에서 전체 활동을 조회합니다.',
    'timeline',
    'USER_AND_ADMIN',
    1,
    110,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'TIMELINE'
);

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'POLL',
    '투표',
    '대표 캘린더에서 투표를 작성하고 응답 결과를 관리합니다.',
    'poll',
    'USER_AND_ADMIN',
    1,
    40,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'POLL'
);

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'SCHEDULE_MANAGE',
    '일정',
    '대표 캘린더에서 일정을 작성하고 참가 응답을 관리합니다.',
    'edit_calendar',
    'USER_AND_ADMIN',
    1,
    30,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'SCHEDULE_MANAGE'
);

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'TOURNAMENT_RECORD',
    '대회 운영',
    '대회 등록, 승인, 참가 신청과 참가자 확정을 한 흐름에서 운영합니다.',
    'emoji_events',
    'USER_AND_ADMIN',
    1,
    50,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'TOURNAMENT_RECORD'
);

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'BRACKET',
    '대진표 초안',
    '대회 참가자를 불러와 표준 시드 배치 초안을 만들고 관리자 승인을 받습니다.',
    'account_tree',
    'USER_AND_ADMIN',
    1,
    51,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'BRACKET'
);

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'FINANCE',
    '회비·정산',
    '회비 수납, 멤버 요청, 승인 지출과 지출 원장을 분리해 관리합니다.',
    'payments',
    'USER_AND_ADMIN',
    1,
    60,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'FINANCE'
);

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'ROLE_MANAGEMENT',
    '직책관리',
    '직책을 생성하고 하위 권한을 연결해 멤버 권한을 세밀하게 관리합니다.',
    'manage_accounts',
    'ADMIN_ONLY',
    1,
    100,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'ROLE_MANAGEMENT'
);

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'MEMBER_DIRECTORY',
    '멤버·조직',
    '멤버가 공개한 프로필과 직책을 조회하며 최근 활동은 기본 비공개로 보호합니다.',
    'group_search',
    'USER_AND_ADMIN',
    1,
    80,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'MEMBER_DIRECTORY'
);

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'FEEDBACK',
    '피드백',
    '비공개로 건의와 불편 신고를 접수하고 운영 답변을 확인합니다.',
    'forum',
    'USER_AND_ADMIN',
    1,
    90,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'FEEDBACK'
);

INSERT INTO feature_catalog (
    feature_key,
    display_name,
    description,
    icon_name,
    navigation_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT
    'TODO',
    '할 일',
    '담당자와 지원 가능 업무를 명확하게 관리하고 완료 상태를 추적합니다.',
    'assignment',
    'USER_AND_ADMIN',
    1,
    70,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM feature_catalog
    WHERE feature_key = 'TODO'
);

-- Keep existing installations aligned with the current product navigation contract.
UPDATE feature_catalog
SET display_name = '가입 신청', description = '관리자가 가입 신청 대기열을 검토하고 승인 또는 반려합니다.', navigation_scope = 'ADMIN_ONLY', sort_order = 10, update_date = NOW()
WHERE feature_key = 'JOIN_REQUEST';
UPDATE feature_catalog
SET display_name = '게시판 공지', description = '대표 게시판에서 공지를 작성하고 공유 범위를 관리합니다.', sort_order = 20, update_date = NOW()
WHERE feature_key = 'NOTICE';
UPDATE feature_catalog
SET display_name = '일정', description = '대표 캘린더에서 일정을 작성하고 참가 응답을 관리합니다.', sort_order = 30, update_date = NOW()
WHERE feature_key = 'SCHEDULE_MANAGE';
UPDATE feature_catalog
SET display_name = '투표', description = '대표 캘린더에서 투표를 작성하고 응답 결과를 관리합니다.', sort_order = 40, update_date = NOW()
WHERE feature_key = 'POLL';
UPDATE feature_catalog
SET display_name = '일정 참석', description = '대표 캘린더의 일정별 참가 응답과 참석 현황을 관리합니다.', sort_order = 45, update_date = NOW()
WHERE feature_key = 'ATTENDANCE';
UPDATE feature_catalog
SET display_name = '대회 운영', description = '대회 등록, 승인, 참가 신청과 참가자 확정을 한 흐름에서 운영합니다.', sort_order = 50, update_date = NOW()
WHERE feature_key = 'TOURNAMENT_RECORD';
UPDATE feature_catalog
SET display_name = '대진표 초안', description = '대회 참가자를 불러와 표준 시드 배치 초안을 만들고 관리자 승인을 받습니다.', sort_order = 51, update_date = NOW()
WHERE feature_key = 'BRACKET';
UPDATE feature_catalog
SET display_name = '회비·정산', description = '회비 수납, 멤버 요청, 승인 지출과 지출 원장을 분리해 관리합니다.', sort_order = 60, update_date = NOW()
WHERE feature_key = 'FINANCE';
UPDATE feature_catalog SET sort_order = 70, update_date = NOW() WHERE feature_key = 'TODO';
UPDATE feature_catalog
SET display_name = '멤버·조직', description = '멤버가 공개한 프로필과 직책을 조회하며 최근 활동은 기본 비공개로 보호합니다.', sort_order = 80, update_date = NOW()
WHERE feature_key = 'MEMBER_DIRECTORY';
UPDATE feature_catalog
SET description = '비공개로 건의와 불편 신고를 접수하고 운영 답변을 확인합니다.', sort_order = 90, update_date = NOW()
WHERE feature_key = 'FEEDBACK';
UPDATE feature_catalog SET sort_order = 100, update_date = NOW() WHERE feature_key = 'ROLE_MANAGEMENT';
UPDATE feature_catalog
SET display_name = '내 활동', description = '멤버는 자신의 활동을 확인하고 운영진은 감사 로그에서 전체 활동을 조회합니다.', sort_order = 110, update_date = NOW()
WHERE feature_key = 'TIMELINE';

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'NOTICE_CREATE', 'NOTICE', '공지 작성', '공지 콘텐츠를 새로 작성합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'NOTICE_CREATE');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'NOTICE_UPDATE_SELF', 'NOTICE', '공지 수정', '본인이 작성한 공지를 수정합니다.', 'SELF', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'NOTICE_UPDATE_SELF');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'NOTICE_DELETE_SELF', 'NOTICE', '공지 삭제', '본인이 작성한 공지를 삭제합니다.', 'SELF', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'NOTICE_DELETE_SELF');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'POLL_CREATE', 'POLL', '투표 작성', '투표를 새로 생성합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'POLL_CREATE');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'POLL_UPDATE_SELF', 'POLL', '투표 수정', '본인이 작성한 투표를 수정합니다.', 'SELF', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'POLL_UPDATE_SELF');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'POLL_DELETE_SELF', 'POLL', '투표 삭제', '본인이 작성한 투표를 삭제합니다.', 'SELF', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'POLL_DELETE_SELF');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'SCHEDULE_CREATE', 'SCHEDULE_MANAGE', '일정 작성', '일정을 새로 생성합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'SCHEDULE_CREATE');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'SCHEDULE_UPDATE_SELF', 'SCHEDULE_MANAGE', '일정 수정', '본인이 작성한 일정을 수정합니다.', 'SELF', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'SCHEDULE_UPDATE_SELF');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'SCHEDULE_DELETE_SELF', 'SCHEDULE_MANAGE', '일정 삭제', '본인이 작성한 일정을 삭제합니다.', 'SELF', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'SCHEDULE_DELETE_SELF');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'TOURNAMENT_RECORD_CREATE', 'TOURNAMENT_RECORD', '대회 작성', '대회를 새로 생성합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TOURNAMENT_RECORD_CREATE');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'TOURNAMENT_RECORD_UPDATE_SELF', 'TOURNAMENT_RECORD', '대회 수정', '본인이 작성한 대회를 수정합니다.', 'SELF', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TOURNAMENT_RECORD_UPDATE_SELF');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'TOURNAMENT_RECORD_PIN', 'TOURNAMENT_RECORD', '대회 고정', '대회를 게시판 상단에 고정합니다.', 'SELF', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TOURNAMENT_RECORD_PIN');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'TOURNAMENT_RECORD_REVIEW', 'TOURNAMENT_RECORD', '대회 승인 검토', '작성된 대회를 승인 또는 거절합니다.', 'CLUB', 1, 40, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TOURNAMENT_RECORD_REVIEW');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'TOURNAMENT_RECORD_DELETE_ANY', 'TOURNAMENT_RECORD', '대회 삭제', '운영자 화면에서 대회를 삭제합니다.', 'CLUB', 1, 50, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TOURNAMENT_RECORD_DELETE_ANY');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'BRACKET_CREATE', 'BRACKET', '대진표 작성', '대진표 초안을 새로 생성합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'BRACKET_CREATE');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'BRACKET_UPDATE_SELF', 'BRACKET', '대진표 수정', '본인이 작성한 대진표 초안을 수정합니다.', 'SELF', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'BRACKET_UPDATE_SELF');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'BRACKET_REVIEW', 'BRACKET', '대진표 승인', '제출된 대진표를 승인 또는 반려합니다.', 'CLUB', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'BRACKET_REVIEW');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'BRACKET_DELETE_ANY', 'BRACKET', '대진표 삭제', '운영자 화면에서 대진표를 삭제합니다.', 'CLUB', 1, 40, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'BRACKET_DELETE_ANY');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'FINANCE_VIEW', 'FINANCE', '재정 조회', '재정 운영 화면과 납부 현황을 조회합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_VIEW');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'FINANCE_ISSUE', 'FINANCE', '재정 항목 발행', '재정 항목을 생성하고 대상 멤버에게 발행합니다.', 'CLUB', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_ISSUE');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'FINANCE_MARK_PAID', 'FINANCE', '재정 납부 처리', '재정 항목을 납부 완료 상태로 변경합니다.', 'CLUB', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_MARK_PAID');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'FINANCE_MARK_WAIVED', 'FINANCE', '재정 면제 처리', '재정 항목을 면제 상태로 변경합니다.', 'CLUB', 1, 40, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_MARK_WAIVED');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'TIMELINE_VIEW', 'TIMELINE', '타임라인 조회', '타임라인 화면을 확인합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TIMELINE_VIEW');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'TODO_VIEW', 'TODO', '할 일 조회', '할 일 운영 화면과 담당 현황을 조회합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TODO_VIEW');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'TODO_CREATE', 'TODO', '할 일 생성', '새 할 일을 등록하고 기본 정보를 수정합니다.', 'CLUB', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TODO_CREATE');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'TODO_ASSIGN', 'TODO', '할 일 배정', '담당자를 지정하거나 지원 가능 업무로 전환합니다.', 'CLUB', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TODO_ASSIGN');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'TODO_MANAGE_STATUS', 'TODO', '할 일 상태 관리', '진행중, 완료, 취소 등 상태를 운영합니다.', 'CLUB', 1, 40, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TODO_MANAGE_STATUS');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'TODO_DELETE_ANY', 'TODO', '할 일 삭제', '등록된 할 일과 관련 신청 데이터를 삭제합니다.', 'CLUB', 1, 50, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'TODO_DELETE_ANY');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'ROLE_MANAGEMENT_VIEW', 'ROLE_MANAGEMENT', '직책 조회', '직책 목록과 권한 구성을 조회합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'ROLE_MANAGEMENT_VIEW');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'ROLE_MANAGEMENT_CREATE', 'ROLE_MANAGEMENT', '직책 생성', '새 직책을 생성합니다.', 'CLUB', 1, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'ROLE_MANAGEMENT_CREATE');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'ROLE_MANAGEMENT_UPDATE', 'ROLE_MANAGEMENT', '직책 수정', '직책 정보와 권한 구성을 수정합니다.', 'CLUB', 1, 30, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'ROLE_MANAGEMENT_UPDATE');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'ROLE_MANAGEMENT_DELETE', 'ROLE_MANAGEMENT', '직책 삭제', '직책을 삭제합니다.', 'CLUB', 1, 40, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'ROLE_MANAGEMENT_DELETE');

INSERT INTO feature_permission_catalog (
    permission_key,
    feature_key,
    display_name,
    description,
    ownership_scope,
    active,
    sort_order,
    create_date,
    update_date
)
SELECT 'ROLE_MANAGEMENT_ASSIGN', 'ROLE_MANAGEMENT', '직책 할당', '멤버에게 직책을 할당하거나 해제합니다.', 'CLUB', 1, 50, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'ROLE_MANAGEMENT_ASSIGN');

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'BOARD_NOTICE', 'Board Notice', 'Latest announcements from your board.', 'forum', NULL, 'USER_HOME', 2, 1, 10, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'BOARD_NOTICE');

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'BOARD_STRIP', 'Board Strip', 'Compact strip of the latest board notices.', 'vertical_distribute', NULL, 'USER_HOME', 1, 2, 12, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'BOARD_STRIP');

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'SCHEDULE_OVERVIEW', 'Schedule Overview', 'Upcoming schedules and next events.', 'calendar_month', NULL, 'USER_HOME', 1, 1, 20, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'SCHEDULE_OVERVIEW');

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'SCHEDULE_INSIGHT', 'Schedule Insight', 'Upcoming and pending schedule metrics.', 'schedule', NULL, 'USER_HOME', 1, 1, 22, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'SCHEDULE_INSIGHT');

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'POLL_STATUS', 'Poll Status', 'Latest ongoing poll for your club.', 'poll', 'POLL', 'USER_HOME', 1, 1, 25, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'POLL_STATUS');

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'POLL_PULSE', 'Poll Pulse', 'Waiting, ongoing, and closed poll counts.', 'stacked_bar_chart', 'POLL', 'USER_HOME', 1, 1, 27, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'POLL_PULSE');

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'PROFILE_SUMMARY', 'My Profile', 'Quick access to your club profile.', 'person', NULL, 'USER_HOME', 1, 1, 30, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'PROFILE_SUMMARY');

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'TOURNAMENT_RECORD_LATEST', 'Tournament Center', 'Featured tournament and my closest tournament.', 'emoji_events', 'TOURNAMENT_RECORD', 'USER_HOME', 2, 1, 35, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'TOURNAMENT_RECORD_LATEST');

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'TOURNAMENT_RECORD_MINE', 'My Tournaments', 'My tournament pipeline and participation counts.', 'sports_score', 'TOURNAMENT_RECORD', 'USER_HOME', 1, 1, 36, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'TOURNAMENT_RECORD_MINE');

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'BRACKET_LATEST', 'Bracket Board', 'Approved brackets and my latest draft.', 'account_tree', 'BRACKET', 'USER_HOME', 1, 1, 37, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'BRACKET_LATEST');

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'ATTENDANCE_STATUS', 'Attendance Check', 'Check in and review attendance status.', 'fact_check', 'ATTENDANCE', 'USER_HOME', 1, 1, 40, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'ATTENDANCE_STATUS');

UPDATE dashboard_widget_catalog
SET active = 0, update_date = NOW()
WHERE widget_key = 'ATTENDANCE_STATUS';

UPDATE dashboard_widget_catalog
SET display_name = 'Bracket Draft', description = 'Approved bracket drafts and my latest pending draft.', update_date = NOW()
WHERE widget_key = 'BRACKET_LATEST';

UPDATE feature_permission_catalog
SET display_name = '할 일 보관', description = '업무와 신청 이력을 보존한 채 운영 목록에서 보관합니다.', update_date = NOW()
WHERE permission_key = 'TODO_DELETE_ANY';

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'ATTENDANCE_RECENT', 'Attendance Recent', 'Recent attendance logs and completion rate.', 'event_note', 'ATTENDANCE', 'USER_HOME', 1, 1, 41, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'ATTENDANCE_RECENT');

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'FINANCE_STATUS', 'Finance Status', 'My pending finance items and latest payment status.', 'payments', 'FINANCE', 'USER_HOME', 1, 1, 42, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'FINANCE_STATUS');

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'FINANCE_LEDGER', 'Finance Ledger', 'Pending and completed finance summary.', 'account_balance_wallet', 'FINANCE', 'USER_HOME', 1, 1, 44, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'FINANCE_LEDGER');

-- ------------------------------------------------------------
-- Optional example: enable attendance for specific clubs.
-- Uncomment and adjust club ids only when you intentionally want
-- to turn the feature on outside the admin UI.
-- ------------------------------------------------------------
-- INSERT INTO feature_activation (
--     club_id,
--     feature_key,
--     enabled,
--     enabled_by_club_profile_id,
--     enabled_at,
--     create_date,
--     update_date
-- )
-- SELECT
--     c.club_id,
--     'ATTENDANCE',
--     1,
--     cp.club_profile_id,
--     NOW(),
--     NOW(),
--     NOW()
-- FROM club c
-- JOIN club_member cm
--   ON cm.club_id = c.club_id
--  AND cm.role_code = 'OWNER'
--  AND cm.membership_status = 'ACTIVE'
-- JOIN club_profile cp
--   ON cp.club_member_id = cm.club_member_id
-- WHERE c.club_id IN (1)
--   AND NOT EXISTS (
--       SELECT 1
--       FROM feature_activation fa
--       WHERE fa.club_id = c.club_id
--         AND fa.feature_key = 'ATTENDANCE'
--   );
