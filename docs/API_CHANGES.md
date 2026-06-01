# TeamPulse API Changes

Last updated: 2026-05-28
Scope: current backend source 기준 MVP/legacy API 정리 현황

## Current public API surface

현재 백엔드는 과제/프론트 제출 기준으로 아래 `/api` 계열을 공식 API로 유지한다.

### Auth

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/logout`

### User

- `GET /api/users/me`

### Project / dashboard / members

- `POST /api/projects`
- `GET /api/projects`
- `GET /api/projects/{projectId}`
- `PATCH /api/projects/{projectId}`
- `GET /api/projects/{projectId}/dashboard`
- `GET /api/projects/{projectId}/members`
- `DELETE /api/projects/{projectId}/members/me`
- `DELETE /api/projects/{projectId}/members/{memberId}`

### Tasks

- `POST /api/projects/{projectId}/tasks`
- `GET /api/projects/{projectId}/tasks`
- `PATCH /api/tasks/{taskId}`
- `DELETE /api/tasks/{taskId}`
- `PATCH /api/tasks/{taskId}/status`
- `POST /api/tasks/{taskId}/dependencies`
- `DELETE /api/tasks/{taskId}/dependencies/{dependencyId}`

### Meetings

- `POST /api/projects/{projectId}/meetings`
- `GET /api/projects/{projectId}/meetings`
- `GET /api/meetings/{meetingId}`

### Invitations

- `POST /api/projects/{projectId}/invitations`
- `GET /api/invitations/{inviteCode}`
- `POST /api/invitations/{inviteCode}/accept`

### Risks / activity / reports

- `GET /api/projects/{projectId}/activity-logs`
- `GET /api/projects/{projectId}/risks`
- `POST /api/projects/{projectId}/reports`
- `GET /api/reports/{reportId}/download`

### Runtime

- `GET /api/health`

## Removed or no longer documented legacy endpoints

아래 API는 현재 소스 기준 공식 API로 보지 않는다. 테스트/문서 작성 시 제외한다.

- `/api/mobile/**`
- `/api/account`
- `/api/account/activities`
- `/api/projects/{projectId}/invite-links`
- `/api/projects/{projectId}/risks/{riskId}/actions`
- `/api/roadmap`

## Current compatibility notes

- Java package 이름에는 아직 `mobile`이 남아 있지만, 실제 HTTP path는 `/api/mobile/**`이 아니다.
- `mobile` package는 현재 프로젝트/태스크/회의록/초대/리포트 핵심 구현을 담고 있으므로 단순 삭제 대상이 아니다.
- 패키지명 정리는 별도 리팩터링으로 분리해야 한다. 현재 단계에서는 API 동작 안정성과 테스트 커버리지를 우선한다.
- `demo` profile은 로컬/테스트용 인메모리 저장소이고, 실제 배포는 `mysql` profile을 기준으로 한다.

## Cleanup completed

- 사용되지 않는 `UpdateAccountRequest` DTO를 제거했다.
- 문서에 남아 있던 `/api/mobile/**`, `/api/account/**`, `/api/roadmap`, `/invite-links` 중심 설명을 현재 코드 기준으로 정리했다.
- `GET /api/health`는 프론트 배포/서버 상태 확인용으로 유지하고 테스트 대상으로 포함한다.

## Follow-up candidates

- `com.teampulse.backend.mobile` 패키지를 `workspace` 또는 `project` 계열 패키지로 리네이밍.
- `Mobile*Entity`, `Mobile*UseCase` 클래스명을 `Workspace*` 또는 도메인별 이름으로 정리.
- 위 리네이밍은 import와 테스트 전체에 영향을 주므로 API 안정화 PR과 분리해서 처리한다.
