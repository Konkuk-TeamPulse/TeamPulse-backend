# DB/Auth Handoff

이 문서는 DB 연결, 로그인 저장, 프론트 API 호출 방식 변경 사항을 팀원이나 Notion에 옮길 때 바로 확인하기 위한 요약입니다.

## 이번 변경의 목적

- 로컬/배포 환경에서 MySQL을 기준 DB로 사용합니다.
- 회원가입, 로그인, 세션 정보를 서버 메모리가 아니라 MySQL에 저장합니다.
- 로그인한 사용자별로 프로젝트/업무 워크스페이스가 분리되도록 합니다.
- MySQL 프로필에서는 실제 DB의 `assignment2_workspaces.id`를 `projectId`로 사용해 여러 프로젝트를 구분합니다.
- MySQL 프로필에서는 access token을 JWT로 발급하고, refresh token은 DB에 원문이 아니라 hash로 저장합니다.
- 운영 배포에서는 `prod` profile로 DB 계정과 JWT secret을 환경변수에서 받도록 분리했습니다.
- `prod` profile에서도 MySQL/JPA 인증과 워크스페이스 구현체가 뜨도록 정리했습니다.
- 리포트 PDF가 단순 개수 요약이 아니라 업무, 회의, 리스크, 활동 내역을 포함하도록 보강합니다.

## DB 변경 사항

### 새 테이블

- `users`
  - 회원 기본 정보 저장
  - 주요 필드: `id`, `email`, `password_hash`, `name`, `university`, `phone`, `created_at`, `updated_at`
- `auth_sessions`
  - 로그인 토큰/세션 저장
  - 주요 필드: `id`, `user_id`, `access_token`, `refresh_token`, `revoked`, `expires_at`, `created_at`
  - MySQL profile에서는 `access_token`, `refresh_token` 컬럼에 토큰 원문이 아니라 SHA-256 hash가 저장됩니다.

### 기존 테이블 변경

- `assignment2_workspaces`
  - `owner_email` 컬럼 추가
  - 로그인한 사용자 이메일 기준으로 워크스페이스를 분리하기 위한 필드입니다.
  - `id` 값이 API 응답의 실제 `projectId`로 사용됩니다.
  - 비로그인 legacy `/api/mobile/**` 요청은 `owner_email`이 비어 있는 별도 워크스페이스만 사용합니다.
- `assignment2_members`
  - `email` 컬럼 추가
  - 초대 수락 사용자를 이메일 기준으로 프로젝트 멤버와 연결하기 위한 필드입니다.

## API 변경 사항

상세 변경 로그는 `docs/API_CHANGES.md`에 정리했습니다.

### 인증 필요로 정리된 API

- `/api/users/me`
- `/api/account/**`
- `/api/projects/**`
- `/api/tasks/**`
- `/api/meetings/**`
- `/api/reports/**`
- `/api/invitations/**` 중 초대 수락 API

### 공개 유지 API

- `/api/health`
- `/api/roadmap`
- `/api/auth/signup`
- `/api/auth/login`
- `/api/auth/refresh`
- `GET /api/invitations/{inviteCode}`
- `/api/mobile/**` legacy demo API

### 인증/권한 보완

- `POST /api/auth/refresh`
  - refresh token을 받아 새 access token과 refresh token을 발급합니다.
  - 기존 refresh token은 즉시 폐기됩니다.
  - 프론트는 refresh 성공 시 저장된 refresh token을 새 값으로 교체해야 합니다.
- 로그인 실패 응답
  - 없는 이메일과 틀린 비밀번호는 모두 401/code `2014`로 통일했습니다.
  - 반복 실패 시 429/code `2015`로 일정 시간 차단됩니다.
  - 실패 기록은 만료 cleanup과 최대 추적 이메일 수 제한으로 관리합니다.
- 팀 관리 권한
  - 프로젝트 수정, 멤버 추가/삭제, 초대 링크 생성은 `LEADER`만 가능합니다.
  - 권한 부족 시 403/code `3008`을 반환합니다.
  - 프로젝트 멤버가 아닌 사용자의 접근도 403/code `3008`로 반환합니다.
- 초대 수락
  - 요청 body에 `role`이 들어와도 서버는 초대 수락자를 항상 `MEMBER`로 저장합니다.

## 운영 환경변수

