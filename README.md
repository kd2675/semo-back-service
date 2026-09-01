# semo-back-service

`semo-back-service`는 Semo의 실제 백엔드 서비스입니다. 현재 코드는 인증 셸이나 스캐폴드 수준이 아니라, 클럽 운영과 `/more` 기능 모듈이 DB와 연결된 상태를 전제로 움직입니다.

## Code Truth Summary

- 핵심 도메인
  - `profile`
  - `club`
  - `join request`
  - `club feature activation`
  - `dashboard widgets`
  - `notice`
  - `board read status`
  - `schedule`
  - `poll`
  - member activity
  - schedule attendance
  - `finance`
  - `tournament`
  - `bracket`
  - `role management`
  - `admin activity log`
  - operating term / handover
  - decision log
  - persistent notification
  - common resource attachment
- 인증 방식
  - `local-direct`: Semo 프론트가 access token의 사용자 이름/식별자/역할 헤더를 전달
  - `local`: Cloud Gateway가 JWT를 검증하고 사용자 식별 헤더를 재주입
  - 두 모드 모두 `UserContextArgumentResolver`에서 동일한 `UserContext`로 복원
  - 컨트롤러는 `UserContext` 기반으로 `USER` 권한을 검사하며 공통 권한 계층상 `ADMIN`도 허용
  - 전역 `ADMIN`의 로그인 허용과 클럽 `OWNER`/`ADMIN` 권한은 분리하며, 클럽 운영 권한은 멤버십으로 판정
- 이미지 처리
  - 프론트/이미지 서버에서 임시 업로드
  - 백엔드는 `ImageFinalizeClient`로 `/files/finalize` 호출
  - DB에는 `fileName` 중심으로 저장하고 URL은 `ImageFileUrlResolver`가 조합
- 첨부파일 처리
  - 프론트는 이미지 서버의 `/upload/temp-file`에 임시 업로드
  - 백엔드는 동일한 `ATTACHMENT_INTERNAL_TOKEN`으로 이미지 서버의 내부 확정 API를 호출
  - `resource_attachment` 저장 커밋 후 확정 표식을 제거하고, 롤백 시 파일을 보상 삭제
  - 프로세스 중단으로 표식이 남으면 조정 작업이 DB 행 존재 여부에 따라 확정 또는 고아 삭제
  - 다운로드는 공개 이미지 URL이 아니라 SEMO의 권한 검사 API를 거쳐 프록시
  - 소프트 삭제 파일은 감사·보존 정책에 따라 물리 보존
- 응답 계약
  - 성공 응답은 `web-common-core`의 `ResponseDataDTO`
  - 에러 응답은 `web-common-core`의 `ResponseErrorDTO`
  - `semo` 커스텀 에러 바디가 아니라 공통 `{ success, code, message }` 래퍼를 기준으로 맞춥니다.

## Stack

- Java `21`
- 루트 Gradle Wrapper `9.2.1` 기준으로 워크스페이스 모듈 명령 실행
- 서비스 단독 Gradle Wrapper `9.3.0`
- Spring Boot `4.0.2`
- Spring Cloud BOM `2025.1.0`
- Spring Web, Validation, Data JPA, Actuator, Cache
- OpenFeign + Eureka Client
- MySQL Connector/J `8.3.0`
- H2 test runtime
- Caffeine Cache
- Lombok `1.18.30`

## Internal / External Dependencies

### Internal modules
- `:web-common-core`
- `:auth-common-core`

### Runtime integrations
- `cloud-back-server`
  - 인증 헤더 전달 주체
- `eureka-back-server`
  - 서비스 등록/발견
- `image-back-server`
  - 이미지 확정 API `/files/finalize`
  - 첨부 임시 업로드와 내부 확정·확인·조회·고아 정리 API
- MySQL
  - 운영 데이터 저장소

## Profiles and Ports

| Profile | Port |
|---|---:|
| `local-direct` | `20280` |
| `local` | `20280` |
| `dev` | `20280` |
| `prod` | `10280` |
| `test` | `30280` |

