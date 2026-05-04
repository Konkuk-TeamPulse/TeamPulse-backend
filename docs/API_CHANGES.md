# TeamPulse API 변경사항

이 문서는 Notion API 명세와 실제 백엔드 구현이 어긋나지 않도록 변경된 API, 요청/응답, 인증, DB 저장 방식을 정리하는 기록입니다.

## 먼저 읽을 요약

이번 변경의 핵심은 "MySQL로 실제 데이터를 저장하는 상태에서, 프론트가 정상 사용자 흐름을 끝까지 탈 수 있게 백엔드 API를 안정화한 것"입니다.

| 구분 | 기존 문제 | 변경 후 | 프론트 영향 | Notion/API 문서 반영 |
| --- | --- | --- | --- | --- |
| 회의 생성 | 프론트가 `decisions`를 배열로 보내면 500 발생 가능 | `string`과 `string[]` 모두 허용 | 현재 배열 payload 유지 가능 | `decisions` 타입 설명 수정 |
| 초대 링크 | 초대 조회/수락이 현재 사용자 워크스페이스 기준이라 400 발생 | `inviteCode` 기준으로 조회/수락 | 수락 후 프로젝트 목록 재조회 필요 | 공개 조회/로그인 수락 구분 |
| 태스크 의존관계 | `A -> B`, `B -> A` 순환 관계가 가능 | 순환 의존관계는 400, 코드 `3006` | 오류 메시지 표시만 하면 됨 | 오류 코드 표 반영 |
| 리포트 목록 | 생성된 리포트 목록을 다시 조회할 API 부족 | `GET /api/projects/{projectId}/reports` 추가 | 리포트 화면에서 목록 조회 가능 | 신규 API 추가 |
| 잘못된 요청 본문 | 잘못된 enum 값이 500으로 보일 수 있음 | 400, 코드 `2020`으로 처리 | 상태값은 `TODO/DOING/DONE`만 전송 | enum 값 명시 |
| DB 스키마 | 초대 멤버와 워크스페이스 소유자 구분이 약함 | `members.email`, `workspaces.owner_email` 사용 | 직접 영향 없음 | DB 설계 문서 반영 |
| 멀티 프로젝트 | API 응답과 조회가 `projectId=1` 중심으로 고정됨 | 실제 DB의 `assignment2_workspaces.id`를 `projectId`로 사용 | 생성/조회/태스크/회의/리포트 모두 실제 projectId 사용 필요 | MVP 제약 문구 수정 |
| DB migration | 운영 DB 스키마가 Hibernate `ddl-auto=update`에 의존 | MySQL profile은 Flyway migration + Hibernate validate로 전환 | 기존 API path 영향 없음 | DB 운영/배포 절차 반영 |
| API 문서 | Notion과 코드의 API 명세 동기화가 수동 중심 | OpenAPI JSON 제공 | API 호출 구조 확인 가능 | API 문서 위치 추가 |
| 인증 토큰 | DB에 토큰 원문 저장, refresh rotation 없음 | MySQL profile은 JWT access token + hashed refresh token + rotation 적용 | `POST /api/auth/refresh` 추가, 기존 Authorization 헤더 방식 유지 | 인증 API와 DB 저장 방식 수정 |
| 로그인 보안 | 없는 이메일/비밀번호 오류가 구분됨 | 둘 다 401/code `2014`로 통일, 실패 횟수 제한 추가 | 로그인 실패 메시지 하나로 표시 | 오류 코드/메시지 수정 |
| 팀 권한 | 멤버도 초대/멤버 삭제/프로젝트 수정 가능 | 프로젝트 LEADER만 팀 관리 API 호출 가능 | 403/code `3008` 처리 필요 | 권한 표 추가 |
| 초대 수락 role | 요청 body의 `role`이 반영될 수 있음 | 초대 수락자는 항상 `MEMBER`로 저장 | 프론트에서 role을 보내지 않아도 됨 | 초대 수락 명세 수정 |
| 운영 설정 | 로컬/운영 DB 설정 구분 약함 | `prod` profile은 DB/JWT secret 환경변수 필수, OpenAPI 비공개 기본값 | 배포 환경변수 정리 필요 | 배포 설정 문서 반영 |
| PDF 리포트 | 단순 텍스트 PDF라 한글이 `?`로 깨지고 내용이 부족함 | HTML/CSS 템플릿 기반 다중 섹션 PDF로 변경 | 다운로드 API path는 그대로, 파일 내용만 개선 | 리포트 산출물 설명 수정 |
| 운영 부팅 | `prod` 단독 실행 시 JPA 인증/워크스페이스 빈이 뜨지 않을 수 있음 | JPA 구현체를 `mysql`, `prod` profile 모두에서 사용 | 배포 profile을 `prod`로만 둬도 부팅 가능 | 배포 설정 문서 반영 |
| 프로젝트 접근 권한 | 인증 객체가 없거나 비멤버여도 내부 role 계산이 느슨함 | 인증 사용자만 role 계산, 비멤버는 403/code `3008` | 권한 없음 UI 처리 필요 | 오류 코드 표 반영 |
| 로그인 실패 기록 | 실패 이메일이 서버 메모리에 계속 누적될 수 있음 | 만료 cleanup과 최대 추적 이메일 수 제한 추가 | API path 영향 없음 | 운영 설정 문서 반영 |

