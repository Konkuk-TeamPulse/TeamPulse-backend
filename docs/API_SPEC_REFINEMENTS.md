# API Spec Refinements

Date: 2026-04-29
Branch: `feature-api-spec-refine`

## 반영한 수정사항

### FR-001 회원가입 이메일 기준

회원가입/로그인 요청은 학번이 아니라 이메일을 기준으로 받는다.

Relevant endpoint:

```text
POST /api/auth/signup
POST /api/auth/login
```

Current MVP behavior:

- `POST /api/auth/signup`은 별도 auth 모듈의 Controller/Service/Repository/DTO 구조로 분리했다.
- `email`, `password`, `name`, `university`, `phone` 필드를 받는다.
- 이메일은 소문자 정규화 후 중복 검증한다.
- 비밀번호는 BCrypt로 해시 처리해 저장한다.
- 응답은 `isSuccess`, `responseCode`, `responseMessage`, `result` 명세형 wrapper를 사용한다.
- 응답에는 demo access/refresh token과 사용자 이메일을 포함한다.
- 실제 JWT 발급과 영속 User 테이블 저장은 후속 고도화 대상이다.

Success response:

```json
{
  "isSuccess": true,
  "responseCode": 1000,
  "responseMessage": "요청에 성공하였습니다.",
  "result": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "university": "건국대학교",
    "phone": "010-1234-1234",
    "jwtInfo": {
      "accessToken": "Bearer demo-access-...",
      "refreshToken": "Bearer demo-refresh-..."
    }
  }
}
```

Failure response:

```json
{
  "isSuccess": false,
  "responseCode": 2012,
  "responseMessage": "중복된 이메일입니다.",
  "result": null
}
```

Validation response:

```json
{
  "isSuccess": false,
  "responseCode": 2020,
  "responseMessage": "요청 값이 잘못되었습니다.",
  "result": {
    "errors": []
  }
}
```

### FR-003 계정 수정

계정 조회만 있던 상태에서 계정 수정 API를 추가했다.

```text
GET   /api/account
PATCH /api/account
```

Request:

```json
{
  "name": "Lee Juho",
  "email": "juho@example.com",
  "university": "Konkuk University",
  "phone": "010-1111-2222"
}
```

MVP behavior:

- demo/in-memory profile 상태가 실제로 갱신된다.
- mysql profile에서는 `assignment2_workspaces`에 사용자 학교/전화번호 컬럼을 추가해 저장한다.

### FR-018 활동 로그 마지막 수정 시각

활동 로그 응답에 `updatedAt` 필드를 추가했다.

```text
GET /api/projects/{projectId}/activities
```

Current response item shape:

```json
{
  "id": 1001,
  "actor": "Lee Juho",
  "at": "2026-04-29 12:00",
  "updatedAt": "2026-04-29 12:00",
  "summary": "Account profile updated."
}
```

현재 MVP에서는 활동 생성 시각과 마지막 수정 시각이 동일하다.

### FR-019 사용자 활동 기록

프로젝트별 활동 로그와 별도로 현재 사용자 기준 활동 로그 API를 추가했다.

```text
GET /api/account/activities
```

MVP behavior:

- 현재 workspace user name과 actor가 같은 활동만 반환한다.
- 실제 JWT/userId 기반 필터링은 후속 인증 고도화에서 처리한다.

### FR-035/FR-036 리스크 대응 옵션 목록

리스크별 대응 옵션 목록 API를 추가했다.

```text
GET /api/projects/{projectId}/risks/{riskId}/actions
```

Response example:

```json
[
  {
    "type": "RESCHEDULE",
    "label": "일정 재조정",
    "description": "지연 또는 임박한 태스크의 마감일을 뒤로 조정합니다.",
    "targetTaskId": 1002,
    "suggestedOwner": null,
    "suggestedDueDate": "2026-04-11"
  },
  {
    "type": "REASSIGN",
    "label": "작업 재할당",
    "description": "마감 위험이 있는 태스크를 작업량이 적은 팀원에게 재배정합니다.",
    "targetTaskId": 1002,
    "suggestedOwner": "Min",
    "suggestedDueDate": null
  }
]
```