- 기본 profile: `local-direct`
- `local-direct`는 `local` DB 설정을 재사용하면서 Eureka 등록/탐색을 끄고 `127.0.0.1`에만 바인딩한 뒤 `SEMO_AUTH_BASE_URL`로 auth 서버를 직접 찾습니다.
- Cloud Gateway/Eureka 경유가 필요하면 기존 `local` profile을 사용합니다.
- 테스트 profile: H2 in-memory datasource 사용
- `muse-back-service`와 기본 포트가 같아서 로컬 동시 실행 시 포트 조정이 필요합니다.

## Related Docs

- `AGENTS.md`
- `../semo-front-service/SEMO_MORE_FEATURE_GUIDE.md`

## Package Map

- `src/main/java/semo/back/service/common`
  - config, datasource, exception, logback, util
- `src/main/java/semo/back/service/database/pub`
  - JPA entity, repository, datasource config
- `src/main/java/semo/back/service/feature/profile`
- `src/main/java/semo/back/service/feature/club`
- `src/main/java/semo/back/service/feature/clubfeature`
- `src/main/java/semo/back/service/feature/dashboard`
- `src/main/java/semo/back/service/feature/notice`
- `src/main/java/semo/back/service/feature/contentread`
- `src/main/java/semo/back/service/feature/schedule`
- `src/main/java/semo/back/service/feature/poll`
- `src/main/java/semo/back/service/feature/finance`
- `src/main/java/semo/back/service/feature/tournament`
- `src/main/java/semo/back/service/feature/bracket`
- `src/main/java/semo/back/service/feature/position`
- `src/main/java/semo/back/service/feature/activity`
- `src/main/java/semo/back/service/feature/attachment`
- `src/main/java/semo/back/service/feature/notification`
- `src/main/java/semo/back/service/feature/handover`
- `src/main/java/semo/back/service/feature/decision`

## Semo Modular Pattern

`semo`는 기능의 데이터·권한 경계를 분리하되, 프론트 진입 화면까지 기능 key와 일대일로 만들지는 않습니다.

- 기능 카탈로그: `feature_catalog`
- 클럽별 활성화: `feature_activation`
- 기능별 권한 카탈로그: `feature_permission_catalog`
- 홈 위젯 카탈로그: `dashboard_widget_catalog`
- 기능 전용 테이블 예시
  - `club_schedule_event`, `club_event_participant`
  - `finance_obligation`, `finance_payment`
  - `tournament_record`, `tournament_application`, `tournament_roster_member`, `tournament_schedule_slot`
  - `bracket_record`, `bracket_participant`
  - `club_schedule_event`, `club_schedule_vote`, `club_schedule_vote_option`

독립적인 운영 흐름이 있는 기능은 아래 경로를 따릅니다.

- 유저: `/clubs/{clubId}/more/<feature>`
- 관리자: `/clubs/{clubId}/admin/more/<feature>`

공지 API는 게시판, 일정·투표·참석 API는 캘린더 대표 화면에서 함께 사용합니다. 기능 활성화와 권한 판정은 각각의 feature key를 유지하며, 기존 `/more` 프론트 URL은 호환용 redirect로 남깁니다.

## Main API Map

### Profile
- `GET /api/semo/v1/profile/summary`
- `POST /api/semo/v1/profile/initialize`

### Club / membership
- `POST /api/semo/v1/clubs`
- `GET /api/semo/v1/clubs/my`
- `GET /api/semo/v1/clubs/discover`
- `GET /api/semo/v1/clubs/{clubId}`
- `GET /api/semo/v1/clubs/{clubId}/board`
- `GET /api/semo/v1/clubs/{clubId}/profile`
- `PUT /api/semo/v1/clubs/{clubId}/profile`
- `POST /api/semo/v1/clubs/{clubId}/join-requests`
- `DELETE /api/semo/v1/clubs/{clubId}/join-requests/me`
- `GET /api/semo/v1/clubs/{clubId}/admin/join-requests`
- `PUT /api/semo/v1/clubs/{clubId}/admin/join-requests/{clubJoinRequestId}/review`
- `GET /api/semo/v1/clubs/{clubId}/admin/members`
- `PUT /api/semo/v1/clubs/{clubId}/admin/members/{clubMemberId}/role`
- `PUT /api/semo/v1/clubs/{clubId}/admin/members/{clubMemberId}/status`
- `POST /api/semo/v1/clubs/{clubId}/admin/members/{clubMemberId}/approve`
- `PUT /api/semo/v1/clubs/{clubId}/admin/members/{clubMemberId}/positions`

