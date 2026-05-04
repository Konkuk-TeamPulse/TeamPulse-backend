# Frontend Handoff: Backend Productization Changes

이 문서는 프론트 담당자가 백엔드 최신 변경사항을 빠르게 연결하고 QA할 수 있도록 정리한 전달 문서입니다.

## 요약

- 기존 보호 API의 `Authorization: Bearer ...` 헤더 방식은 유지됩니다.
- refresh token 갱신 API가 새로 추가되었습니다.
- 프로젝트 팀 관리 API는 `LEADER`만 호출할 수 있습니다.
- 초대 수락자는 서버에서 항상 `MEMBER`로 저장됩니다.
- 운영 환경에서는 OpenAPI 문서가 비공개일 수 있습니다.
- 운영 환경에서는 legacy `/api/mobile/**` demo API가 기본 비공개일 수 있습니다. 프론트는 공식 `/api/projects/**`, `/api/tasks/**`, `/api/invitations/**`, `/api/reports/**` 계열을 기준으로 붙이면 됩니다.
- 로그인 실패 제한은 동작하되, 서버가 오래 떠 있어도 실패 기록이 무한히 쌓이지 않도록 정리되었습니다.

## 신규 API

### POST `/api/auth/refresh`

access token 만료 시 refresh token으로 새 토큰 쌍을 발급합니다.

요청:

```json
{
  "refreshToken": "Bearer refresh-token-value"
}
```

응답:

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

프론트 처리:

- refresh 성공 시 `accessToken`, `refreshToken` 둘 다 새 값으로 교체 저장해야 합니다.
- 기존 refresh token은 재사용하면 실패합니다.
- refresh 실패 시 로그인 화면으로 보내는 처리가 필요합니다.

## 인증/로그인 변경

### 로그인 실패 응답 통일

없는 이메일과 틀린 비밀번호는 모두 동일하게 응답합니다.

```json
{
  "isSuccess": false,
  "responseCode": 2014,
  "responseMessage": "이메일 또는 비밀번호가 올바르지 않습니다.",
  "result": null
}
```

### 반복 실패 제한

로그인 실패가 누적되면 아래 응답이 내려올 수 있습니다.

```json
{
  "isSuccess": false,
  "responseCode": 2015,
  "responseMessage": "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.",
  "result": null
}
```

프론트 처리:

- 로그인 실패 메시지는 하나로 통일해서 보여주면 됩니다.
- `2015`는 잠시 후 다시 시도하라는 안내를 보여주면 됩니다.

## 권한 변경

아래 API는 프로젝트 `LEADER`만 호출할 수 있습니다.

- `PATCH /api/projects/{projectId}`
- `POST /api/projects/{projectId}/members`
- `DELETE /api/projects/{projectId}/members/{memberId}`
- `POST /api/projects/{projectId}/invite-links`
- `POST /api/projects/{projectId}/invitations`

권한이 없으면 아래처럼 응답합니다.

```json
{
  "isSuccess": false,
  "responseCode": 3008,
  "responseMessage": "프로젝트 팀장 권한이 필요합니다.",
  "result": null
}
```

프론트 처리:

- 프로젝트 목록/상세 응답의 role이 `LEADER`인 경우에만 팀 관리 버튼을 노출하는 것이 안전합니다.
- `403` 또는 code `3008`을 받으면 권한 없음 안내를 보여주면 됩니다.
- 프로젝트 멤버가 아닌 사용자가 프로젝트 상세/관리 API에 접근해도 `3008` 계열 권한 오류로 처리됩니다.
- 로그인 토큰이 없거나 유효하지 않으면 `401`, code `3001` 기준으로 로그인 화면 이동 처리를 하면 됩니다.

## 초대 링크 변경

### GET `/api/invitations/{inviteCode}`

- 비로그인 상태에서도 조회 가능합니다.
- 초대 화면에서 프로젝트명/과목명/리더 이름을 보여주는 용도입니다.

### POST `/api/invitations/{inviteCode}/accept`

- 로그인 토큰이 필요합니다.
- 요청 body에 `role`이 들어와도 서버는 항상 `MEMBER`로 저장합니다.

프론트 처리:

- 초대 링크 진입 시 로그인하지 않은 사용자는 로그인/회원가입 후 accept를 다시 호출해야 합니다.
- 초대 수락 성공 후 `GET /api/projects`를 다시 호출해 프로젝트 목록을 갱신해야 합니다.
- 초대 수락 요청에는 role을 보낼 필요가 없습니다.