`prod` profile 기준 필수 값입니다.

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET`

선택 값입니다.

- `APP_CORS_ALLOWED_ORIGINS`
- `APP_JWT_ACCESS_TOKEN_MINUTES`
- `APP_JWT_REFRESH_TOKEN_DAYS`
- `APP_LOGIN_MAX_FAILURES`
- `APP_LOGIN_LOCKOUT_MINUTES`
- `APP_LOGIN_MAX_TRACKED_EMAILS`
- `SPRINGDOC_API_DOCS_ENABLED`
- `APP_PUBLIC_API_DOCS`

## 프론트 수정 이유

프론트 UI를 바꾼 것이 아니라, 인증이 필요한 API 호출에서 토큰이 빠지지 않도록 API 클라이언트 옵션만 정리했습니다.

- `src/apis/projects.ts`
  - 프로젝트 상세 조회, 프로젝트 수정, 대시보드 조회에서 `auth=false` 제거
- `src/apis/tasks.ts`
  - 태스크 목록 조회에서 `auth=false` 제거

이 변경이 없으면 백엔드에서 프로젝트/태스크 API를 로그인 필수로 바꾼 뒤 프론트가 `401 Unauthorized`를 받을 수 있습니다.

## Notion에 반영할 항목

- DB: MySQL 사용 명시
- 인증: 회원/세션 정보가 MySQL의 `users`, `auth_sessions`에 저장된다고 수정
- API: 프로젝트/업무/회의/리포트 API는 `Authorization: Bearer ...` 필요하다고 표시
- API: refresh token 갱신 API `POST /api/auth/refresh` 추가
- API: 없는 이메일/비밀번호 오류는 401/code `2014`로 동일하게 반환한다고 표시
- API: 팀 관리 API는 LEADER 권한 필요, 실패 시 403/code `3008`로 표시
- API: `projectId`는 `1` 고정값이 아니라 `assignment2_workspaces.id` 기반 실제 DB ID라고 수정
- API: `GET /api/projects`는 로그인 사용자가 소유자이거나 멤버로 속한 모든 프로젝트를 반환한다고 정리
- API: 초대 수락 후 프로젝트 목록을 다시 조회하면 초대받은 프로젝트가 `MEMBER` 역할로 나타난다고 정리
- 리포트: PDF 리포트가 태스크 완료율, 업무 상태, 담당자, 회의, 리스크, 다음 액션, 최근 활동을 포함한다고 보강

## 현재 남은 제약

- `demo` 프로필은 기존 테스트 호환을 위해 인메모리 단일 프로젝트 중심입니다. 실제 앱 검증은 `mysql` 프로필 기준으로 봐야 합니다.
- PDF 생성기는 HTML/CSS 템플릿 기반으로 보강되었습니다. 다만 대형 프로젝트의 상세 부록 자동 분리나 팀원별 workload 추세 그래프는 아직 고도화 대상입니다.
- 한글 폰트는 서버 OS의 한글 폰트 또는 `app.report.pdf.font-path` 설정값을 사용합니다. 배포 서버에 한글 폰트가 없으면 폰트 경로를 환경별로 지정해야 합니다.
- MySQL 프로필은 Flyway migration + Hibernate `validate` 기반으로 전환했습니다.
- Supabase 프로필은 아직 별도 PostgreSQL migration을 구성하지 않았고, 기존 Hibernate `ddl-auto=update` 설정을 유지합니다.
- 운영 profile은 `application-prod.properties`를 사용하며 OpenAPI 공개를 기본 비활성화합니다.
- `prod` profile 단독 실행도 JPA 인증/워크스페이스 구현체를 사용합니다.

## 검증한 흐름

1. 로컬 MySQL 실행
2. `mysql` 프로필로 백엔드 실행
3. 회원가입
4. 로그인
5. 프로젝트 생성
6. 같은 계정으로 프로젝트 2개 생성
7. 프로젝트별 태스크 생성 및 목록 격리 확인
8. projectId 없는 태스크 상태 변경 API가 소속 프로젝트를 찾아 정상 반영되는지 확인
9. 프로젝트 B 회의/리포트 생성
10. 프로젝트 B 초대 링크 생성/조회/수락
11. 초대받은 멤버가 프로젝트 B만 조회하고 프로젝트 A 접근은 차단되는지 확인
12. refresh token rotation 확인
13. 멤버가 초대 링크를 생성하려 할 때 403/code `3008` 반환 확인
14. `auth_sessions`에 token hash만 저장되는지 확인