회원·직책·임기 집행부 경계:

- `club_member.role_code`는 `OWNER`/`ADMIN`/`MEMBER` 클럽 접근 등급입니다. 일반 변경 API는 `ADMIN`과 `MEMBER`만 전환하며 OWNER 이전은 지원하지 않습니다.
- `club_position`과 `club_member_position`은 현재 업무 직책과 배정입니다. 직책 구성과 배정 자체는 OWNER/ADMIN만 관리하며 `ROLE_MANAGEMENT`는 꺼서 인가가 사라질 수 없는 핵심 운영 기능입니다.
- 직책 설정 API의 쓰기 계약은 원자 권한 배열이 아니라 기능별 운영 수준과 추가 승인 권한입니다. 이 사용자 의도는 `club_position_feature_grant`와 `club_position_sensitive_grant`에 저장하고, 서버가 실제 액션 권한을 `club_position_permission`으로 투영합니다.
- 인가 코드는 타입이 있는 `ClubCapability`와 한 번의 배정-직책-권한 조인 조회를 사용합니다. 기존 `club_position_permission`은 런타임 인가와 감사 호환을 위한 투영 테이블이며 프론트 쓰기 모델이 아닙니다.
- `ClubPositionAccessPolicy.policyVersion`이 바뀌어도 기존 투영 권한은 자동 확대하지 않습니다. 화면에 정책 업데이트를 알리고 관리자가 명시적으로 적용할 때만 새 조합으로 다시 투영합니다.
- 기존 비표준 원자 권한 조합은 `LEGACY_CUSTOM`으로 읽고 관리자가 표준 운영 수준을 선택하기 전까지 그대로 보존합니다. 정책에 등록되지 않은 신규 액션 권한도 자동 위임하지 않습니다.
- 직책 수정은 `club_position.version`의 낙관적 잠금 값을 요구하여 두 관리자의 덮어쓰기를 막습니다.
- 직책 사용 종료는 물리 삭제가 아니라 비활성화이며 현재 배정과 열린 이력을 종료합니다. 과거 이력, 권한 구성, 인수인계 FK는 보존합니다.
- `club_term_executive_assignment`는 임기별 집행부 스냅샷이며 현재 권한을 부여하지 않습니다.

### Feature activation / dashboard
- `GET /api/semo/v1/clubs/{clubId}/features`
- `PUT /api/semo/v1/clubs/{clubId}/features`
- `GET /api/semo/v1/clubs/{clubId}/dashboard/widgets`
- `GET /api/semo/v1/clubs/{clubId}/admin/dashboard/widgets/editor`
- `PUT /api/semo/v1/clubs/{clubId}/admin/dashboard/widgets/layout`

### Notice / board read
- `GET /api/semo/v1/clubs/{clubId}/board/notices`
- `GET /api/semo/v1/clubs/{clubId}/board/notices/{noticeId}`
- `POST /api/semo/v1/clubs/{clubId}/board/notices`
- `PUT /api/semo/v1/clubs/{clubId}/board/notices/{noticeId}`
- `DELETE /api/semo/v1/clubs/{clubId}/board/notices/{noticeId}`
- `POST /api/semo/v1/clubs/{clubId}/board/items/{boardItemId}/read`
- `GET /api/semo/v1/clubs/{clubId}/board/items/{boardItemId}/read-status`

