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
    '가입 승인제 클럽에서 신청 대기열을 검토하고 승인 또는 반려합니다.',
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
    '일정 기능과 함께 참가 응답과 실제 참석 현황을 관리합니다.',
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
    '대회 등록, 승인과 참가자를 독립적으로 운영하며 재정 기능을 켜면 참가비 납부를 연결합니다.',
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
    '참가자를 직접 입력해 대진표를 만들고, 대회 기능을 켜면 승인 참가자를 불러옵니다.',
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
    '직책·권한',
    'OWNER와 ADMIN이 업무 직책을 만들고 일반 회원에게 필요한 기능 권한만 위임합니다.',
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
SET display_name = '가입 신청', description = '가입 승인제 클럽에서 신청 대기열을 검토하고 승인 또는 반려합니다.', navigation_scope = 'ADMIN_ONLY', sort_order = 10, update_date = NOW()
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
SET display_name = '일정 참석', description = '일정 기능과 함께 참가 응답과 실제 참석 현황을 관리합니다.', sort_order = 45, update_date = NOW()
WHERE feature_key = 'ATTENDANCE';
UPDATE feature_catalog
SET display_name = '대회 운영', description = '대회 등록, 승인과 참가자를 독립적으로 운영하며 재정 기능을 켜면 참가비 납부를 연결합니다.', sort_order = 50, update_date = NOW()
WHERE feature_key = 'TOURNAMENT_RECORD';
UPDATE feature_catalog
SET display_name = '대진표 초안', description = '참가자를 직접 입력해 대진표를 만들고, 대회 기능을 켜면 승인 참가자를 불러옵니다.', sort_order = 51, update_date = NOW()
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
UPDATE feature_catalog
SET display_name = '직책·권한',
    description = 'OWNER와 ADMIN이 업무 직책을 만들고 일반 회원에게 필요한 기능 권한만 위임합니다.',
    navigation_scope = 'ADMIN_ONLY',
    sort_order = 100,
    update_date = NOW()
WHERE feature_key = 'ROLE_MANAGEMENT';
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
SELECT 'ATTENDANCE_MANAGE', 'ATTENDANCE', '일정 출석 관리', '일정별 실제 출석 상태와 확인 메모를 관리합니다.', 'CLUB', 1, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'ATTENDANCE_MANAGE');

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
SELECT 'FINANCE_ISSUE', 'FINANCE', '레거시 재정 발행', '세분화된 재정 권한으로 이관된 비활성 호환 권한입니다.', 'CLUB', 0, 20, NOW(), NOW()
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
SELECT 'FINANCE_MARK_PAID', 'FINANCE', '레거시 납부 처리', 'FINANCE_PAYMENT_UPDATE로 이관된 비활성 호환 권한입니다.', 'CLUB', 0, 30, NOW(), NOW()
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
SELECT 'FINANCE_MARK_WAIVED', 'FINANCE', '레거시 면제 처리', 'FINANCE_PAYMENT_UPDATE로 이관된 비활성 호환 권한입니다.', 'CLUB', 0, 40, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_MARK_WAIVED');

INSERT INTO feature_permission_catalog
    (permission_key, feature_key, display_name, description, ownership_scope, active, sort_order, create_date, update_date)
SELECT 'FINANCE_BILLING_ISSUE', 'FINANCE', '청구 발행', '회비와 분담금 청구를 생성하고 대상 멤버에게 발행합니다.', 'CLUB', 1, 21, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_BILLING_ISSUE');

INSERT INTO feature_permission_catalog
    (permission_key, feature_key, display_name, description, ownership_scope, active, sort_order, create_date, update_date)
SELECT 'FINANCE_REQUEST_REVIEW', 'FINANCE', '정산 요청 검토', '멤버가 제출한 선지출·환불·정산 요청을 승인하거나 반려합니다.', 'CLUB', 1, 22, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_REQUEST_REVIEW');

INSERT INTO feature_permission_catalog
    (permission_key, feature_key, display_name, description, ownership_scope, active, sort_order, create_date, update_date)
SELECT 'FINANCE_EXPENSE_CREATE', 'FINANCE', '지출 입력 및 정정', '지출을 입력하고 증빙과 정정·취소 이력을 관리합니다.', 'CLUB', 1, 23, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_EXPENSE_CREATE');

INSERT INTO feature_permission_catalog
    (permission_key, feature_key, display_name, description, ownership_scope, active, sort_order, create_date, update_date)
SELECT 'FINANCE_PAYMENT_UPDATE', 'FINANCE', '수납 상태 변경', '납부 상태와 결제 수단을 변경합니다.', 'CLUB', 1, 31, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_PAYMENT_UPDATE');

