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
  - `timeline`
  - `attendance`
  - `dues`
  - `tournament`
  - `bracket`
  - `role management`
  - `admin activity log`
- 인증 방식
  - Gateway가 전달한 헤더를 `UserContextArgumentResolver`로 복원
  - 컨트롤러는 `UserContext` 기반으로 `USER` 권한을 검사
- 이미지 처리
  - 프론트/이미지 서버에서 임시 업로드
  - 백엔드는 `ImageFinalizeClient`로 `/files/finalize` 호출
  - DB에는 `fileName` 중심으로 저장하고 URL은 `ImageFileUrlResolver`가 조합

## Stack

- Java `21`
- Gradle Wrapper `9.3.0`
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
| `local` | `20280` |
| `dev` | `20280` |
| `prod` | `10280` |
| `test` | `30280` |

- 기본 profile: `local`
- 테스트 profile: H2 in-memory datasource 사용
- `muse-back-service`와 기본 포트가 같아서 로컬 동시 실행 시 포트 조정이 필요합니다.

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
- `src/main/java/semo/back/service/feature/timeline`
- `src/main/java/semo/back/service/feature/attendance`
- `src/main/java/semo/back/service/feature/dues`
- `src/main/java/semo/back/service/feature/tournament`
- `src/main/java/semo/back/service/feature/bracket`
- `src/main/java/semo/back/service/feature/position`
- `src/main/java/semo/back/service/feature/activity`

## Semo Modular Pattern

`semo`는 기능 단위를 클럽 공통 도메인에 섞지 않고 기능 경계로 나눕니다.

- 기능 카탈로그: `feature_catalog`
- 클럽별 활성화: `feature_activation`
- 기능별 권한 카탈로그: `feature_permission_catalog`
- 홈 위젯 카탈로그: `dashboard_widget_catalog`
- 기능 전용 테이블 예시
  - `attendance_session`, `attendance_checkin`
  - `dues_charge`, `dues_invoice`
  - `tournament_record`, `tournament_application`
  - `bracket_record`, `bracket_participant`
  - `club_schedule_event`, `club_schedule_vote`, `club_schedule_vote_option`

프론트 경로도 이 구조를 그대로 따릅니다.

- 유저: `/clubs/{clubId}/more/<feature>`
- 관리자: `/clubs/{clubId}/admin/more/<feature>`

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
- `GET /api/semo/v1/clubs/{clubId}/more/notices`
- `GET /api/semo/v1/clubs/{clubId}/more/notices/{noticeId}`
- `POST /api/semo/v1/clubs/{clubId}/more/notices`
- `PUT /api/semo/v1/clubs/{clubId}/more/notices/{noticeId}`
- `DELETE /api/semo/v1/clubs/{clubId}/more/notices/{noticeId}`
- `POST /api/semo/v1/clubs/{clubId}/board/items/{boardItemId}/read`
- `GET /api/semo/v1/clubs/{clubId}/board/items/{boardItemId}/read-status`

### Schedule / poll / timeline / attendance
- `GET /api/semo/v1/clubs/{clubId}/schedule`
- `GET /api/semo/v1/clubs/{clubId}/schedule/events/{eventId}`
- `POST /api/semo/v1/clubs/{clubId}/schedule/events`
- `PUT /api/semo/v1/clubs/{clubId}/schedule/events/{eventId}`
- `DELETE /api/semo/v1/clubs/{clubId}/schedule/events/{eventId}`
- `PUT /api/semo/v1/clubs/{clubId}/schedule/events/{eventId}/participation`
- `GET /api/semo/v1/clubs/{clubId}/schedule/votes/{voteId}`
- `POST /api/semo/v1/clubs/{clubId}/schedule/votes`
- `PUT /api/semo/v1/clubs/{clubId}/schedule/votes/{voteId}`
- `DELETE /api/semo/v1/clubs/{clubId}/schedule/votes/{voteId}`
- `PUT /api/semo/v1/clubs/{clubId}/schedule/votes/{voteId}/selection`
- `PUT /api/semo/v1/clubs/{clubId}/schedule/votes/{voteId}/close`
- `GET /api/semo/v1/clubs/{clubId}/more/schedules`
- `GET /api/semo/v1/clubs/{clubId}/more/polls`
- `GET /api/semo/v1/clubs/{clubId}/more/polls/{voteId}`
- `POST /api/semo/v1/clubs/{clubId}/more/polls`
- `PUT /api/semo/v1/clubs/{clubId}/more/polls/{voteId}`
- `DELETE /api/semo/v1/clubs/{clubId}/more/polls/{voteId}`
- `PUT /api/semo/v1/clubs/{clubId}/more/polls/{voteId}/selection`
- `PUT /api/semo/v1/clubs/{clubId}/more/polls/{voteId}/close`
- `GET /api/semo/v1/clubs/{clubId}/more/timeline`
- `GET /api/semo/v1/clubs/{clubId}/admin/more/timeline`
- `PUT /api/semo/v1/clubs/{clubId}/admin/more/timeline`
- `GET /api/semo/v1/clubs/{clubId}/more/attendance`
- `POST /api/semo/v1/clubs/{clubId}/more/attendance/check-in`
- `GET /api/semo/v1/clubs/{clubId}/admin/more/attendance`
- `GET /api/semo/v1/clubs/{clubId}/more/todos`
- `POST /api/semo/v1/clubs/{clubId}/more/todos/{todoItemId}/apply`
- `POST /api/semo/v1/clubs/{clubId}/more/todos/{todoItemId}/claim`
- `DELETE /api/semo/v1/clubs/{clubId}/more/todos/{todoItemId}/applications/me`
- `POST /api/semo/v1/clubs/{clubId}/more/todos/{todoItemId}/complete`
- `GET /api/semo/v1/clubs/{clubId}/admin/more/todos`
- `POST /api/semo/v1/clubs/{clubId}/admin/more/todos`
- `PUT /api/semo/v1/clubs/{clubId}/admin/more/todos/{todoItemId}`
- `PUT /api/semo/v1/clubs/{clubId}/admin/more/todos/{todoItemId}/status`
- `DELETE /api/semo/v1/clubs/{clubId}/admin/more/todos/{todoItemId}`