### Schedule / poll / attendance
- `GET /api/semo/v1/clubs/{clubId}/schedule`
- `GET /api/semo/v1/clubs/{clubId}/schedule/events/{eventId}`
- `POST /api/semo/v1/clubs/{clubId}/schedule/events`
- `PUT /api/semo/v1/clubs/{clubId}/schedule/events/{eventId}`
- `DELETE /api/semo/v1/clubs/{clubId}/schedule/events/{eventId}`
- `PUT /api/semo/v1/clubs/{clubId}/schedule/events/{eventId}/participation`
- `GET /api/semo/v1/clubs/{clubId}/schedule/attendance/summary`
- `GET /api/semo/v1/clubs/{clubId}/schedule/events/{eventId}/attendance`
- `PUT /api/semo/v1/clubs/{clubId}/schedule/events/{eventId}/attendance/{clubProfileId}`
- `GET /api/semo/v1/clubs/{clubId}/schedule/votes/{voteId}`
- `POST /api/semo/v1/clubs/{clubId}/schedule/votes`
- `PUT /api/semo/v1/clubs/{clubId}/schedule/votes/{voteId}`
- `DELETE /api/semo/v1/clubs/{clubId}/schedule/votes/{voteId}`
- `PUT /api/semo/v1/clubs/{clubId}/schedule/votes/{voteId}/selection`
- `PUT /api/semo/v1/clubs/{clubId}/schedule/votes/{voteId}/close`
- `GET /api/semo/v1/clubs/{clubId}/schedule/votes/summary`

일정 참석 응답과 실제 출석은 `club_event_participant`에서 함께 관리합니다. `participation_status`는 `GOING`, `NOT_GOING`, `CANCELED`, 실제 `attendance_status`는 `PRESENT`, `LATE`, `ABSENT`, `EXCUSED`를 사용합니다. 실제 출석에는 확인자, 확인 시각, 운영 메모가 함께 저장되며 `ATTENDANCE_MANAGE` 권한을 직책에 위임할 수 있습니다.

### More summary / preference
- `GET /api/semo/v1/clubs/{clubId}/more/summary`
  - 활성 기능의 실제 정렬 순서, 사용자/운영 capability, 기능별 미처리·지연 건수, 즐겨찾기와 최근 사용 시각을 반환
- `PUT /api/semo/v1/clubs/{clubId}/more/preferences/{featureKey}`
  - 활성 기능의 개인 즐겨찾기 저장
- `POST /api/semo/v1/clubs/{clubId}/more/preferences/{featureKey}/usage`
  - 기능 진입 시 개인 최근 사용 시각 저장

선호 설정은 `club_more_preference`의 `(club_id, club_profile_id, feature_key)` 유일 키로 관리합니다. 최초 생성 경쟁도 같은 클럽 프로필 행을 잠근 뒤 처리해 중복 삽입을 방지합니다. 테이블 정의는 기준 DDL에 포함되어 있지만 현재 저장소에는 별도 `db/ops` 운영 반영 파일이 없습니다. 운영 DB 반영 시에는 기준 DDL 전체를 실행하지 말고 배포 대상 버전 간 스키마 차이로 검토된 마이그레이션을 별도로 준비합니다.

### Activity
- `GET /api/semo/v1/clubs/{clubId}/profile/activity`
  - 로그인한 활성 멤버가 자신이 수행한 활동만 커서 기반으로 조회
- `GET /api/semo/v1/clubs/{clubId}/admin/activity`
  - 클럽 관리자 감사 로그이며 기능 활성화 여부와 무관하게 항상 기록·조회

### Notification / attachment
- `GET /api/semo/v1/notifications`
- `GET /api/semo/v1/notifications/summary`
- `PUT /api/semo/v1/notifications/{notificationId}/read`
- `PUT /api/semo/v1/notifications/read-all`
- `GET /api/semo/v1/clubs/{clubId}/attachments`
- `POST /api/semo/v1/clubs/{clubId}/attachments`
- `GET /api/semo/v1/clubs/{clubId}/attachments/{attachmentId}/download`
- `DELETE /api/semo/v1/clubs/{clubId}/attachments/{attachmentId}`

첨부 목록과 다운로드는 모두 대상 업무·재정·피드백·인수인계·결정 기록의 실제 권한을 다시 판정합니다. 이미지 서버의 `semo/attachments/**` 최종 파일은 공개 `/files`로 조회할 수 없으며 내부 토큰 요청만 허용합니다. `dev`와 `prod`에서는 SEMO와 이미지 서버에 같은 `ATTACHMENT_INTERNAL_TOKEN`을 설정해야 합니다.