## 프로젝트 ID 주의

- `projectId`는 더 이상 `1` 고정값이 아닙니다.
- `POST /api/projects` 응답의 `result.projectId`를 저장해서 이후 API path에 사용해야 합니다.
- 초대 수락 후에도 `GET /api/projects` 응답에서 받은 실제 `projectId`를 써야 합니다.

## 리포트 API

- `POST /api/projects/{projectId}/reports`
- `GET /api/projects/{projectId}/reports`
- `GET /api/projects/{projectId}/reports/{reportId}/download`
- `GET /api/reports/{reportId}/download`

PDF 다운로드는 기존처럼 파일 다운로드로 처리하면 됩니다.

백엔드 변경 사항:

- PDF 다운로드 API path는 바뀌지 않았습니다.
- 생성되는 PDF 내용은 단순 숫자 요약에서 프로젝트 진행 상황 리포트로 보강되었습니다.
- PDF에는 태스크 완료율, 주요 지표, 프로젝트 개요, 태스크 현황, 팀원별 작업 현황, 지연/병목 확인 항목, 회의, 리스크, 다음 액션, 활동 로그가 포함됩니다.
- 실제 데이터가 없는 섹션은 백엔드에서 empty state 문구로 표시합니다.
- 프론트에서 PDF 본문을 조립할 필요는 없습니다.

## 프론트 QA 체크리스트

- [ ] 로그인 성공 후 access/refresh token 저장
- [ ] 보호 API 호출 시 `Authorization` 헤더 포함
- [ ] access token 만료 상황에서 `/api/auth/refresh` 호출
- [ ] refresh 성공 시 새 refresh token으로 교체
- [ ] refresh 실패 시 로그인 화면 이동
- [ ] 프로젝트 생성 후 실제 `projectId`로 상세/태스크/회의/리포트 API 호출
- [ ] `MEMBER` 계정에서 팀 관리 버튼 숨김
- [ ] `403`/`3008` 권한 오류 안내 처리
- [ ] 비멤버 계정으로 프로젝트 상세/초대 생성 API 접근 시 권한 없음 처리
- [ ] 초대 링크 비로그인 조회
- [ ] 로그인 후 초대 수락
- [ ] 초대 수락 후 프로젝트 목록 재조회
- [ ] PDF 리포트 다운로드 동작 확인
- [ ] 다운로드한 PDF에서 한글이 `?`로 깨지지 않는지 확인

## 로컬 기준

백엔드 로컬 기본 포트는 환경에 따라 다를 수 있습니다.

- 일반 dev: `http://127.0.0.1:8080`
- smoke 검증에서 사용한 임시 포트: `http://127.0.0.1:18084`

프론트 `.env` 또는 API base URL은 실제 실행 중인 백엔드 포트에 맞춰야 합니다.

## 출시 전 API 고정 기준

현재부터는 API path, 요청 body, 응답 필드명을 추가 변경하지 않는 것을 기준으로 봅니다.
프론트에서 새로 맞춰야 하는 변경은 아래 항목만 보면 됩니다.

- 인증 필요한 API에는 `Authorization: Bearer ...`를 붙입니다.
- access token 만료 시 `POST /api/auth/refresh`를 호출하고, 응답의 새 access/refresh token을 모두 저장합니다.
- 프로젝트 생성/초대 수락 이후에는 응답 또는 `GET /api/projects`에서 받은 실제 `projectId`를 사용합니다.
- `LEADER`가 아닌 사용자는 프로젝트 관리 버튼을 숨기고, `403`/`3008`은 권한 없음 안내로 처리합니다.
- 출시 prod 환경에서 legacy `/api/mobile/**`가 막혀 있으면 공식 API로 전환해야 합니다.

## 이번 런타임 검증 결과

- 회원가입, 프로젝트 생성, 태스크 생성, 회의록 생성, 리포트 생성 흐름 정상 확인
- `POST /api/auth/refresh` 후 기존 refresh token 재사용 실패 확인
- `MEMBER`가 초대 링크를 다시 생성하려 할 때 `403`, code `3008` 반환 확인
- 초대 수락 요청에 `role: "LEADER"`를 보내도 응답 role은 `MEMBER`로 고정 확인
- 로그인 5회 실패 후 정상 비밀번호로 로그인해도 `429`, code `2015` 반환 확인
- PDF 다운로드 파일이 `%PDF-1.4` 형식으로 생성되는 것 확인
