# TeamPulse API Changes

이 문서는 Notion/API 명세와 실제 구현이 어긋나지 않도록, API 경로, 요청/응답, 인증 방식, DB 저장 방식이 바뀔 때마다 기록하는 변경 로그입니다.

## 2026-05-03

### 1. 리포트 PDF 내용 확장

- 변경 파일: `src/main/java/com/teampulse/backend/mobile/api/ProjectApiController.java`
- 영향 API:
  - `POST /api/projects/{projectId}/reports`
  - `GET /api/reports/{reportId}/download`
  - `GET /api/projects/{projectId}/reports/{reportId}/download`
- API 경로 변경: 없음
- 요청 형식 변경: 없음
- JSON 응답 형식 변경: 없음
- 다운로드 파일 형식 변경: 없음. 기존처럼 `application/pdf`
- 변경 내용:
  - 기존 PDF 본문은 프로젝트명, 기간, 상태, 업무/회의/리스크 개수만 표시했습니다.
  - 이제 PDF 본문에 프로젝트 요약, 업무 상태 분포, 담당자, 업무 상세, 회의 요약, 리스크 신호, 최근 활동 로그가 함께 표시됩니다.
- Notion/API 명세 반영:
  - endpoint 자체는 수정하지 않아도 됩니다.
  - 기능 설명에는 "PDF 리포트는 업무, 회의, 리스크, 활동 로그를 요약해 내려받는다" 정도로 보강하면 됩니다.

### 2. MySQL 프로필 인증/세션 저장 전환

- 변경 파일:
  - `src/main/java/com/teampulse/backend/auth/infrastructure/InMemoryAuthUserRepository.java`
  - `src/main/java/com/teampulse/backend/auth/infrastructure/DemoTokenIssuer.java`
  - `src/main/java/com/teampulse/backend/auth/infrastructure/JpaAuthUserRepository.java`
  - `src/main/java/com/teampulse/backend/auth/infrastructure/JpaTokenIssuer.java`
  - `src/main/java/com/teampulse/backend/auth/persistence/*`
- 영향 API:
  - `POST /api/auth/signup`
  - `POST /api/auth/login`
  - `POST /api/auth/logout`
- API 경로 변경: 없음
- 요청 형식 변경: 없음
- JSON 응답 형식 변경: 없음
- 변경 내용:
  - `demo` 프로필은 기존처럼 메모리 인증 저장소를 사용합니다.
  - `mysql` 프로필은 `users` 테이블에 회원 정보를 저장합니다.
  - `mysql` 프로필은 `auth_sessions` 테이블에 access token, refresh token, 만료/폐기 상태를 저장합니다.
  - 서버를 재시작해도 기존 회원으로 다시 로그인할 수 있습니다.
- Notion/API 명세 반영:
  - endpoint 문서는 유지해도 됩니다.
  - 구현 방식 설명에서 "회원/세션 정보는 MySQL에 저장"으로 수정해야 합니다.

### 3. 프로젝트 API 인증 필수화

- 변경 파일:
  - `src/main/java/com/teampulse/backend/common/config/SecurityConfig.java`
  - `../TeamPulse-frontend/src/apis/projects.ts`
  - `../TeamPulse-frontend/src/apis/tasks.ts`
- 인증 필요 API:
  - `GET /api/users/me`
  - `GET/PATCH /api/account`
  - `GET/POST/PATCH /api/projects/**`
  - `GET/POST/PATCH/DELETE /api/tasks/**`
  - `GET/POST /api/meetings/**`
  - `GET /api/reports/**`
  - `POST /api/invitations/{inviteCode}/accept`
- 공개 유지 API:
  - `GET /api/health`
  - `GET /api/roadmap`
  - `POST /api/auth/signup`
  - `POST /api/auth/login`
  - `GET /api/invitations/{inviteCode}`
  - `GET/POST/PATCH/DELETE /api/mobile/**` legacy demo API
- API 경로 변경: 없음
- 요청 형식 변경: 없음
- JSON 응답 형식 변경: 없음
- 인증 헤더 변경:
  - 프로젝트/업무/회의/리포트 관련 API는 `Authorization: Bearer ...` 헤더가 필요합니다.
- 프론트 수정 이유:
  - 기존 프론트의 일부 프로젝트/태스크 조회 호출은 `auth=false`로 설정되어 토큰을 보내지 않았습니다.
  - 백엔드가 인증 필수 구조로 바뀌면 해당 호출이 `401 Unauthorized`로 깨질 수 있습니다.
  - 그래서 UI는 건드리지 않고 API 클라이언트 호출 옵션만 로그인 토큰을 보내도록 정리했습니다.
- Notion/API 명세 반영:
  - 각 프로젝트/업무/회의/리포트 API에 "로그인 필요" 또는 "Authorization 헤더 필요" 표시가 필요합니다.

### 4. MySQL 프로필 워크스페이스 사용자 분리

- 변경 파일:
  - `src/main/java/com/teampulse/backend/mobile/persistence/MobileWorkspaceEntity.java`
  - `src/main/java/com/teampulse/backend/mobile/persistence/MobileWorkspaceRepository.java`
  - `src/main/java/com/teampulse/backend/mobile/application/service/JpaWorkspaceService.java`
- 영향 API:
  - `POST /api/projects`
  - `GET /api/projects`
  - `GET /api/projects/{projectId}`
  - `GET/POST /api/projects/{projectId}/tasks`
  - `GET/POST /api/projects/{projectId}/meetings`
  - `POST /api/projects/{projectId}/reports`
- API 경로 변경: 없음
- 요청 형식 변경: 없음
- JSON 응답 형식 변경: 없음
- DB 스키마 변경:
  - `assignment2_workspaces.owner_email` 컬럼 추가
- 변경 내용:
  - MySQL 프로필에서는 로그인한 사용자의 이메일 기준으로 워크스페이스를 조회/생성합니다.
  - 서로 다른 사용자 계정의 프로젝트/업무 데이터가 같은 전역 워크스페이스에 섞이지 않도록 분리했습니다.
  - 비로그인 legacy `/api/mobile/**` 요청은 `owner_email`이 비어 있는 별도 워크스페이스만 사용합니다.
- 남은 제약:
  - 외부 API의 `projectId`는 아직 MVP 호환을 위해 `1` 고정 응답을 유지합니다.
  - 실제 다중 프로젝트 ID 체계로 바꾸면 endpoint 응답 의미가 바뀌므로 별도 API 문서 업데이트가 필요합니다.

### 5. 로컬 MySQL 검증 기록

- 설치: `winget install --id Oracle.MySQL -e --silent --accept-package-agreements --accept-source-agreements`
- 로컬 데이터 디렉터리: `C:\tmp\teampulse-mysql-data`
- 실행 포트: `127.0.0.1:3306`
- 검증 완료:
  - MySQL 8.4.8 접속 성공
  - `teampulse` DB/계정 생성
  - `SPRING_PROFILES_ACTIVE=mysql` 백엔드 실행 성공
  - 회원가입 -> 프로젝트 생성 -> 태스크 생성 -> 서버 재시작 -> 로그인 -> 프로젝트/태스크 재조회 성공
