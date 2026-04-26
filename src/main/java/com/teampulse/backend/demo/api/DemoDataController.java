package com.teampulse.backend.demo.api;

import com.teampulse.backend.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoDataController {

    @GetMapping("/shell-data")
    public ApiResponse<Map<String, Object>> shellData() {
        List<Map<String, Object>> milestones = List.of(
                Map.of("date", "2026-04-07", "label", "아키텍처 + ERD 확정"),
                Map.of("date", "2026-04-14", "label", "API 명세 + 와이어프레임 완료"),
                Map.of("date", "2026-04-21", "label", "선후행 관계 PoC 검증"),
                Map.of("date", "2026-04-28", "label", "설계 문서 최종 제출")
        );

        List<Map<String, Object>> tasks = List.of(
                Map.of(
                        "id", 1,
                        "title", "ERD 초안 확정",
                        "owner", "한지훈",
                        "status", "DOING",
                        "dueDate", "2026-04-07",
                        "priority", "HIGH",
                        "blockers", List.of(),
                        "next", List.of("API 명세 1차 작성"),
                        "note", "핵심 엔티티 정리 완료. team과 project 단일화 여부만 남음."
                ),
                Map.of(
                        "id", 2,
                        "title", "API 명세 1차 작성",
                        "owner", "이주호",
                        "status", "TODO",
                        "dueDate", "2026-04-10",
                        "priority", "HIGH",
                        "blockers", List.of("ERD 초안 확정"),
                        "next", List.of("프론트 모킹 데이터 교체"),
                        "note", "인증, 팀, 태스크, 회의, 리포트 범위를 1차 고정."
                ),
                Map.of(
                        "id", 3,
                        "title", "대시보드 UI 셸 구현",
                        "owner", "박태희",
                        "status", "DOING",
                        "dueDate", "2026-04-09",
                        "priority", "MEDIUM",
                        "blockers", List.of(),
                        "next", List.of("리스크 카드 연결"),
                        "note", "현재 프론트 MVP를 Tailwind 기준으로 재정렬하는 작업이다."
                ),
                Map.of(
                        "id", 4,
                        "title", "리스크 규칙 임계값 초안",
                        "owner", "이주호",
                        "status", "TODO",
                        "dueDate", "2026-04-12",
                        "priority", "MEDIUM",
                        "blockers", List.of("ERD 초안 확정"),
                        "next", List.of("대시보드 신호 반영"),
                        "note", "진행 정체 / 병목 / 일정 지연 위험을 우선 정의."
                ),
                Map.of(
                        "id", 5,
                        "title", "설계 점검 회의록 정리",
                        "owner", "박태희",
                        "status", "DONE",
                        "dueDate", "2026-04-05",
                        "priority", "LOW",
                        "blockers", List.of(),
                        "next", List.of(),
                        "note", "액션아이템을 태스크로 연결하는 흐름 검증 완료."
                )
        );

        List<Map<String, Object>> riskSignals = List.of(
                Map.of(
                        "id", 1,
                        "severity", "CRITICAL",
                        "title", "선행 태스크 지연 시 API 작업이 함께 밀릴 수 있습니다.",
                        "body", "ERD 초안 확정이 늦어지면 API 명세와 보드 연동 작업이 연쇄적으로 대기 상태가 됩니다.",
                        "action", "ERD 범위를 줄이거나 API 초안을 병렬로 분리 작성"
                ),
                Map.of(
                        "id", 2,
                        "severity", "WARNING",
                        "title", "백엔드 준비 작업이 한 명에게 몰리고 있습니다.",
                        "body", "현재 인증, 팀, DB 스키마 논의가 모두 한지훈 중심으로 쏠려 있습니다.",
                        "action", "API 문서화와 DB 제약조건 리뷰를 분담"
                ),
                Map.of(
                        "id", 3,
                        "severity", "INFO",
                        "title", "회의 기록과 액션아이템 연결 흐름은 비교적 안정적입니다.",
                        "body", "회의 화면 구조와 보고서 섹션 정의가 이미 문서화되어 있어 구현 진입 장벽이 낮습니다.",
                        "action", "백엔드 엔드포인트 확정 후 바로 화면 연결 가능"
                )
        );

        List<Map<String, Object>> meetings = List.of(
                Map.of(
                        "id", 1,
                        "title", "2단계 설계 점검 회의",
                        "time", "2026-04-06 19:00",
                        "agenda", "ERD, API 명세, 선후행 관계 표현 방식 확정",
                        "decisions", List.of("team = project 단순화", "리스크 5종 유지", "React + Spring 분리 구조 유지"),
                        "actions", List.of("ERD 초안 확정", "API 10개 이상 엔드포인트 명세", "프론트 셸 구현")
                ),
                Map.of(
                        "id", 2,
                        "title", "구현 진입 회의",
                        "time", "2026-04-09 18:30",
                        "agenda", "Phase 0/1 실제 착수 범위 확인",
                        "decisions", List.of("인증/팀/태스크를 첫 목표로 고정"),
                        "actions", List.of("백엔드 공통 응답 구조 작성", "프론트 API 연결")
                )
        );

        List<Map<String, Object>> activities = List.of(
                Map.of(
                        "id", 1,
                        "actor", "Codex",
                        "at", "2026-04-05 22:20",
                        "summary", "React 프론트와 Spring Boot 백엔드 골격을 생성했다."
                ),
                Map.of(
                        "id", 2,
                        "actor", "Codex",
                        "at", "2026-04-05 21:40",
                        "summary", "시스템 아키텍처, DB, API, UI, 로드맵 문서를 코텍스 워크 플레이스에 작성했다."
                ),
                Map.of(
                        "id", 3,
                        "actor", "TeamPulse",
                        "at", "2026-04-05 19:10",
                        "summary", "기획서와 SRS 기준본을 확정했다."
                )
        );

        List<Map<String, Object>> reports = List.of(
                Map.of(
                        "id", 1,
                        "label", "설계 단계 팀 요약",
                        "range", "2026-04-01 ~ 2026-04-05",
                        "status", "READY"
                ),
                Map.of(
                        "id", 2,
                        "label", "주간 활동 로그",
                        "range", "2026-04-06 ~ 2026-04-12",
                        "status", "GENERATING"
                ),
                Map.of(
                        "id", 3,
                        "label", "발표 제출용 프로젝트 리포트",
                        "range", "2026-04-01 ~ 2026-06-09",
                        "status", "FAILED"
                )
        );

        Map<String, Object> payload = Map.of(
                "meta", Map.of(
                        "teamName", "Team 4",
                        "workspaceName", "TeamPulse workspace",
                        "currentPhase", "Phase 0 -> Phase 1",
                        "deadline", "2026-06-09",
                        "stack", List.of("React SPA", "Tailwind CSS", "Spring Boot", "MySQL"),
                        "deployment", Map.of(
                                "frontend", "Vercel Production",
                                "backend", "AWS App Runner or Elastic Beanstalk",
                                "database", "AWS RDS MySQL"
                        )
                ),
                "milestones", milestones,
                "tasks", tasks,
                "riskSignals", riskSignals,
                "meetings", meetings,
                "activities", activities,
                "reports", reports
        );

        return ApiResponse.ok(payload);
    }
}