### Todo
- `GET /api/semo/v1/clubs/{clubId}/more/todos`
- `POST /api/semo/v1/clubs/{clubId}/more/todos/{todoItemId}/apply`
- `DELETE /api/semo/v1/clubs/{clubId}/more/todos/{todoItemId}/applications/me`
- `POST /api/semo/v1/clubs/{clubId}/more/todos/{todoItemId}/complete`
- `GET /api/semo/v1/clubs/{clubId}/admin/more/todos`
- `POST /api/semo/v1/clubs/{clubId}/admin/more/todos`
- `PUT /api/semo/v1/clubs/{clubId}/admin/more/todos/{todoItemId}`
- `PUT /api/semo/v1/clubs/{clubId}/admin/more/todos/{todoItemId}/status`
- `DELETE /api/semo/v1/clubs/{clubId}/admin/more/todos/{todoItemId}`
  - 실제 삭제가 아니라 `CANCELED` 상태로 보관

### Finance / tournament / bracket / role management
- `GET /api/semo/v1/clubs/{clubId}/more/finance`
- `GET /api/semo/v1/clubs/{clubId}/admin/more/finance`
- `GET /api/semo/v1/clubs/{clubId}/admin/more/finance/obligations`
- `GET /api/semo/v1/clubs/{clubId}/admin/more/finance/obligations/{obligationId}/payments`
- `POST /api/semo/v1/clubs/{clubId}/admin/more/finance/obligations`
- `PATCH /api/semo/v1/clubs/{clubId}/admin/more/finance/payments/{paymentId}/status`
- `DELETE /api/semo/v1/clubs/{clubId}/admin/more/finance/obligations/{obligationId}`
- `GET /api/semo/v1/clubs/{clubId}/more/tournaments`
- `GET /api/semo/v1/clubs/{clubId}/more/tournaments/{tournamentRecordId}`
- `POST /api/semo/v1/clubs/{clubId}/more/tournaments`
- `PUT /api/semo/v1/clubs/{clubId}/more/tournaments/{tournamentRecordId}`
- `PUT /api/semo/v1/clubs/{clubId}/more/tournaments/{tournamentRecordId}/cancel`
- `POST /api/semo/v1/clubs/{clubId}/more/tournaments/{tournamentRecordId}/applications`
- `DELETE /api/semo/v1/clubs/{clubId}/more/tournaments/{tournamentRecordId}/applications/me`
- `PUT /api/semo/v1/clubs/{clubId}/more/tournaments/{tournamentRecordId}/applications/{tournamentApplicationId}/review`
- `PUT /api/semo/v1/clubs/{clubId}/more/tournaments/{tournamentRecordId}/applications/{tournamentApplicationId}/operations`
- `POST /api/semo/v1/clubs/{clubId}/more/tournaments/{tournamentRecordId}/schedule-slots`
- `PUT /api/semo/v1/clubs/{clubId}/more/tournaments/{tournamentRecordId}/schedule-slots/{scheduleSlotId}`
- `DELETE /api/semo/v1/clubs/{clubId}/more/tournaments/{tournamentRecordId}/schedule-slots/{scheduleSlotId}`
- `GET /api/semo/v1/clubs/{clubId}/admin/more/tournaments`
- `PUT /api/semo/v1/clubs/{clubId}/admin/more/tournaments/{tournamentRecordId}/review`
- `DELETE /api/semo/v1/clubs/{clubId}/admin/more/tournaments/{tournamentRecordId}`
- `GET /api/semo/v1/clubs/{clubId}/more/brackets`
- `GET /api/semo/v1/clubs/{clubId}/more/brackets/{bracketRecordId}`
- `POST /api/semo/v1/clubs/{clubId}/more/brackets`
- `PUT /api/semo/v1/clubs/{clubId}/more/brackets/{bracketRecordId}`
- `PUT /api/semo/v1/clubs/{clubId}/more/brackets/{bracketRecordId}/submit`
- `GET /api/semo/v1/clubs/{clubId}/admin/more/brackets`
- `PUT /api/semo/v1/clubs/{clubId}/admin/more/brackets/{bracketRecordId}/review`
- `DELETE /api/semo/v1/clubs/{clubId}/admin/more/brackets/{bracketRecordId}`
- `GET /api/semo/v1/clubs/{clubId}/admin/more/roles`
- `POST /api/semo/v1/clubs/{clubId}/admin/more/roles`
- `GET /api/semo/v1/clubs/{clubId}/admin/more/roles/{clubPositionId}`
- `PUT /api/semo/v1/clubs/{clubId}/admin/more/roles/{clubPositionId}`
- `DELETE /api/semo/v1/clubs/{clubId}/admin/more/roles/{clubPositionId}`
- `GET /api/semo/v1/clubs/{clubId}/admin/operations-catalog`
- `POST /api/semo/v1/clubs/{clubId}/admin/operations-catalog/presets/{presetKey}/apply`
- `POST /api/semo/v1/clubs/{clubId}/admin/operations-catalog/templates/{templateKey}/apply`

