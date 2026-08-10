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
- `local-direct`는 `local` DB 설정을 재사용하면서 Eureka 등록/탐색을 끄고 `SEMO_AUTH_BASE_URL`로 auth 서버를 직접 찾습니다.
- Cloud Gateway/Eureka 경유가 필요하면 기존 `local` profile을 사용합니다.
- 테스트 profile: H2 in-memory datasource 사용
- `muse-back-service`와 기본 포트가 같아서 로컬 동시 실행 시 포트 조정이 필요합니다.

## Related Docs

- `AGENTS.md`
- `../semo-front-service/AGENTS_SEMO_MORE_FEATURE_CHECKLIST.md`

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

## Semo Modular Pattern

`semo`는 기능의 데이터·권한 경계를 분리하되, 프론트 진입 화면까지 기능 key와 일대일로 만들지는 않습니다.

- 기능 카탈로그: `feature_catalog`
- 클럽별 활성화: `feature_activation`
- 기능별 권한 카탈로그: `feature_permission_catalog`
- 홈 위젯 카탈로그: `dashboard_widget_catalog`
- 기능 전용 테이블 예시
  - `club_schedule_event`, `club_event_participant`
  - `finance_obligation`, `finance_payment`
  - `tournament_record`, `tournament_application`
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

선호 설정은 `club_more_preference`의 `(club_id, club_profile_id, feature_key)` 유일 키로 관리합니다. 최초 생성 경쟁도 같은 클럽 프로필 행을 잠근 뒤 처리해 중복 삽입을 방지합니다. 운영 DB에는 `db/ops/semo_more_preference_apply.sql`을 별도로 적용해야 하며 전체 DDL을 운영 DB에 직접 실행하지 않습니다.

### Activity
- `GET /api/semo/v1/clubs/{clubId}/profile/activity`
  - 로그인한 활성 멤버가 자신이 수행한 활동만 커서 기반으로 조회
- `GET /api/semo/v1/clubs/{clubId}/admin/activity`
  - 클럽 관리자 감사 로그이며 기능 활성화 여부와 무관하게 항상 기록·조회

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

## Schema and Seed Files

- DDL source of truth
  - `src/main/resources/db/ddl/semo_ddl_all.sql`
- Seed source of truth
  - `src/main/resources/db/seed/semo_seed_all.sql`
- 운영 DB 단건 반영 SQL
  - `src/main/resources/db/ops/semo_finance_request_expense_apply.sql`
  - 승인된 정산 요청과 지출 원장을 연결하는 nullable FK/unique 컬럼을 추가하며, 배포 전 백업 후 1회 적용
  - `src/main/resources/db/ddl/semo_timeline_feature_remove.sql`
  - 폐기된 `TIMELINE` 카탈로그·활성화·권한 데이터만 외래 키 순서대로 제거
  - `src/main/resources/db/ddl/semo_schedule_attendance_integration.sql`
  - 일정 참가자 원장에 실제 출석 필드를 추가하고 event 연결이 있는 레거시 출석을 이관
  - 날짜만 가진 `attendance_session`/`attendance_checkin`은 임의로 일정에 연결하지 않고, 운영자가 임시 `semo_attendance_session_event_mapping`에 세션-일정 관계를 명시한 건만 이관
  - `invalid_session_event_mapping_count`, `unmapped_daily_checkin_count`, `unmapped_legacy_attendance_count`가 모두 0이고 백업·이관 행을 검증한 뒤에만 주석 처리된 레거시 테이블/컬럼 제거문을 별도로 실행

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
  - 공지, 투표, 일정 출석, 대회, 대진표, 재정, 직책관리 권한
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

2026-08-07 현재 루트 wrapper 기준으로 `compileJava`와 전체 테스트를 검증합니다.

## Test Coverage Snapshot

현재 `src/test/java` 기준으로 아래 테스트가 존재합니다.

- `profile`
- `club`
- `clubfeature`
- `contentread`
- `dashboard`
- `feedback`
- `finance`
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
