# API Implementation Progress

Date: 2026-04-29
Branch: `feature-api-spec-refine`

This document records the current backend completion level against the Notion API checklist.

## Completed To BE-Done Level

### 1. Signup

Endpoint:

```text
POST /api/auth/signup
```

Implemented behavior:

- Uses `email` as the user identifier instead of student number.
- Normalizes email to lowercase before duplicate checks and storage.
- Validates required fields with Jakarta Bean Validation.
- Hashes passwords with BCrypt through `PasswordEncoder`.
- Rejects duplicate emails with HTTP `409` and `responseCode: 2012`.
- Returns the Notion-style response wrapper:
  - `isSuccess`
  - `responseCode`
  - `responseMessage`
  - `result`
- Returns demo access/refresh tokens in `jwtInfo`.

Main code:

- `src/main/java/com/teampulse/backend/auth/api/AuthApiController.java`
- `src/main/java/com/teampulse/backend/auth/application/AuthService.java`
- `src/main/java/com/teampulse/backend/auth/domain/AuthUser.java`
- `src/main/java/com/teampulse/backend/auth/infrastructure/InMemoryAuthUserRepository.java`
- `src/main/java/com/teampulse/backend/auth/infrastructure/DemoTokenIssuer.java`

### 2. Login

Endpoint:

```text
POST /api/auth/login
```

Implemented behavior:

- Uses email and password.
- Normalizes email to lowercase before lookup.
- Verifies submitted password against the BCrypt hash.
- Rejects missing user with HTTP `404` and `responseCode: 2013`.
- Rejects password mismatch with HTTP `401` and `responseCode: 2014`.
- Returns demo access/refresh tokens in `jwtInfo`.

Main code:

- `src/main/java/com/teampulse/backend/auth/dto/LoginRequest.java`
- `src/main/java/com/teampulse/backend/auth/dto/LoginResponse.java`
- `src/main/java/com/teampulse/backend/auth/application/AuthService.java`

### 3. Logout

Endpoint:

```text
POST /api/auth/logout
```

Implemented behavior:

- Accepts `refreshToken` in the request body.
- Tracks active demo refresh tokens through `RefreshTokenRegistry`.
- Revokes the refresh token on logout.
- Rejects missing token with HTTP `400` and `responseCode: 2020`.
- Rejects invalid or already revoked token with HTTP `401` and `responseCode: 2009`.

Main code:

- `src/main/java/com/teampulse/backend/auth/dto/LogoutRequest.java`
- `src/main/java/com/teampulse/backend/auth/application/RefreshTokenRegistry.java`
- `src/main/java/com/teampulse/backend/auth/application/InvalidRefreshTokenException.java`
- `src/main/java/com/teampulse/backend/auth/infrastructure/DemoTokenIssuer.java`

### 4. Project Create

Endpoint:

```text
POST /api/projects
```

Implemented behavior:

- Accepts the Notion API request shape:
  - `projectName`
  - `subject`
  - `description`
  - `startDate`
  - `endDate`
- Maps the request to the existing workspace lifecycle use case.
- Stores project description and start date in the workspace state.
- Returns the Notion API response shape:
  - `projectId`
  - `projectName`
  - `role`
- Uses the standard spec wrapper:
  - `isSuccess`
  - `responseCode`
  - `responseMessage`
  - `result`

Main code:

- `src/main/java/com/teampulse/backend/mobile/api/ProjectApiController.java`
- `src/main/java/com/teampulse/backend/mobile/dto/ProjectCreateRequest.java`
- `src/main/java/com/teampulse/backend/mobile/dto/ProjectCreateResponse.java`
- `src/main/java/com/teampulse/backend/mobile/dto/TeamProfile.java`
- `src/main/java/com/teampulse/backend/mobile/application/service/InMemoryWorkspaceService.java`
- `src/main/java/com/teampulse/backend/mobile/application/service/JpaWorkspaceService.java`

### 5. Project List, Detail, Update

Endpoints:

```text
GET   /api/projects
GET   /api/projects/{projectId}
PATCH /api/projects/{projectId}
```

Implemented behavior:

- `GET /api/projects` returns `projectId`, `projectName`, `subject`, `role`, `endDate`.
- `GET /api/projects/{projectId}` returns `projectId`, `projectName`, `subject`, `description`, `startDate`, `endDate`, `memberCount`.
- `PATCH /api/projects/{projectId}` accepts the same Notion project fields and returns updated project fields plus `updatedAt`.
- The current MVP still supports one demo project id: `1`.

Main code:

- `src/main/java/com/teampulse/backend/mobile/dto/ProjectSummaryView.java`
- `src/main/java/com/teampulse/backend/mobile/dto/ProjectDetailView.java`
- `src/main/java/com/teampulse/backend/mobile/dto/ProjectUpdateRequest.java`
- `src/main/java/com/teampulse/backend/mobile/dto/ProjectUpdateResponse.java`

### 6. Task Create, List, Update, Delete, Status Change

Endpoints:

```text
POST   /api/projects/{projectId}/tasks
GET    /api/projects/{projectId}/tasks
PATCH  /api/tasks/{taskId}
DELETE /api/tasks/{taskId}
PATCH  /api/tasks/{taskId}/status
```

Implemented behavior:

- `POST /api/projects/{projectId}/tasks` accepts the Notion request shape:
  - `title`
  - `description`
  - `assigneeId`
  - `dueDate`
- `GET /api/projects/{projectId}/tasks` returns `taskId`, `title`, `status`, `assigneeName`, `dueDate`.
- `PATCH /api/tasks/{taskId}` accepts `title`, `description`, `assigneeId`, `dueDate`.
- `DELETE /api/tasks/{taskId}` returns success with `result: null`.
- `PATCH /api/tasks/{taskId}/status` returns `taskId` and `status`.
- `/api/tasks/**` remains authenticated instead of being opened with `permitAll`.
- A demo access-token filter validates the demo access token issued by signup/login so the protected task endpoints can be called in live smoke tests.

Main code:

- `src/main/java/com/teampulse/backend/mobile/api/ProjectTaskApiController.java`
- `src/main/java/com/teampulse/backend/mobile/api/TaskApiController.java`
- `src/main/java/com/teampulse/backend/mobile/dto/TaskCreateSpecRequest.java`
- `src/main/java/com/teampulse/backend/mobile/dto/TaskCreateSpecResponse.java`
- `src/main/java/com/teampulse/backend/mobile/dto/TaskSummarySpecResponse.java`
- `src/main/java/com/teampulse/backend/mobile/dto/TaskUpdateSpecRequest.java`
- `src/main/java/com/teampulse/backend/mobile/dto/TaskUpdateSpecResponse.java`
- `src/main/java/com/teampulse/backend/mobile/dto/TaskStatusSpecResponse.java`
- `src/main/java/com/teampulse/backend/auth/infrastructure/DemoAccessTokenAuthenticationFilter.java`

### 7. Meeting Create, List, Detail

Endpoints:

```text
POST /api/projects/{projectId}/meetings
GET  /api/projects/{projectId}/meetings
GET  /api/projects/{projectId}/meetings/{meetingId}
```

Implemented behavior:

- `POST /api/projects/{projectId}/meetings` creates a meeting record from the existing meeting request fields.
- `GET /api/projects/{projectId}/meetings` returns meeting summaries in the standard spec wrapper.
- `GET /api/projects/{projectId}/meetings/{meetingId}` returns one meeting record.
- Existing `/api/mobile/meetings` remains available for the old MVP frontend flow.

Main code:

- `src/main/java/com/teampulse/backend/mobile/api/ProjectMeetingApiController.java`
- `src/main/java/com/teampulse/backend/mobile/dto/MeetingSpecResponse.java`

## 2026-04-29 Notion In-Progress API Alignment

Scope:

- Only the rows marked `in progress` in the Notion API table were aligned in this batch.
- `GET /api/projects/{projectId}/risks` was not changed because the latest table marked risk signal lookup as `not started`.
- Existing `/api/mobile/**` MVP endpoints were kept for frontend compatibility.

Completed endpoints:

```text
POST   /api/projects/{projectId}/meetings
GET    /api/projects/{projectId}/dashboard
GET    /api/users/me
POST   /api/tasks/{taskId}/dependencies
DELETE /api/tasks/{taskId}/dependencies/{dependencyId}
GET    /api/projects/{projectId}/activity-logs
DELETE /api/projects/{projectId}/members/me
GET    /api/projects/{projectId}/members
```

Implemented behavior:

- Meeting creation now accepts the Notion request shape: `meetingDate`, `agenda`, `content`, `decisions`, `attendeeIds`, and `actionItems`.
- Meeting records retain `content`, `attendeeIds`, and structured `actionItems` so list/detail responses do not lose the submitted meeting data.
- Project dashboard returns task summary, schedule summary, member workload, risk summary, and dashboard risk items.
- Current account lookup returns the standard spec wrapper from `GET /api/users/me`. This API remains authenticated by Spring Security and resolves the demo access token user when an `Authorization` header is supplied.
- Task dependency add/delete now accepts task IDs instead of the earlier MVP title-based dependency API shape.
- Spec-aligned controllers now return the Notion-style failure wrapper for validation and domain errors.
- Activity logs now return `logId`, `action`, `content`, `userName`, and `createdAt` through `/activity-logs`.
- Team member list now returns Notion-style `memberId`, `name`, `email`, and `role`.
- Team leave now returns a standard success wrapper with a `null` result.

Main code:

- `src/main/java/com/teampulse/backend/mobile/api/ProjectApiController.java`
- `src/main/java/com/teampulse/backend/mobile/api/ProjectMeetingApiController.java`
- `src/main/java/com/teampulse/backend/mobile/api/TaskApiController.java`
- `src/main/java/com/teampulse/backend/mobile/api/MobileSpecExceptionHandler.java`
- `src/main/java/com/teampulse/backend/mobile/dto/UserMeResponse.java`
- `src/main/java/com/teampulse/backend/mobile/dto/DashboardResponse.java`
- `src/main/java/com/teampulse/backend/mobile/dto/ActivityLogSpecResponse.java`
- `src/main/java/com/teampulse/backend/mobile/dto/MemberSpecResponse.java`
- `src/main/java/com/teampulse/backend/mobile/dto/MeetingActionItemView.java`
- `src/main/java/com/teampulse/backend/mobile/dto/MeetingCreateSpecRequest.java`
- `src/main/java/com/teampulse/backend/mobile/dto/MeetingCreateSpecResponse.java`
- `src/main/java/com/teampulse/backend/mobile/dto/TaskDependencySpecRequest.java`
- `src/main/java/com/teampulse/backend/mobile/dto/TaskDependencySpecResponse.java`

## Verification

Focused auth API test:

```powershell
.\mvnw.cmd '-Dtest=AuthApiControllerTest' test '-Dspring.profiles.active=demo'
```

Result:

```text
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Full backend test:

```powershell
.\mvnw.cmd test '-Dspring.profiles.active=demo'
```

Result:

```text
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Focused Notion in-progress API test:

```powershell
.\mvnw.cmd '-Dtest=WorkspaceControllerTest' test '-Dspring.profiles.active=demo'
```

Result:

```text
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Live HTTP smoke test on `http://127.0.0.1:18080`:

```text
POST /api/auth/signup  -> responseCode 1000
POST /api/auth/login   -> responseCode 1000
POST /api/auth/logout  -> responseCode 1000
POST /api/auth/logout with invalid token -> HTTP 401, responseCode 2009
```

Project live HTTP smoke test on `http://127.0.0.1:18080`:

```text
POST  /api/projects       -> responseCode 1000, projectName TeamPulse Live
GET   /api/projects       -> 1 project
GET   /api/projects/1     -> description Live project smoke
PATCH /api/projects/1     -> responseCode 1000, projectName TeamPulse Live Updated
```

Task live HTTP smoke test on `http://127.0.0.1:18080`:

```text
POST   /api/auth/signup              -> responseCode 1000, demo access token issued
POST   /api/projects/1/tasks         -> responseCode 1000, task created
PATCH  /api/tasks/{taskId}           -> responseCode 1000 with Authorization header
PATCH  /api/tasks/{taskId}/status    -> responseCode 1000 with Authorization header
DELETE /api/tasks/{taskId}           -> responseCode 1000 with Authorization header
```

Meeting live HTTP smoke test on `http://127.0.0.1:18080`:

```text
POST /api/projects/1/meetings              -> responseCode 1000
GET  /api/projects/1/meetings              -> 1 meeting
GET  /api/projects/1/meetings/{meetingId}  -> responseCode 1000
```

Review follow-up verification on 2026-04-30:

```text
GET  /api/users/me without token -> HTTP 401, responseCode 3001
GET  /api/users/me with demo access token -> token user fields returned
POST /api/tasks/{taskId}/dependencies with same task id -> HTTP 400, responseCode 3006
GET  /api/projects/1/meetings -> content, attendeeIds, actionItems retained
GET  /api/projects/1/meetings/{meetingId} -> action item assignee and due date retained
```

## Remaining Auth Hardening

The current auth implementation is complete enough for demo/MVP API behavior, but these items remain for production hardening:

- Replace demo tokens with signed JWT access/refresh tokens.
- Persist users and refresh tokens in database tables.
- Add token expiration and refresh-token rotation.
- Connect Spring Security authentication filters to issued JWTs.