INSERT INTO feature_permission_catalog
    (permission_key, feature_key, display_name, description, ownership_scope, active, sort_order, create_date, update_date)
SELECT 'FINANCE_EXPORT', 'FINANCE', '재정 내보내기', '기간별 청구·수납·지출 내역을 CSV로 내보냅니다.', 'CLUB', 1, 50, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_EXPORT');

INSERT INTO feature_permission_catalog
    (permission_key, feature_key, display_name, description, ownership_scope, active, sort_order, create_date, update_date)
SELECT 'FINANCE_PERIOD_CLOSE', 'FINANCE', '예산 및 기간 마감', '예산을 관리하고 월·시즌 재정 기간을 마감합니다.', 'CLUB', 1, 60, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'FINANCE_PERIOD_CLOSE');

-- 기존 광범위 재정 권한을 세분 권한으로 보존 이관한 뒤 더 이상 신규 직책에 노출하지 않습니다.
INSERT INTO club_position_permission (club_position_id, permission_key, create_date, update_date)
SELECT legacy.club_position_id, replacement.permission_key, NOW(), NOW()
FROM club_position_permission legacy
CROSS JOIN (
    SELECT 'FINANCE_BILLING_ISSUE' AS permission_key
    UNION ALL SELECT 'FINANCE_REQUEST_REVIEW'
    UNION ALL SELECT 'FINANCE_EXPENSE_CREATE'
) replacement
WHERE legacy.permission_key = 'FINANCE_ISSUE'
  AND NOT EXISTS (
      SELECT 1
      FROM club_position_permission current_permission
      WHERE current_permission.club_position_id = legacy.club_position_id
        AND current_permission.permission_key = replacement.permission_key
  );

INSERT INTO club_position_permission (club_position_id, permission_key, create_date, update_date)
SELECT DISTINCT legacy.club_position_id, 'FINANCE_PAYMENT_UPDATE', NOW(), NOW()
FROM club_position_permission legacy
WHERE legacy.permission_key IN ('FINANCE_MARK_PAID', 'FINANCE_MARK_WAIVED')
  AND NOT EXISTS (
      SELECT 1
      FROM club_position_permission current_permission
      WHERE current_permission.club_position_id = legacy.club_position_id
        AND current_permission.permission_key = 'FINANCE_PAYMENT_UPDATE'
  );

UPDATE feature_permission_catalog
SET active = 0,
    description = CASE permission_key
        WHEN 'FINANCE_ISSUE' THEN '세분화된 재정 권한으로 이관된 비활성 호환 권한입니다.'
        ELSE 'FINANCE_PAYMENT_UPDATE로 이관된 비활성 호환 권한입니다.'
    END,
    update_date = NOW()
WHERE permission_key IN ('FINANCE_ISSUE', 'FINANCE_MARK_PAID', 'FINANCE_MARK_WAIVED');

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
SELECT 'ROLE_MANAGEMENT_VIEW', 'ROLE_MANAGEMENT', '직책 조회', '거버넌스 권한은 OWNER와 ADMIN에게만 부여됩니다.', 'CLUB', 0, 10, NOW(), NOW()
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
SELECT 'ROLE_MANAGEMENT_CREATE', 'ROLE_MANAGEMENT', '직책 생성', '거버넌스 권한은 OWNER와 ADMIN에게만 부여됩니다.', 'CLUB', 0, 20, NOW(), NOW()
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
SELECT 'ROLE_MANAGEMENT_UPDATE', 'ROLE_MANAGEMENT', '직책 수정', '거버넌스 권한은 OWNER와 ADMIN에게만 부여됩니다.', 'CLUB', 0, 30, NOW(), NOW()
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
SELECT 'ROLE_MANAGEMENT_DELETE', 'ROLE_MANAGEMENT', '직책 사용 종료', '거버넌스 권한은 OWNER와 ADMIN에게만 부여됩니다.', 'CLUB', 0, 40, NOW(), NOW()
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
SELECT 'ROLE_MANAGEMENT_ASSIGN', 'ROLE_MANAGEMENT', '직책 할당', '거버넌스 권한은 OWNER와 ADMIN에게만 부여됩니다.', 'CLUB', 0, 50, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'ROLE_MANAGEMENT_ASSIGN');

