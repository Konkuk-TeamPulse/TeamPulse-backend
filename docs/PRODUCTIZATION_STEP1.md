# TeamPulse 제품화 1차 정리

이 문서는 MVP에서 실제 서비스 방향으로 넘어가기 위해 이번 백엔드 변경에서 무엇이 바뀌었고, 팀 문서/Notion에 무엇을 반영해야 하는지 정리한 기록입니다.

## 변경 목적

- MySQL 운영 DB를 Hibernate 자동 수정(`ddl-auto=update`)에 맡기지 않고, Flyway migration SQL로 관리합니다.
- 애플리케이션 시작 시 JPA는 DB 스키마를 자동 변경하지 않고 `validate`만 수행합니다.
- API 명세를 코드 기준으로 확인할 수 있도록 OpenAPI JSON을 제공합니다.
- 기존 프론트 API path는 유지합니다. 이번 변경은 API 호출 구조 변경이 아니라 DB 운영 방식과 문서화 방식 변경입니다.

## 변경된 파일

| 파일 | 변경 내용 |
| --- | --- |
| `pom.xml` | Flyway, Flyway MySQL, springdoc-openapi 의존성 추가 |
| `src/main/resources/application.properties` | 기본 profile에서는 Flyway 비활성화, OpenAPI JSON path 설정 |
| `src/main/resources/application-mysql.properties` | MySQL profile만 Flyway 활성화, Hibernate `validate` 전환 |
| `src/main/resources/db/migration/mysql/V1__initial_mysql_schema.sql` | 신규 MySQL DB 생성용 초기 스키마 |
| `src/main/resources/db/migration/mysql/V2__backfill_workspace_owner_and_member_email.sql` | 기존 DB 보강용 컬럼 migration |
| `src/main/java/com/teampulse/backend/common/config/OpenApiConfig.java` | OpenAPI 기본 정보와 bearer token scheme 추가 |
| `src/main/java/com/teampulse/backend/common/config/SecurityConfig.java` | OpenAPI 문서 endpoint 공개 허용 |
| `docs/API_CHANGES.md` | 팀원이 확인할 API 문서/DB migration 반영 사항 추가 |

## 새로 확인 가능한 문서 endpoint

| 구분 | endpoint | 인증 |
| --- | --- | --- |
| OpenAPI JSON | `GET /v3/api-docs` | 불필요 |

프론트가 호출하는 업무 API path는 이번 작업에서 바뀌지 않았습니다.

Swagger UI는 Spring Boot 3.5와 springdoc UI starter 호환성을 별도 확인한 뒤 다음 단계에서 붙입니다. 이번 단계에서는 서버 부팅 안정성을 우선해 JSON 명세만 제공합니다.

## MySQL profile 동작 방식

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration/mysql
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
```

- 새 DB에서는 `V1`부터 migration이 실행되어 전체 테이블을 만듭니다.
- 이미 테이블이 있는 기존 DB에서는 Flyway가 현재 스키마를 baseline으로 잡고, `V2`부터 필요한 보강 migration을 실행합니다.
- 기존 DB에 `assignment2_workspaces.owner_email` 또는 `assignment2_members.email`이 없으면 `V2`가 추가합니다.
- migration 이후 Hibernate가 entity와 DB 스키마를 비교해 맞지 않으면 서버 시작 단계에서 실패합니다.

## 검증 결과

- `mvn test -Dspring.profiles.active=demo`: 24개 테스트 통과
- `OpenApiDocumentationTest`: `/v3/api-docs` 공개 접근과 OpenAPI title 확인
- 임시 MySQL 검증:
  - `flyway_schema_history`에 `V1`, `V2` 성공 기록 확인
  - `assignment2_workspaces.owner_email` 컬럼 확인
  - `assignment2_members.email` 컬럼 확인
  - `users`, `auth_sessions`, `assignment2_*` 테이블 생성 확인

## Notion/API 문서 반영 위치

1. API 문서 페이지
   - `GET /v3/api-docs`
   - 이 endpoint는 코드 기준 API 명세 확인용이라고 적습니다.

2. DB 설계 페이지
   - MySQL 기준 migration 위치: `src/main/resources/db/migration/mysql`
   - 주요 테이블: `users`, `auth_sessions`, `assignment2_workspaces`, `assignment2_members`, `assignment2_tasks`, `assignment2_meetings`, `assignment2_activities`, `assignment2_reports`
   - 운영 DB는 Hibernate `update`가 아니라 Flyway SQL로 변경된다고 적습니다.

3. 배포/실행 가이드
   - MySQL profile 실행 전 DB backup 권장
   - `SPRING_PROFILES_ACTIVE=mysql`
   - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` 설정 필요

## 팀원에게 전달할 요약

```text
이번 백엔드 제품화 1차 변경은 API path 변경이 아니라 DB 운영 방식과 API 문서화 추가입니다.
MySQL profile은 Flyway migration으로 스키마를 만들고, Hibernate는 validate만 수행합니다.
OpenAPI JSON은 /v3/api-docs에서 확인할 수 있습니다.
프론트 기존 호출 path는 그대로 유지됩니다.
```
