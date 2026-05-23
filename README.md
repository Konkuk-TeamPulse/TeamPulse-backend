# TeamPulse Backend

Spring Boot 기반 TeamPulse 백엔드. 회원가입, 프로젝트, 태스크, 회의록, 초대, 리포트, 리스크 API를 제공한다.

- 운영 배포: https://teampulse-api.duckdns.org
- Frontend repo: https://github.com/Konkuk-TeamPulse/TeamPulse-frontend
- 운영 상태 확인: `GET /api/health` -> `status=UP`, `storageMode=mysql`

---

## 실행 환경

- Java 17 이상
- Maven Wrapper (`./mvnw` 또는 `.\mvnw.cmd`) 포함
- 운영 DB: MySQL (AWS RDS), 로컬 데모는 인메모리

## 환경변수 (.env)

운영 또는 로컬에서 MySQL 프로필로 실행할 때 다음 변수를 정의한다. `.env.example` 참고.

| 변수 | 설명 |
|---|---|
| `PORT` | 서버 포트 (기본 8080) |
| `SPRING_PROFILES_ACTIVE` | `mysql` (운영) / `demo` (로컬 인메모리) |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://<host>:3306/teampulse?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8` |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자 |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 |
| `APP_CORS_ALLOWED_ORIGINS` | 콤마 구분 origin 목록 (운영 프론트 도메인 포함) |
| `APP_FRONTEND_PUBLIC_BASE_URL` | 초대 링크 생성용 프론트 base URL |

비밀값은 commit 하지 않는다. 운영 환경에서는 GitHub Actions/EC2 시크릿으로 주입한다.

## 로컬 실행

### 운영(MySQL) 프로필

```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:SPRING_DATASOURCE_URL="jdbc:mysql://<host>:3306/teampulse?useSSL=true&serverTimezone=Asia/Seoul"
$env:SPRING_DATASOURCE_USERNAME="<user>"
$env:SPRING_DATASOURCE_PASSWORD="<password>"
.\mvnw.cmd spring-boot:run
```

### 데모(인메모리) 프로필

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

데모 프로필은 인메모리 저장이며 재시작 시 데이터가 초기화된다.

## 테스트

```powershell
.\mvnw.cmd test "-Dspring.profiles.active=demo"
```

현재 main(`2fa5c46`) 기준 JUnit/MockMvc 테스트 25개 통과 (Tests run: 25, Failures: 0, Errors: 0).

## 배포

- main 브랜치 push -> GitHub Actions(`.github/workflows/deploy-ec2.yml`)가 EC2 인스턴스에 SSH 접속하여 빌드/재기동.
- 배포 직후 `curl http://localhost:8080/api/health`가 30회까지 재시도로 헬스체크.

## 주요 API

명세형 API 응답은 `isSuccess`, `responseCode`, `responseMessage`, `result` wrapper를 사용한다.

- 인증: `POST /api/auth/signup`, `/login`, `/logout`
- 프로젝트: `POST/GET/PATCH /api/projects`, `GET /api/projects/{id}/dashboard`
- 태스크: `POST/GET /api/projects/{id}/tasks`, `PATCH/DELETE /api/tasks/{id}`, `PATCH /api/tasks/{id}/status`
- 의존관계: `POST /api/tasks/{id}/dependencies`, `DELETE /api/tasks/{id}/dependencies/{dependencyId}`
- 회의록: `POST/GET /api/projects/{id}/meetings`, `GET /api/projects/{id}/meetings/{meetingId}`
- 초대: `POST /api/projects/{id}/invitations`, `GET/POST /api/invitations/{code}`
- 리포트: `POST /api/projects/{id}/reports`, `GET /api/reports/{id}/download`
- 리스크: `GET /api/projects/{id}/risks`, `/risks/{riskId}/actions`
- 헬스: `GET /api/health`

## 디렉터리 구조

- `auth.api` 인증 컨트롤러
- `mobile.api` REST 컨트롤러 (프론트 연동)
- `mobile.application(.service)` UseCase와 서비스 구현 (InMemory/Jpa 두 가지)
- `mobile.dto` 요청/응답 record
- `mobile.persistence` JPA 엔티티, Repository
- `common` 공통 설정과 예외 처리
- `domain` 핵심 도메인 enum

규모가 커지면 `task`, `meeting`, `team`, `report` 같은 도메인별 패키지로 확장한다.
