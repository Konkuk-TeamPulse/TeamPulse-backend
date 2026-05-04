# TeamPulse API 변경사항

이 문서는 Notion API 명세와 실제 백엔드 구현이 어긋나지 않도록 변경된 API, 요청/응답, 인증, DB 저장 방식을 정리하는 기록입니다.

## 2026-05-04 백엔드 검증 및 DB 연동 보완

### 1. 회의 생성 API: `decisions` 배열 요청 허용

- 변경 파일
  - `src/main/java/com/teampulse/backend/mobile/dto/MeetingCreateSpecRequest.java`
  - `src/main/java/com/teampulse/backend/mobile/dto/FlexibleStringListDeserializer.java`
  - `src/main/java/com/teampulse/backend/mobile/api/ProjectMeetingApiController.java`
- 영향 API
  - `POST /api/projects/{projectId}/meetings`
- 변경 내용
  - 기존 문자열 형태의 `decisions`도 계속 허용합니다.
  - 프론트에서 보내는 배열 형태의 `decisions: string[]`도 허용합니다.
  - 빈 문자열은 저장 대상에서 제외합니다.
- 프론트 전달사항
  - 현재 프론트가 `decisions`를 배열로 보내도 백엔드에서 정상 처리됩니다.
  - 별도 프론트 수정은 필요하지 않습니다.

```json
{
  "title": "주간 회의",
  "meetingDate": "2026-05-08",
  "agenda": "DB 연동 점검",
  "content": "회의 내용",
  "decisions": ["MySQL 프로필 유지", "API 문서 반영"],
  "attendeeIds": [1],
  "actionItems": [
    {
      "content": "API 변경사항 문서화",
      "assigneeId": 1,
      "dueDate": "2026-05-10"
    }
  ]
}
```

### 2. 초대 링크 조회/수락 로직 보완

- 변경 파일
  - `src/main/java/com/teampulse/backend/mobile/application/MobileInvitationUseCase.java`
  - `src/main/java/com/teampulse/backend/mobile/api/InvitationApiController.java`
  - `src/main/java/com/teampulse/backend/mobile/application/service/JpaWorkspaceService.java`
  - `src/main/java/com/teampulse/backend/mobile/application/service/InMemoryWorkspaceService.java`
  - `src/main/java/com/teampulse/backend/mobile/persistence/MobileMemberEntity.java`
  - `src/main/java/com/teampulse/backend/mobile/persistence/MobileWorkspaceRepository.java`
- 영향 API
  - `GET /api/invitations/{inviteCode}`
  - `POST /api/invitations/{inviteCode}/accept`
  - `GET /api/projects`
- 변경 내용
  - 초대 링크 조회는 현재 로그인 사용자의 워크스페이스가 아니라 `inviteCode` 기준으로 공개 조회합니다.
  - 초대 수락은 로그인한 사용자의 이메일을 팀 멤버 이메일로 저장합니다.
  - 초대 수락 후 해당 사용자가 `GET /api/projects`를 호출하면 초대받은 프로젝트가 `MEMBER` 권한으로 조회됩니다.
- DB 스키마 변경
  - `assignment2_members.email` 컬럼 추가
- 프론트 전달사항
  - `GET /api/invitations/{inviteCode}`는 비로그인 상태에서도 호출할 수 있습니다.
  - `POST /api/invitations/{inviteCode}/accept`는 로그인 토큰이 필요합니다.
  - 초대 수락 후 프로젝트 목록을 다시 조회해야 합니다.

### 3. 태스크 의존관계 순환 방지

- 변경 파일
  - `src/main/java/com/teampulse/backend/mobile/application/service/JpaWorkspaceService.java`
  - `src/main/java/com/teampulse/backend/mobile/application/service/InMemoryWorkspaceService.java`
  - `src/main/java/com/teampulse/backend/mobile/api/MobileSpecExceptionHandler.java`
- 영향 API
  - `POST /api/tasks/{taskId}/dependencies`
- 변경 내용
  - 자기 자신을 선행 태스크로 지정하는 경우를 계속 차단합니다.
  - 이미 `A -> B` 관계가 있을 때 `B -> A`를 추가하는 순환 의존관계도 차단합니다.
- 오류 응답

```json
{
  "isSuccess": false,
  "responseCode": 3006,
  "responseMessage": "순환되는 태스크 의존관계는 설정할 수 없습니다.",
  "result": null
}
```

### 4. 리포트 목록 조회 API 추가

- 변경 파일
  - `src/main/java/com/teampulse/backend/mobile/api/ProjectApiController.java`
- 신규 API
  - `GET /api/projects/{projectId}/reports`
- 변경 내용
  - 생성된 리포트 목록을 프로젝트 화면에서 다시 조회할 수 있습니다.
  - 기존 리포트 생성 및 다운로드 API는 유지됩니다.
- 관련 API
  - `POST /api/projects/{projectId}/reports`
  - `GET /api/reports/{reportId}/download`
  - `GET /api/projects/{projectId}/reports/{reportId}/download`

```json
{
  "isSuccess": true,
  "responseCode": 1000,
  "responseMessage": "요청에 성공했습니다.",
  "result": [
    {
      "id": 8,
      "label": "TeamPulse report draft",
      "range": "2026-05-01 ~ 2026-05-03",
      "status": "READY"
    }
  ]
}
```

### 5. 잘못된 JSON 본문 오류 처리

- 변경 파일
  - `src/main/java/com/teampulse/backend/mobile/api/MobileSpecExceptionHandler.java`
- 영향 API
  - Spec 응답을 사용하는 프로젝트/태스크/회의/초대/리포트 API
