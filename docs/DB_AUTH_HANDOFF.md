# DB/Auth Handoff

이 문서는 DB 연결, 로그인 저장, 프론트 API 호출 방식 변경 사항을 팀원이나 Notion에 옮길 때 바로 확인하기 위한 요약입니다.

## 이번 변경의 목적

- 로컬/배포 환경에서 MySQL을 기준 DB로 사용합니다.
- 회원가입, 로그인, 세션 정보를 서버 메모리가 아니라 MySQL에 저장합니다.
- 로그인한 사용자별로 프로젝트/업무 워크스페이스가 분리되도록 합니다.
- 리포트 PDF가 단순 개수 요약이 아니라 업무, 회의, 리스크, 활동 내역을 포함하도록 보강합니다.

## DB 변경 사항

### 새 테이블

- `users`
  - 회원 기본 정보 저장
  - 주요 필드: `id`, `email`, `password_hash`, `name`, `university`, `phone`, `created_at`, `updated_at`
- `auth_sessions`
  - 로그인 토큰/세션 저장
  - 주요 필드: `id`, `user_id`, `access_token`, `refresh_token`, `revoked`, `expires_at`, `created_at`

### 기존 테이블 변경

- `assignment2_workspaces`
  - `owner_email` 컬럼 추가
  - 로그인한 사용자 이메일 기준으로 워크스페이스를 분리하기 위한 필드입니다.
  - 비로그인 legacy `/api/mobile/**` 요청은 `owner_email`이 비어 있는 별도 워크스페이스만 사용합니다.

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
- `GET /api/invitations/{inviteCode}`
- `/api/mobile/**` legacy demo API

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
- 리포트: PDF 리포트가 업무 상태, 담당자, 회의, 리스크, 최근 활동을 포함한다고 보강
- 제약: MVP 단계에서는 외부 응답의 `projectId`가 아직 `1` 고정 구조임을 내부 메모에 남기기

## 현재 남은 제약

- 프로젝트 ID 체계는 아직 MVP 호환을 위해 `1` 고정 응답을 유지합니다.
- PDF 생성기는 최소 구현이라 한 페이지 중심이며, 한글 폰트 내장은 아직 없습니다.
- 운영 배포용 DB 마이그레이션 도구는 아직 별도로 구성하지 않았습니다. 현재는 Hibernate `ddl-auto=update` 기반입니다.

## 검증한 흐름

1. 로컬 MySQL 실행
2. `mysql` 프로필로 백엔드 실행
3. 회원가입
4. 로그인
5. 프로젝트 생성
6. 태스크 생성
7. 백엔드 재시작
8. 같은 계정으로 로그인 후 프로젝트/태스크 재조회
