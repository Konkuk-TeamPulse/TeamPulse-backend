# DB/Auth Handoff

Last updated: 2026-05-28
Scope: current backend deployment and DB/auth behavior

## Purpose

이 문서는 서버 배포, DB 연결, 인증 흐름에서 팀원이 헷갈리지 않도록 현재 기준만 정리한다. 오래된 MVP API 경로는 여기서 제외한다.

## Runtime profiles

- `demo`: 로컬 테스트용 인메모리 저장소를 사용한다.
- `mysql`: 운영/배포용 MySQL 저장소를 사용한다.
- `prod`: 배포 환경에서는 `mysql` 동작을 포함해야 한다.

## Required server environment variables

운영 서버의 실제 값은 GitHub에 커밋하지 않는다. EC2 서버 `.env` 또는 GitHub Secrets에서 관리한다.

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_FRONTEND_PUBLIC_BASE_URL`

주의: 현재 배포 스크립트는 `.env` 값을 shell 변수로 읽는다. 값에 불필요한 따옴표를 넣으면 따옴표까지 값에 포함될 수 있으므로 서버 `.env`는 원칙적으로 `KEY=VALUE` 형식으로 작성한다.

## Main tables

- `users`: account information
- `auth_sessions`: access/refresh token sessions
- `assignment2_workspaces`: projects/workspaces
- `assignment2_members`: project members
- `assignment2_tasks`: project tasks
- `assignment2_meetings`: meeting minutes
- `assignment2_reports`: generated reports
- `assignment2_activities`: activity logs

## Current API auth policy summary

Public endpoints:

- `GET /api/health`
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/invitations/{inviteCode}`

Authenticated endpoints:

- `POST /api/auth/logout`
- `GET /api/users/me`
- `GET/POST/PATCH /api/projects/**`
- `GET/POST/PATCH/DELETE /api/tasks/**`
- `GET/POST /api/meetings/**`
- `GET /api/reports/**`
- `POST /api/invitations/{inviteCode}/accept`

## Not official anymore

아래 경로는 현재 제출/테스트 기준 공식 API가 아니다.

- `/api/mobile/**`
- `/api/account/**`
- `/api/roadmap`
- `/api/projects/{projectId}/invite-links`
- `/api/projects/{projectId}/risks/{riskId}/actions`

## Verification baseline

현재 과제 제출 기준 검증은 다음 둘을 함께 본다.

- JUnit/Spring test: `mvnw test -Dspring.profiles.active=demo`
- Postman collection: submitted Postman JSON collection

MySQL 실서버 검증은 배포 담당자가 운영 서버 환경변수와 GitHub Actions 결과까지 함께 확인한다.