대회 신청은 개인전 또는 팀 로스터를 저장하며 정원 초과 시 대기 순번을 부여합니다. 취소·반려로 자리가 생기면 가장 앞선 대기 신청을 자동으로 검토 대기로 올립니다. 참가 승인 시 `FINANCE`가 활성화된 유료 대회는 참가비 청구와 결제 원장을 한 번만 생성하고, 운영자는 코트 시간표·체크인·순위·결과 메모를 기록합니다.

운영 카탈로그의 프리셋은 기존 활성 기능을 보존하는 `MERGE`를 기본으로 기능 순서, 사용자 홈 위젯, 미생성 위임 직책과 권한을 함께 구성합니다. 운영 템플릿은 `TODO` 업무와 체크리스트를 실제 생성하며 필요한 도메인 기능이 비활성화된 경우 적용을 거부합니다.

## Schema and Seed Files

- DDL source of truth
  - `src/main/resources/db/ddl/semo_ddl_all.sql`
  - 신규 환경과 전체 스키마 검증을 위한 기준 파일이며 운영 DB에 그대로 실행하는 배포 묶음이 아닙니다.
  - 활성·휴면 `club_member`에 `club_profile`이 반드시 존재하도록 레거시 누락 행을 보정하는 idempotent `INSERT ... LEFT JOIN` 구문을 포함합니다.
- Seed source of truth
  - `src/main/resources/db/seed/semo_seed_all.sql`
- 현재 유지되는 목적별 DDL
  - `src/main/resources/db/ddl/semo_growth_core_apply.sql`
  - 모임과 1:1인 상시 `club_growth_core` 테이블을 만들고 기존 모임을 원석 상태로 백필합니다. 누락·고아 건수가 0인지 확인한 뒤 애플리케이션의 bounded reconciler가 실제 원장 기록을 정책 V1 점수로 투영합니다.
  - `src/main/resources/db/ddl/semo_club_profile_backfill.sql`
  - 가입·가입 승인·멤버 상태 변경 쓰기를 일시 중지한 상태에서 실행하는 운영 데이터 보정입니다. 적용 전 누락 건수를 기록하고 적용 후 누락 건수가 0인지 확인한 뒤 애플리케이션을 배포하고 쓰기를 재개합니다.
  - `src/main/resources/db/ddl/semo_timeline_feature_remove.sql`
  - 폐기된 `TIMELINE` 카탈로그·활성화·권한 데이터만 외래 키 순서대로 제거
  - `src/main/resources/db/ddl/semo_schedule_attendance_integration.sql`
  - 일정 참가자 원장에 실제 출석 필드를 추가하고 event 연결이 있는 레거시 출석을 이관
  - 날짜만 가진 `attendance_session`/`attendance_checkin`은 임의로 일정에 연결하지 않고, 운영자가 임시 `semo_attendance_session_event_mapping`에 세션-일정 관계를 명시한 건만 이관
  - `invalid_session_event_mapping_count`, `unmapped_daily_checkin_count`, `unmapped_legacy_attendance_count`가 모두 0이고 백업·이관 행을 검증한 뒤에만 주석 처리된 레거시 테이블/컬럼 제거문을 별도로 실행