## 팀원이 바로 확인할 일

### 프론트 담당

- 회의 생성 시 `decisions: string[]`는 그대로 보내도 됩니다.
- 태스크 상태값은 `TODO`, `DOING`, `DONE`만 보내야 합니다. `IN_PROGRESS`는 실패합니다.
- 초대 수락 성공 후 `GET /api/projects`를 다시 호출해 프로젝트 목록을 갱신해야 합니다.
- 리포트 히스토리가 필요하면 `GET /api/projects/{projectId}/reports`를 호출하면 됩니다.
- API 명세를 코드 기준으로 확인할 때는 `GET /v3/api-docs`를 보면 됩니다.
- 운영 배포에서는 `/v3/api-docs`가 비공개일 수 있습니다. 로컬/demo 기준으로만 직접 확인하면 됩니다.
- refresh token 갱신은 `POST /api/auth/refresh`를 사용합니다.
- 프로젝트 수정, 멤버 추가/삭제, 초대 링크 생성은 LEADER만 가능하므로 `403`, code `3008` 처리가 필요합니다.
- MySQL migration 도입으로 기존 프론트 API path는 바뀌지 않았습니다.
- PDF 다운로드 API path는 바뀌지 않았고, 백엔드가 생성하는 PDF 내용만 보강되었습니다.

### API/Notion 문서 담당

- 회의 생성 API의 `decisions` 타입을 `string | string[]` 또는 `string[] 권장`으로 수정합니다.
- 초대 API를 `GET` 공개 조회, `POST accept` 로그인 필요로 분리해서 적습니다.
- 리포트 목록 조회 API `GET /api/projects/{projectId}/reports`를 추가합니다.
- 태스크 상태 enum을 `TODO`, `DOING`, `DONE`으로 명시합니다.
- 오류 코드 `3006`에 "자기 자신/순환 태스크 의존관계 불가"를 포함합니다.
- 운영 DB 문서에는 MySQL profile 기준 `src/main/resources/db/migration/mysql`의 Flyway SQL이 실제 스키마 기준이라고 적습니다.
- API 문서 위치에는 `GET /v3/api-docs`를 추가합니다.
- 인증 API에는 `POST /api/auth/refresh`를 추가합니다.
- 로그인 실패 응답은 없는 이메일/비밀번호 오류 모두 401/code `2014`로 통일합니다.
- 팀 관리 API는 LEADER 권한 필요, 실패 시 403/code `3008`로 정리합니다.
- 운영 profile에서는 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `APP_JWT_SECRET`이 필수입니다.
- 비멤버가 프로젝트 API에 접근하면 403/code `3008` 권한 오류로 처리됩니다.
- 리포트 PDF 설명은 "단순 현황표"가 아니라 태스크, 팀원별 작업, 회의, 리스크, 다음 액션을 포함한 프로젝트 진행 상황 리포트로 수정합니다.

### DB/백엔드 담당

