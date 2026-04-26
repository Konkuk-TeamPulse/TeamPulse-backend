# TeamPulse Backend

Spring Boot 기반 TeamPulse 백엔드입니다.

## 실행

```powershell
.\mvnw.cmd spring-boot:run
```

기본 포트는 `8080`이다.

## 테스트

```powershell
.\mvnw.cmd test
```

로컬 Maven Wrapper 문제가 있으면 설치된 Maven으로 실행할 수 있다.

```powershell
mvn test
```

## 주요 API

현재 프론트 데모와 연결되는 모바일 API는 `/api/mobile/...` 경로를 사용한다.

- `GET /api/mobile/workspace`
- `POST /api/mobile/workspace/bootstrap`
- `POST /api/mobile/workspace/reset`
- `POST /api/mobile/workspace/sample`
- `POST /api/mobile/tasks`
- `PATCH /api/mobile/tasks/{taskId}/status`
- `DELETE /api/mobile/tasks/{taskId}`
- `POST /api/mobile/meetings`
- `POST /api/mobile/reports`
- `PATCH /api/mobile/team`
- `POST /api/mobile/team/regenerate-invite`
- `POST /api/mobile/members`
- `DELETE /api/mobile/members/{memberId}`

## 구조

- `mobile.api`: 현재 프론트와 연결되는 REST 컨트롤러
- `mobile.application`: 기능별 UseCase 인터페이스와 서비스 계약
- `mobile.application.service`: UseCase 구현체와 위험도 계산 로직
- `mobile.dto`: API 요청/응답 record
- `mobile.persistence`: JPA 엔티티, Repository, Converter
- `common`: 공통 설정과 예외 처리
- `domain`: JPA 엔티티
- `repository`: 데이터 접근 계층

`mobile` 패키지는 API, Application, DTO, Persistence 책임을 나눠 두었다. 이후 실제 회원가입, 팀, 과제, 회의, 리포트 기능을 확장할 때 각 기능의 Controller, UseCase, Service, Entity를 분리해서 담당자별 작업이 가능하다.

## 구조 원칙

- 현재 규모에서는 기능별 하위 패키지를 지나치게 늘리지 않는다.
- 컨트롤러는 HTTP 요청/응답만 담당한다.
- UseCase 인터페이스는 프론트가 호출하는 기능 단위의 계약만 표현한다.
- 서비스 구현체는 현재 `InMemoryWorkspaceService`, `JpaWorkspaceService` 두 개로 유지한다.
- DTO는 외부 API 계약이므로 `mobile.dto`에 모아 관리한다.
- 저장소/JPA 엔티티는 `mobile.persistence`에 모아 둔다.

이 구조는 과제/데모 단계에서는 단순하게 유지하고, 실제 기능이 커질 때 `task`, `meeting`, `team`, `report` 같은 도메인별 패키지로 확장할 수 있도록 만든 절충안이다.

## 로컬 검증

다음 항목을 로컬에서 확인했다.

- `mvn test -Dspring.profiles.active=demo` 통과
- 로컬 서버 `spring-boot:run`, demo profile, `18080` 포트 기동 확인
- `GET /api/health`
- `GET /api/mobile/workspace`
- `POST /api/mobile/workspace/bootstrap`
- `POST /api/mobile/members`
- `POST /api/mobile/tasks`
- `POST /api/mobile/meetings`
- `POST /api/mobile/reports`

검증 결과:

- health status: `UP`
- workspace 조회 성공
- bootstrap 후 workspace 초기화 성공
- 멤버 추가 성공
- 태스크 추가 성공
- 회의 추가 및 action task 생성 성공
- 리포트 생성 성공
