# semo-back-service

Semo 백엔드 서비스 스캐폴드입니다. 공통 설정, 데이터소스, JPA 엔티티/리포지토리 구조는 준비돼 있지만 현재 공개 컨트롤러나 도메인 기능은 아직 거의 없는 상태입니다.

## 현재 상태

- Spring Boot 서비스 기본 골격 존재
- `common` 계층과 `database/pub` 계층 존재
- MySQL + JPA + OpenFeign + Eureka 연동 구성
- 공개 API 컨트롤러는 현재 확인되지 않음
- 테스트는 `contextLoads` 수준

## 주요 패키지

- `common/config`
- `common/datasource`
- `common/exception`
- `common/jpa`
- `common/logback`
- `database/pub/dto`
- `database/pub/entity`
- `database/pub/repository`

## 포트

| Profile | Port |
|---|---:|
| `local` | `20280` |
| `dev` | `20280` |
| `prod` | `10280` |
| `test` | `30280` |

## 실행

```bash
./gradlew :semo-back-service:bootRun
./gradlew :semo-back-service:bootRun --args='--spring.profiles.active=local'
./gradlew :semo-back-service:bootRun --args='--spring.profiles.active=dev'
./gradlew :semo-back-service:bootRun --args='--spring.profiles.active=prod'
```

## 빌드 / 테스트

```bash
./gradlew :semo-back-service:compileJava
./gradlew :semo-back-service:test
```

## 설정 포인트

- Java 21
- Spring Boot 4.0.2
- MySQL + JPA
- OpenFeign + Eureka client
- Caffeine cache
- Actuator 활성화
- 기본 프로필은 `local`

## 주의

- 현재 `application-*.yml`에 로컬/개발용 DB 정보가 직접 들어 있으므로 새 환경에 그대로 복사하지 않는 편이 좋습니다.
- 기능 확장 시 `muse-back-service`나 `zeroq-back-service`처럼 feature 단위 패키지 구조를 먼저 설계하는 편이 안전합니다.