-- 직책 구성 자체를 다시 직책으로 위임하면 사용자가 자신의 권한을 증폭시킬 수 있습니다.
-- 거버넌스는 클럽 접근 등급(OWNER/ADMIN)으로만 관리하고 기존 위임 데이터는 정리합니다.
DELETE FROM club_position_permission
WHERE permission_key IN (
    'ROLE_MANAGEMENT_VIEW',
    'ROLE_MANAGEMENT_CREATE',
    'ROLE_MANAGEMENT_UPDATE',
    'ROLE_MANAGEMENT_DELETE',
    'ROLE_MANAGEMENT_ASSIGN'
);

UPDATE feature_permission_catalog
SET active = 0,
    description = '거버넌스 권한은 OWNER와 ADMIN에게만 부여됩니다.',
    update_date = NOW()
WHERE permission_key IN (
    'ROLE_MANAGEMENT_VIEW',
    'ROLE_MANAGEMENT_CREATE',
    'ROLE_MANAGEMENT_UPDATE',
    'ROLE_MANAGEMENT_DELETE',
    'ROLE_MANAGEMENT_ASSIGN'
);

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
SELECT 'ATTENDANCE_STATUS', '다음 일정 출석', '다가오는 일정의 참석 응답과 실제 출석 상태를 확인합니다.', 'fact_check', 'ATTENDANCE', 'USER_HOME', 1, 1, 40, 1, NOW(), NOW()
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
SELECT 'ATTENDANCE_RECENT', '최근 일정 출석', '최근 일정별 참석 예정 인원과 실제 출석률을 확인합니다.', 'event_note', 'ATTENDANCE', 'USER_HOME', 1, 1, 41, 1, NOW(), NOW()
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

-- ------------------------------------------------------------
-- Operating term and handover center
-- ------------------------------------------------------------
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
    'HANDOVER',
    '인수인계 센터',
    '직책·권한과 함께 운영 임기, 집행부 구성과 다음 담당자 메모를 관리합니다.',
    'move_up',
    'ADMIN_ONLY',
    1,
    110,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_catalog WHERE feature_key = 'HANDOVER');

UPDATE feature_catalog
SET display_name = '인수인계 센터',
    description = '직책·권한과 함께 운영 임기, 집행부 구성과 다음 담당자 메모를 관리합니다.',
    icon_name = 'move_up',
    navigation_scope = 'ADMIN_ONLY',
    active = 1,
    sort_order = 110,
    update_date = NOW()
WHERE feature_key = 'HANDOVER';

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
SELECT
    'HANDOVER_VIEW',
    'HANDOVER',
    '인수인계 조회',
    '운영 임기, 집행부, 업무 큐와 인수인계 메모를 조회합니다.',
    'CLUB',
    1,
    10,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'HANDOVER_VIEW');

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
SELECT
    'HANDOVER_MANAGE',
    'HANDOVER',
    '인수인계 관리',
    '운영 임기와 집행부 구성을 관리하고 인수인계 메모를 작성합니다.',
    'CLUB',
    1,
    20,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'HANDOVER_MANAGE');

-- ------------------------------------------------------------
-- Meeting minutes and decision log
-- ------------------------------------------------------------
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
    'DECISION_LOG',
    '회의록·결정',
    '회의 배경과 결정 이유, 참여자, 관련 운영 항목과 후속 업무를 기록합니다.',
    'gavel',
    'USER_AND_ADMIN',
    1,
    90,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_catalog WHERE feature_key = 'DECISION_LOG');

UPDATE feature_catalog
SET display_name = '회의록·결정',
    description = '회의 배경과 결정 이유, 참여자, 관련 운영 항목과 후속 업무를 기록합니다.',
    icon_name = 'gavel',
    navigation_scope = 'USER_AND_ADMIN',
    active = 1,
    sort_order = 90,
    update_date = NOW()
WHERE feature_key = 'DECISION_LOG';

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
SELECT
    'DECISION_VIEW',
    'DECISION_LOG',
    '운영 결정 조회',
    '운영진 공개 회의록, 결정 초안과 검토 일정을 조회합니다.',
    'CLUB',
    1,
    10,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'DECISION_VIEW');

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
SELECT
    'DECISION_MANAGE',
    'DECISION_LOG',
    '운영 결정 관리',
    '회의록과 결정 초안을 작성하고 확정, 대체, 보관합니다.',
    'CLUB',
    1,
    20,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM feature_permission_catalog WHERE permission_key = 'DECISION_MANAGE');

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
SELECT
    'DECISION_LATEST',
    '최근 운영 결정',
    '최근 확정된 회의록과 운영 결정을 확인합니다.',
    'gavel',
    'DECISION_LOG',
    'USER_HOME',
    1,
    1,
    48,
    1,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'DECISION_LATEST');