MVP behavior:

- 현재 감지된 risk id 기준으로 대응 옵션을 반환한다.
- 일정 재조정, 작업 재할당, 선행 작업 정리, 작업 분할, 회의 등록 옵션을 제공한다.
- 대응 옵션 실행 API는 아직 추가하지 않았다.

## 검증

```powershell
.\mvnw.cmd test '-Dspring.profiles.active=demo'
```

Result:

```text
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Signup focused test:

```powershell
.\mvnw.cmd '-Dtest=AuthApiControllerTest' test '-Dspring.profiles.active=demo'
```

Result:

```text
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Live smoke test:

```text
server: http://127.0.0.1:18080
profile: demo
```

Verified endpoints:

```text
POST  /api/auth/signup
PATCH /api/account
GET   /api/projects/1/activities
GET   /api/account/activities
GET   /api/projects/1/risks
GET   /api/projects/1/risks/101/actions
```

Observed result:

```json
{
  "signupEmail": "juho@example.com",
  "signupHasJwtInfo": true,
  "accountEmail": "juho@example.com",
  "accountUniversity": "Konkuk University",
  "accountPhone": "010-1111-2222",
  "projectActivityHasUpdatedAt": true,
  "userActivityCount": 1,
  "userActivityActor": "Lee Juho",
  "riskCount": 2,
  "riskActionTypes": "RESCHEDULE,REASSIGN"
}
```

Signup live smoke test:

```json
{
  "successIsSuccess": true,
  "successCode": 1000,
  "successEmail": "signup-live-1777445766@example.com",
  "successHasToken": true,
  "duplicateStatus": 409,
  "duplicateCode": 2012,
  "invalidStatus": 400,
  "invalidCode": 2020,
  "invalidErrors": 4
}
```

Login implementation status:

- `POST /api/auth/login`도 회원가입과 같은 auth 모듈의 Controller/Service/Repository/DTO 구조로 분리했다.
- 이메일은 소문자 정규화 후 조회한다.
- 비밀번호는 BCrypt hash와 비교한다.
- 성공 응답은 `isSuccess`, `responseCode`, `responseMessage`, `result` 명세형 wrapper를 사용한다.
- 존재하지 않는 회원은 `404 + responseCode 2013`으로 응답한다.
- 비밀번호 불일치는 `401 + responseCode 2014`로 응답한다.
- 검증 오류는 `400 + responseCode 2020`으로 응답한다.

Login live smoke test:

```json
{
  "signupCode": 1000,
  "loginIsSuccess": true,
  "loginCode": 1000,
  "loginEmail": "login-live-1777446292@example.com",
  "loginHasToken": true,
  "wrongStatus": 401,
  "wrongCode": 2014,
  "missingStatus": 404,
  "missingCode": 2013
}
```

## 남은 고도화

- 실제 JWT 인증/인가
- userId 기반 사용자 활동 필터링
- 리스크 대응 옵션 실행 API
- response wrapper를 노션 명세의 `isSuccess`, `responseCode`, `responseMessage`, `result` 구조로 정리

## 2026-04-30 코드 리뷰 후 보완

- 명세형 모바일 API 컨트롤러의 검증 실패와 도메인 오류도 `SpecResponse` 실패 wrapper로 반환하도록 정리했다.
- 인증 실패는 `responseCode: 3001`, 태스크 자기참조 의존관계는 `responseCode: 3006`으로 반환한다.
- `GET /api/users/me`는 demo access token이 있으면 토큰에 연결된 사용자 정보를 반환한다.
- 회의록 생성 시 `content`, `attendeeIds`, `actionItems.assigneeId`, `actionItems.dueDate`가 MeetingView/JPA 엔티티에 보존되도록 보완했다.
