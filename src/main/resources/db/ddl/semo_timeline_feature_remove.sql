-- TIMELINE은 기능 토글이 아닌 항상 사용 가능한 개인 활동 및 관리자 감사 로그로 대체되었습니다.
-- 외래 키 의존 순서에 맞춰 사용되지 않는 권한과 활성화 카탈로그만 제거합니다.

DELETE FROM club_position_permission
WHERE permission_key = 'TIMELINE_VIEW';

DELETE FROM feature_permission_catalog
WHERE permission_key = 'TIMELINE_VIEW'
   OR feature_key = 'TIMELINE';

DELETE FROM feature_activation
WHERE feature_key = 'TIMELINE';

DELETE FROM feature_catalog
WHERE feature_key = 'TIMELINE';