- MySQL 프로필 기준 주요 테이블은 `assignment2_*`, `users`, `auth_sessions`입니다.
- 이번 변경으로 `assignment2_members.email` 컬럼이 추가되었습니다.
- 이전 변경에서 `assignment2_workspaces.owner_email` 컬럼이 추가되었습니다.
- 2026-05-04 추가 변경으로 `projectId=1` 고정 응답을 제거하고, `assignment2_workspaces.id`를 실제 프로젝트 ID로 사용합니다.
- MySQL profile은 Flyway migration으로 스키마를 만들고, Hibernate는 `validate`만 수행합니다.
- 기존 DB에 Flyway 이력이 없으면 `baseline-on-migrate=true`로 현재 스키마를 기준점으로 잡은 뒤 보강 migration을 적용합니다.
- 2026-05-04 제품화 보완으로 MySQL profile의 `auth_sessions.access_token`, `auth_sessions.refresh_token`에는 토큰 원문이 아니라 SHA-256 hash가 저장됩니다.

### 7. 제품화 1차 보안 보완

- 변경 파일
  - `src/main/java/com/teampulse/backend/auth/application/AuthService.java`
  - `src/main/java/com/teampulse/backend/auth/application/LoginAttemptGuard.java`
  - `src/main/java/com/teampulse/backend/auth/infrastructure/JpaTokenIssuer.java`
  - `src/main/java/com/teampulse/backend/auth/infrastructure/DemoTokenIssuer.java`
  - `src/main/java/com/teampulse/backend/auth/api/AuthApiController.java`
  - `src/main/java/com/teampulse/backend/auth/api/AuthExceptionHandler.java`
  - `src/main/java/com/teampulse/backend/mobile/api/ProjectApiController.java`
  - `src/main/java/com/teampulse/backend/mobile/api/InvitationApiController.java`
  - `src/main/java/com/teampulse/backend/mobile/api/MobileSpecExceptionHandler.java`
  - `src/main/resources/application-prod.properties`
- 신규 API
  - `POST /api/auth/refresh`
- 변경 내용
  - MySQL profile에서 access token은 HMAC-SHA256 서명 JWT로 발급합니다.
  - refresh token은 랜덤 토큰으로 발급하고, DB에는 SHA-256 hash만 저장합니다.
  - `POST /api/auth/refresh` 호출 시 기존 refresh token은 폐기되고 새 access/refresh token 쌍이 발급됩니다.
  - 없는 이메일과 틀린 비밀번호는 모두 같은 401/code `2014` 응답을 반환합니다.
  - 로그인 실패가 누적되면 429/code `2015`로 잠시 차단합니다.
  - 초대 수락 요청에 `role: "LEADER"`가 들어와도 서버는 항상 `MEMBER`로 저장합니다.
  - 프로젝트 수정, 멤버 추가/삭제, 초대 링크 생성은 LEADER만 가능하며 실패 시 403/code `3008`을 반환합니다.
  - 인증 사용자 정보가 없으면 role 계산을 하지 않고 401/code `3001`을 반환합니다.
  - 프로젝트 멤버가 아닌 사용자는 403/code `3008`을 반환합니다.
  - 로그인 실패 추적 기록은 만료 cleanup과 `APP_LOGIN_MAX_TRACKED_EMAILS` 상한으로 관리합니다.
  - `prod` profile은 운영 DB/JWT secret을 환경변수로 받으며, OpenAPI 공개를 기본 비활성화합니다.
- 프론트 전달사항
  - 기존 보호 API의 `Authorization: Bearer ...` 헤더 방식은 유지됩니다.
  - access token 만료 대응은 `POST /api/auth/refresh`로 처리하면 됩니다.
  - refresh token은 재사용하면 실패하므로, refresh 성공 시 새 refresh token으로 교체 저장해야 합니다.
  - 팀 관리 버튼은 `GET /api/projects` 또는 프로젝트 상세 응답의 role이 `LEADER`일 때만 노출하는 것이 안전합니다.
  - 멤버가 팀 관리 API를 호출하면 `403`, code `3008`이 내려옵니다.

```json
{
  "refreshToken": "Bearer refresh-token-value"
}
```

```json
{
  "isSuccess": true,
  "responseCode": 1000,
  "responseMessage": "요청에 성공했습니다.",
  "result": {
    "accessToken": "Bearer eyJ...",
    "refreshToken": "Bearer refresh-..."
  }
}
```

### 8. PDF 리포트 서식 보강

- 변경 파일
  - `pom.xml`
  - `src/main/java/com/teampulse/backend/mobile/api/ProjectApiController.java`
  - `src/main/java/com/teampulse/backend/mobile/report/ReportPdfService.java`
