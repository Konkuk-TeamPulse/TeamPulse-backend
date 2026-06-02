# API Spec Refinements

Last updated: 2026-05-28
Scope: Notion/API 명세를 현재 코드 기준으로 맞출 때 참고할 정리본

## Current spec policy

- 공식 API path는 `/api/projects`, `/api/tasks`, `/api/meetings`, `/api/invitations`, `/api/users/me`, `/api/auth` 계열이다.
- `/api/mobile/**`와 `/api/account/**`는 더 이상 공식 명세에 넣지 않는다.
- 프론트/제출용 Postman 기준에서도 현재 코드에 존재하지 않는 legacy endpoint는 제외한다.

## Response wrappers

두 wrapper가 공존한다.

### SpecResponse

```json
{
  "isSuccess": true,
  "responseCode": 1000,
  "responseMessage": "요청에 성공했습니다.",
  "result": {}
}
```

### ApiResponse

```json
{
  "success": true,
  "data": {},
  "meta": null,
  "error": null
}
```

프론트와 Postman 테스트 문서에는 각 엔드포인트의 실제 응답 wrapper를 기준으로 적는다.

## Key refinements kept

- 회원가입/로그인은 `studentId`가 아니라 `email`을 사용자 식별자로 사용한다.
- 프로젝트 ID는 더 이상 `1` 고정이 아니며, MySQL에서는 `assignment2_workspaces.id` 값을 쓴다.
- 태스크 상태 enum은 `TODO`, `DOING`, `DONE` 기준이다.
- 초대 생성은 `/api/projects/{projectId}/invitations`, 초대 조회/수락은 `/api/invitations/{inviteCode}` 기준이다.
- 회의록 상세 조회는 현재 코드 기준 `/api/meetings/{meetingId}`를 사용한다.
- 리포트 다운로드는 현재 코드 기준 `/api/reports/{reportId}/download`를 사용한다.

## Current exclusions

아래는 현재 코드/제출 기준에서 공식 명세에 넣지 않는다.

- `GET/PATCH /api/account`
- `GET /api/account/activities`
- `GET/POST/PATCH/DELETE /api/mobile/**`
- `GET /api/roadmap`
- `POST /api/projects/{projectId}/invite-links`
- `GET /api/projects/{projectId}/risks/{riskId}/actions`

## Remaining cleanup note

`src/main/java/com/teampulse/backend/mobile` 패키지명은 과거 MVP/mobile naming의 흔적이다. 단, 현재 이 패키지는 실제 프로젝트 API 구현을 담고 있으므로 삭제하지 않는다. 이름 변경은 import, 테스트, 문서 영향이 커서 별도 리팩터링 작업으로 분리한다.