- 변경 내용
  - enum 값 오류 등 JSON 본문을 파싱할 수 없는 요청이 500으로 떨어지지 않도록 400 응답으로 처리합니다.
- 예시
  - 태스크 상태값은 `TODO`, `DOING`, `DONE`만 허용합니다.
  - `IN_PROGRESS`를 보내면 아래처럼 실패합니다.

```json
{
  "isSuccess": false,
  "responseCode": 2020,
  "responseMessage": "요청 본문이 올바르지 않습니다.",
  "result": null
}
```

## 2026-05-03 DB/Auth/MySQL 전환

### 1. MySQL 프로필에서 인증/세션 저장

- 영향 API
  - `POST /api/auth/signup`
  - `POST /api/auth/login`
  - `POST /api/auth/logout`
- 변경 내용
  - `demo` 프로필은 기존처럼 메모리 저장소를 사용합니다.
  - `mysql` 프로필은 `users`, `auth_sessions` 테이블에 회원과 토큰 세션을 저장합니다.
  - 서버 재시작 후에도 기존 회원으로 다시 로그인할 수 있습니다.

### 2. MySQL 프로필에서 사용자별 워크스페이스 분리

- 영향 API
  - `POST /api/projects`
  - `GET /api/projects`
  - `GET /api/projects/{projectId}`
  - `GET/POST/PATCH/DELETE /api/projects/{projectId}/tasks`
  - `GET/POST/PATCH/DELETE /api/tasks/**`
  - `GET/POST /api/projects/{projectId}/meetings`
  - `POST /api/projects/{projectId}/reports`
- 변경 내용
  - 로그인한 사용자 이메일 기준으로 워크스페이스를 조회/생성합니다.
  - 서로 다른 사용자 계정의 프로젝트/태스크 데이터가 같은 워크스페이스에 섞이지 않습니다.
- DB 스키마 변경
  - `assignment2_workspaces.owner_email` 컬럼 추가

### 3. 프로젝트/태스크/회의/리포트 API 인증 필수화

- 인증 필요 API
  - `GET /api/users/me`
  - `GET/PATCH /api/account`
  - `GET/POST/PATCH/DELETE /api/projects/**`
  - `GET/POST/PATCH/DELETE /api/tasks/**`
  - `GET/POST /api/meetings/**`
  - `GET /api/reports/**`
  - `POST /api/invitations/{inviteCode}/accept`
- 공개 API
  - `GET /api/health`
  - `GET /api/roadmap`
  - `POST /api/auth/signup`
  - `POST /api/auth/login`
  - `GET /api/invitations/{inviteCode}`
  - `GET/POST/PATCH/DELETE /api/mobile/**` legacy demo API
- 프론트 전달사항
  - 보호 API 호출에는 `Authorization: Bearer ...` 헤더가 필요합니다.

## 현재 DB 테이블

MySQL 프로필 기준 주요 테이블은 아래와 같습니다.

- `users`: 회원 정보
- `auth_sessions`: access token, refresh token 세션
- `assignment2_workspaces`: 프로젝트/워크스페이스
- `assignment2_members`: 팀 멤버, 이메일, 역할
- `assignment2_tasks`: 태스크
- `assignment2_meetings`: 회의록
- `assignment2_reports`: 리포트 메타데이터
- `assignment2_activities`: 활동 로그

## Notion/API 문서에 반영할 내용

1. 회의 생성 API의 `decisions` 타입을 `string | string[]` 또는 `string[] 권장`으로 정리합니다.
2. 리포트 목록 조회 API `GET /api/projects/{projectId}/reports`를 추가합니다.
3. 태스크 상태 enum은 `TODO`, `DOING`, `DONE`으로 고정합니다.
4. 초대 링크는 `GET` 공개 조회, `POST accept` 로그인 필요로 구분합니다.
5. MySQL 저장 테이블에 `assignment2_members.email`과 `assignment2_workspaces.owner_email`이 추가된 점을 DB 설계 문서에 반영합니다.
6. 태스크 의존관계 순환 오류 코드 `3006`을 오류 코드 표에 추가하거나 기존 태스크 의존성 오류에 포함합니다.

## 2026-05-04 실제 검증 결과

- 실행 프로필: `mysql`
- DB: MySQL 8.4.8, `teampulse` database
- 검증 흐름
  - 회원가입 2명
  - 프로젝트 생성
  - 태스크 2개 생성
  - 태스크 상태 변경
  - 잘못된 상태값 400 처리 확인
  - 태스크 의존관계 생성
  - 순환 의존관계 400 처리 확인
  - 회의 생성
  - 초대 링크 생성/공개 조회/수락
  - 초대받은 사용자의 프로젝트 목록 조회
  - 리포트 생성/목록 조회/PDF 다운로드
- 최종 확인 데이터
  - 프로젝트: `Smoke Project 20260504020506`
  - 초대 코드: `JJ3JP7`
  - 초대받은 사용자 역할: `MEMBER`
  - 순환 의존관계 오류 코드: `3006`
  - 잘못된 상태값 오류 코드: `2020`
  - 리포트 ID: `8`
  - PDF 다운로드 크기: `2533 bytes`

## 남은 MVP 제약

- 프로젝트 ID는 아직 MVP 호환을 위해 `1` 중심으로 동작합니다.
- 한 사용자가 여러 프로젝트에 속하는 완전한 멀티 프로젝트 구조는 아직 아닙니다.
- 리포트 PDF는 백엔드에서 생성한 단일 페이지 요약 PDF입니다. 실제 서비스 수준에서는 PDF 템플릿/한글 폰트/다중 페이지 처리가 추가로 필요합니다.