- 영향 API
  - `GET /api/projects/{projectId}/reports/{reportId}/download`
  - `GET /api/reports/{reportId}/download`
- 변경 내용
  - 기존 수동 PDF 생성 코드는 한글을 ASCII로 치환하면서 `?`가 표시될 수 있었습니다.
  - 이제 백엔드가 프로젝트 데이터를 PDF 전용 View Model로 집계한 뒤 HTML/CSS 템플릿을 PDF로 렌더링합니다.
  - PDF에는 표지/핵심 요약, 프로젝트 개요, 전체 진행 요약, 태스크 진행 현황, 팀원별 작업 현황, 지연·병목 확인 항목, 회의·의사결정, 리스크, 다음 액션, 활동 로그가 포함됩니다.
  - 실제 데이터에 없는 회의, 리스크, 활동 로그는 생성하지 않고 섹션별 empty state 문구로 표시합니다.
  - 표현은 "위험 확정", "문제 발생" 대신 "확인 필요", "진행 상태 확인", "추가 점검" 중심으로 정리했습니다.
  - 한글 폰트는 `app.report.pdf.font-path` 설정값을 우선 사용하고, 없으면 OS 기본 한글 폰트를 자동 탐색합니다.
- 프론트 전달사항
  - API path, 요청 방식, 파일 다운로드 처리는 기존과 동일합니다.
  - 프론트는 PDF 미리보기/다운로드 UI만 유지하면 되고, 내용 구성은 백엔드가 담당합니다.
- Notion/API 문서 반영
  - PDF 리포트 산출물 설명을 "단일 페이지 요약"에서 "프로젝트 진행 상황 리포트"로 수정합니다.
  - 리포트 데이터 출처는 프로젝트, 태스크, 팀원, 회의, 리스크, 활동 로그라고 명시합니다.
  - 태스크가 없을 때는 리포트 생성이 400/code `3007`로 실패할 수 있음을 유지해서 적습니다.

### 6. 실제 projectId 기반 멀티 프로젝트 지원

- 변경 파일
  - `src/main/java/com/teampulse/backend/mobile/application/ProjectWorkspaceUseCase.java`
  - `src/main/java/com/teampulse/backend/mobile/application/service/JpaWorkspaceService.java`
  - `src/main/java/com/teampulse/backend/mobile/api/ProjectApiController.java`
  - `src/main/java/com/teampulse/backend/mobile/api/ProjectTaskApiController.java`
  - `src/main/java/com/teampulse/backend/mobile/api/ProjectMeetingApiController.java`
  - `src/main/java/com/teampulse/backend/mobile/api/TaskApiController.java`
  - `src/main/java/com/teampulse/backend/mobile/api/MeetingApiController.java`
  - `src/main/java/com/teampulse/backend/mobile/api/InvitationApiController.java`
  - `src/main/java/com/teampulse/backend/mobile/dto/WorkspaceState.java`
  - `src/main/java/com/teampulse/backend/mobile/dto/MemberView.java`
  - `src/main/java/com/teampulse/backend/mobile/persistence/MobileWorkspaceRepository.java`
- 영향 API
  - `POST /api/projects`
  - `GET /api/projects`
  - `GET/PATCH /api/projects/{projectId}`
  - `GET /api/projects/{projectId}/dashboard`
  - `GET/POST/PATCH/DELETE /api/projects/{projectId}/tasks/**`
  - `PATCH/DELETE /api/tasks/{taskId}`
  - `PATCH /api/tasks/{taskId}/status`
  - `POST/DELETE /api/tasks/{taskId}/dependencies/**`
  - `GET/POST /api/projects/{projectId}/meetings/**`
  - `GET /api/meetings/{meetingId}`
  - `GET/POST /api/projects/{projectId}/reports/**`
  - `GET /api/reports/{reportId}/download`
  - `GET /api/invitations/{inviteCode}`
  - `POST /api/invitations/{inviteCode}/accept`