과거 문서에서 안내하던 `src/main/resources/db/ops/*.sql` 파일은 현재 저장소에 존재하지 않습니다. 재정·대회·할 일·결정 기능의 현재 스키마 계약은 기준 DDL에 통합되어 있으며, 운영 반영 파일명이나 적용 순서를 존재하지 않는 경로에서 추정하지 않습니다.

## Mandatory Growth Core

모임 생성은 `club`, OWNER `club_member`, `club_profile`과 함께 `club_growth_core` 원석 행을 같은 트랜잭션에서 저장합니다. 성장 코어는 feature activation이 아니며 별도 More API를 만들지 않습니다. 생성·내 모임·공개 탐색 응답의 `growthCore` 필드로 현재 소재, 다음 소재, `함께`·`운영`·`이어짐` 진행, 최근 활동 밝기를 제공합니다.

점수는 출석, 일정 투표, 게시판 읽음, 공지, 할 일, 피드백, 결정, 운영 임기와 인수인계 원장의 실제 상태를 단일 bounded 집계로 다시 계산합니다. 사람이 읽는 활동 로그 문구는 티어 근거로 사용하지 않습니다. 성공 활동 로그는 최근 14일 절대 활동 밝기에만 사용합니다. 정책 V1의 구체적인 증거와 가중치, 비경쟁 원칙은 `semo-front-service/SEMO_MORE_FEATURE_GUIDE.md`의 모임 성장 코어 절을 기준으로 합니다.

현재 seed에는 아래 카탈로그 성격의 데이터가 포함됩니다.

- `feature_catalog`
  - `JOIN_REQUEST`
  - `NOTICE`
  - `ATTENDANCE`
  - `POLL`
  - `SCHEDULE_MANAGE`
  - `TOURNAMENT_RECORD`
  - `BRACKET`
  - `FINANCE`
  - `TODO`
  - `MEMBER_DIRECTORY`
  - `FEEDBACK`
  - `ROLE_MANAGEMENT`
- `feature_permission_catalog`
  - 공지, 투표, 일정 출석, 대회, 대진표, 재정 권한
- `dashboard_widget_catalog`
  - 공지, 보드 스트립, 일정 개요, 일정 인사이트, 투표 상태, 투표 펄스, 프로필, 출석 상태, 최근 출석, 재정 상태, 재정 요약, 대회 센터, 내 대회, 대진표 보드, 대진표 워크벤치 위젯

## Run

```bash
./gradlew :semo-back-service:bootRun
./gradlew :semo-back-service:bootRun --args='--spring.profiles.active=local-direct'
./gradlew :semo-back-service:bootRun --args='--spring.profiles.active=local'
./gradlew :semo-back-service:bootRun --args='--spring.profiles.active=dev'
./gradlew :semo-back-service:bootRun --args='--spring.profiles.active=prod'
```

## Verify

```bash
./gradlew :semo-back-service:compileJava
./gradlew :semo-back-service:test
./gradlew :semo-back-service:test --rerun-tasks
```

2026-08-31 현재 루트 wrapper 기준으로 `compileJava`와 전체 230개 테스트를 검증했습니다.

## Test Coverage Snapshot

현재 `src/test/java` 기준으로 아래 테스트가 존재합니다.

- `profile`
- `club`
- `clubfeature`
- `attachment`
- `notification`
- `contentread`
- `dashboard`
- `feedback`
- `finance`
- `handover`
- `decision`
- `memberdirectory`
- `notice` 일부(permission/feed)
- `poll` 일부(permission)
- `position`
- `schedule`
- `activity`
- `todo`
- `tournament`
- `bracket`

상대적으로 공백이 큰 영역도 있습니다.

- 여러 컨트롤러의 상세 API 계약 테스트

## Coding Notes For This Codebase

- 신규 외부 HTTP 호출은 현재 코드처럼 `RestClient` 패턴을 우선합니다.
- DTO는 Java `record` 친화적으로 설계돼 있습니다.
- 리스트 변환은 `stream().toList()` 스타일이 이미 많이 쓰이고 있습니다.
- 기능 추가 시 `feature_catalog -> feature_activation -> feature_permission_catalog -> feature 전용 API/테이블/화면` 흐름을 같이 맞추는 편이 현재 구조와 가장 잘 맞습니다.
