# Backend Release Readiness Check

이 문서는 API path/요청/응답 스펙을 더 바꾸지 않는다는 전제에서, 출시 전 확인해야 할 백엔드 항목을 정리한 체크 문서다.

## 이번 보완 범위

- API path 변경 없음
- request body 필드 변경 없음
- response result 필드 변경 없음
- MySQL/prod profile은 Flyway migration + Hibernate `validate` 유지
- prod profile에서는 OpenAPI 문서와 legacy `/api/mobile/**` 공개를 기본 비활성화
- demo/local 기본값은 기존 MVP 호환을 위해 legacy `/api/mobile/**` 공개 유지

## 현재 통과한 항목

- demo profile 자동 테스트 통과
- 로컬 MySQL live smoke 검증 통과
- 회원가입, 로그인, refresh rotation, 이전 refresh token 재사용 차단 확인
- 실제 DB `projectId` 기반 프로젝트 생성/목록 조회 확인
- 태스크 생성/상태 변경/잘못된 상태값 거부 확인
- 태스크 의존관계 추가 및 순환 의존성 차단 확인
- 회의 생성, 리포트 생성, PDF 다운로드 확인
- 초대 링크 조회/수락, MEMBER 강제 저장, MEMBER의 초대 생성 차단 확인
- 한국어 데이터 입력 후 PDF 텍스트 추출 확인

## 출시 전 남은 항목

| 항목 | 현재 상태 | 출시 전 처리 |
| --- | --- | --- |
| MySQL 자동 통합 테스트 | live smoke로 확인했지만 CI 자동화는 아님 | CI에서 MySQL service container를 붙여 `mysql` profile 테스트 실행 |
| 프론트 실제 E2E | 백엔드 API 단독 검증 완료 | 프론트에서 로그인→프로젝트→태스크→초대→리포트 플로우 검증 |
| 운영 환경변수 | prod profile 분리 완료 | 배포 환경에 `SPRING_DATASOURCE_*`, `APP_JWT_SECRET`, `APP_CORS_ALLOWED_ORIGINS` 설정 |
| DB 백업 | 코드 범위 밖 | 운영 MySQL 자동 백업/복구 절차 마련 |
| 로그/모니터링 | 기본 Spring 로그 수준 | 배포 플랫폼 로그 수집, 장애 알림, 요청 추적 도입 |
| CI/CD | 별도 구성 필요 | 테스트/빌드/배포 자동화 파이프라인 구성 |
| legacy `/api/mobile/**` | demo 호환 때문에 local 기본 공개 | prod 기본 비공개. 프론트는 공식 API 사용 |
| 응답 wrapper 혼재 | 문서화 완료 | 당장 수정하지 않음. 신규 API는 `SpecResponse` 기준 유지 |
| Supabase profile | `ddl-auto=update` 유지 | 최종 DB가 MySQL이면 사용하지 않음. Supabase 사용 시 별도 PostgreSQL migration 필요 |

## 운영 배포 기본값

`application-prod.properties` 기준:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration/mysql
springdoc.api-docs.enabled=${SPRINGDOC_API_DOCS_ENABLED:false}
app.security.public-api-docs=${APP_PUBLIC_API_DOCS:false}
app.security.public-roadmap=${APP_PUBLIC_ROADMAP:false}
app.security.public-legacy-mobile-api=${APP_PUBLIC_LEGACY_MOBILE_API:false}
```

## 프론트 담당자에게 전달할 핵심

- 이제 API path는 고정된 것으로 보고 작업한다.
- 인증 필요한 API에는 `Authorization: Bearer ...`를 항상 붙인다.
- access token 만료 시 `POST /api/auth/refresh`로 새 토큰을 받고, access/refresh token 모두 교체한다.
- 프로젝트 ID는 `1` 고정이 아니라 서버 응답의 실제 `projectId`를 사용한다.
- prod에서는 `/api/mobile/**`가 막힐 수 있으므로 공식 API만 사용한다.
- `403`/`3008`은 권한 없음 처리, `401`/`3001`은 로그인 필요 처리로 보면 된다.

## 팀 내 결정 필요

- 최종 운영 DB는 MySQL로 고정할지 결정한다.
- Supabase profile을 유지할지 제거할지 결정한다.
- `/api/mobile/**`를 완전히 제거할지, demo/local 전용으로 남길지 결정한다.
- CI에서 실제 MySQL 테스트를 돌릴 수 있는 환경을 정한다.