- 변경 내용
  - `POST /api/projects`는 더 이상 항상 `projectId: 1`을 반환하지 않고, MySQL에 저장된 실제 워크스페이스 ID를 반환합니다.
  - `GET /api/projects`는 로그인 사용자가 소유자이거나 멤버로 속한 모든 프로젝트를 반환합니다.
  - `/api/projects/{projectId}/...` 하위 API는 전달받은 projectId에 해당하는 프로젝트 안에서만 태스크/회의/리포트/멤버를 조회·수정합니다.
  - `/api/tasks/{taskId}`처럼 projectId가 없는 API는 taskId가 속한 프로젝트를 먼저 찾고, 현재 로그인 사용자가 접근 가능한 프로젝트인지 확인합니다.
  - 초대 조회/수락 응답의 `projectId`, 회의 상세 응답의 `projectId`, 리포트 다운로드도 실제 소속 프로젝트 ID를 반환합니다.
  - 초대 수락 시 같은 이름의 다른 사용자를 같은 멤버로 오인하지 않도록 이메일을 우선 기준으로 사용합니다.
  - `DELETE /api/projects/{projectId}/members/me`는 프로젝트 리더 이름이 아니라 현재 로그인 사용자의 이메일/이름 기준으로 본인을 찾습니다.
- 프론트 전달사항
  - 프로젝트 생성 후 응답의 `result.projectId`를 저장해서 이후 URL에 사용해야 합니다.
  - 더 이상 프론트에서 `1`을 기본 projectId로 가정하면 안 됩니다.
  - 초대 수락 후 `GET /api/projects`를 다시 호출하면 초대받은 프로젝트가 `MEMBER` 역할로 나타납니다.
  - 권한 없는 프로젝트 접근은 현재 Spec 오류 응답으로 400을 반환합니다. 프론트에서는 접근 불가/프로젝트 없음 화면으로 처리하면 됩니다.

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
  - `GET /api/roadmap`은 demo/local에서만 공개 권장
  - `POST /api/auth/signup`
  - `POST /api/auth/login`
  - `POST /api/auth/refresh`
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
7. `projectId`는 더 이상 `1` 고정값이 아니라 `assignment2_workspaces.id` 기반 실제 DB ID라고 명시합니다.
8. projectId 없는 태스크/회의/리포트 상세 API는 해당 리소스가 속한 프로젝트를 서버가 찾아 권한을 검사한다고 적습니다.

## 2026-05-04 실제 검증 결과

- 실행 프로필: `mysql`
- DB: MySQL 8.4.8, `teampulse` database
- 검증 흐름
  - 회원가입 2명
  - 동일한 리더 계정으로 프로젝트 2개 생성
  - 프로젝트 A/B에 각각 태스크 생성
  - 프로젝트 A/B 태스크 목록 격리 확인
  - projectId 없는 `PATCH /api/tasks/{taskId}/status`로 프로젝트 B 태스크 상태 변경
  - 잘못된 상태값 400 처리 확인
  - 태스크 의존관계 생성
  - 순환 의존관계 400 처리 확인
  - 프로젝트 B 회의 생성 및 `GET /api/meetings/{meetingId}`의 projectId 확인
  - 초대 링크 생성/공개 조회/수락
  - 초대받은 사용자의 프로젝트 목록 조회 및 unrelated 프로젝트 접근 차단 확인
  - 프로젝트 B 리포트 생성/목록 조회/PDF 다운로드
- 최종 확인 데이터
  - 리더 이메일: `multi-leader-1777865007084@example.com`
  - 초대 멤버 이메일: `multi-member-1777865007084@example.com`
  - 프로젝트 A ID: `22`
  - 프로젝트 B ID: `23`
  - 프로젝트 A 태스크 ID: `27`
  - 프로젝트 B 태스크 ID: `28`
  - 프로젝트 B 회의 ID: `6`
  - 초대받은 사용자 역할: `MEMBER`
  - 권한 없는 프로젝트 접근 응답: `400`
  - 순환 의존관계 오류 코드: `3006`
  - 잘못된 상태값 오류 코드: `2020`
  - 프로젝트 B 리포트 ID: `10`

## 남은 MVP 제약

- MySQL 프로필에서는 실제 projectId 기반으로 여러 프로젝트가 동작합니다. `demo` 프로필의 인메모리 구현은 기존 테스트 호환을 위해 단일 프로젝트 중심으로 유지합니다.
- 리포트 PDF는 HTML/CSS 템플릿 기반으로 보강되었지만, 대형 프로젝트에서 표가 매우 길어지는 경우 부록 페이지 분리나 요약/상세 분리 정책은 추가 고도화가 필요합니다.