### Dues / tournament / bracket / role management / activity
- `GET /api/semo/v1/clubs/{clubId}/more/dues`
- `GET /api/semo/v1/clubs/{clubId}/admin/more/dues`
- `POST /api/semo/v1/clubs/{clubId}/admin/more/dues/charges`
- `PUT /api/semo/v1/clubs/{clubId}/admin/more/dues/invoices/{invoiceId}/payment-status`
- `DELETE /api/semo/v1/clubs/{clubId}/admin/more/dues/charges/{chargeId}`
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
- `GET /api/semo/v1/clubs/{clubId}/admin/activity`

## Schema and Seed Files

- DDL source of truth
  - `src/main/resources/db/ddl/semo_ddl_all.sql`
- Seed source of truth
  - `src/main/resources/db/seed/semo_seed_all.sql`

현재 seed에는 아래 카탈로그 성격의 데이터가 포함됩니다.

- `feature_catalog`
  - `NOTICE`
  - `ATTENDANCE`
  - `TIMELINE`
  - `POLL`
  - `SCHEDULE_MANAGE`
  - `TOURNAMENT_RECORD`
  - `BRACKET`
  - `DUES`
  - `ROLE_MANAGEMENT`
- `feature_permission_catalog`
  - 공지, 투표, 대회, 대진표, 회비, 직책관리 권한
- `dashboard_widget_catalog`
  - 공지, 일정, 투표, 프로필, 출석, 회비, 대회, 대진표 위젯

## Run

```bash
./gradlew :semo-back-service:bootRun
./gradlew :semo-back-service:bootRun --args='--spring.profiles.active=local'
./gradlew :semo-back-service:bootRun --args='--spring.profiles.active=dev'
./gradlew :semo-back-service:bootRun --args='--spring.profiles.active=prod'
```

## Verify

```bash
./gradlew :semo-back-service:compileJava
./gradlew :semo-back-service:test
```

## Test Coverage Snapshot

현재 `src/test/java` 기준으로 아래 테스트가 존재합니다.

- `profile`
- `club`
- `clubfeature`
- `attendance`
- `contentread`
- `dashboard`
- `dues`
- `notice` 일부(permission/feed)
- `poll` 일부(permission)
- `position`
- `schedule`
- `timeline`

상대적으로 공백이 큰 영역도 있습니다.

- `tournament`
- `bracket`
- `activity` 서비스
- 여러 컨트롤러의 상세 API 계약 테스트

## Coding Notes For This Codebase

- 신규 외부 HTTP 호출은 현재 코드처럼 `RestClient` 패턴을 우선합니다.
- DTO는 Java `record` 친화적으로 설계돼 있습니다.
- 리스트 변환은 `stream().toList()` 스타일이 이미 많이 쓰이고 있습니다.
- 기능 추가 시 `feature_catalog -> feature_activation -> feature_permission_catalog -> feature 전용 API/테이블/화면` 흐름을 같이 맞추는 편이 현재 구조와 가장 잘 맞습니다.
