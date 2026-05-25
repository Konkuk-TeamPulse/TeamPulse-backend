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

    @Deprecated
    @GetMapping("/shell-data")
    public ApiResponse<Map<String, Object>> shellData() {
        return ApiResponse.ok(Map.of(
                "meta", meta(),
                "milestones", milestones(),
                "tasks", tasks(),
                "riskSignals", riskSignals(),
                "meetings", meetings(),
                "activities", activities(),
                "reports", reports()
        ));
    }

    private Map<String, Object> meta() {
        return Map.of(
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
        );
    }

    private List<Map<String, Object>> milestones() {
        return List.of(
                Map.of("date", "2026-04-07", "label", "아키텍처와 ERD 확정"),
                Map.of("date", "2026-04-14", "label", "API 명세와 데이터 플로우 완료"),
                Map.of("date", "2026-04-21", "label", "주요 기능 PoC 검증"),
                Map.of("date", "2026-04-28", "label", "설계 문서 최종 제출")
        );
    }

    private List<Map<String, Object>> tasks() {
        return List.of(
                task(1, "ERD 초안 확정", "서지훈", "DOING", "2026-04-07", "HIGH"),
                task(2, "API 명세 1차 작성", "이주현", "TODO", "2026-04-10", "HIGH"),
                task(3, "대시보드 UI 구현", "박태민", "DOING", "2026-04-09", "MEDIUM"),
                task(4, "리스크 규칙 초안", "이주현", "TODO", "2026-04-12", "MEDIUM"),
                task(5, "설계 회의 정리", "박태민", "DONE", "2026-04-05", "LOW")
        );
    }

    private Map<String, Object> task(int id, String title, String owner, String status, String dueDate, String priority) {
        return Map.of(
                "id", id,
                "title", title,
                "owner", owner,
                "status", status,
                "dueDate", dueDate,
                "priority", priority,
                "blockers", List.of(),
                "next", List.of(),
                "note", "Demo workspace sample task."
        );
    }

    private List<Map<String, Object>> riskSignals() {
        return List.of(
                risk(1, "CRITICAL", "선행 작업 지연", "API 작업이 ERD 확정에 묶여 있습니다."),
                risk(2, "WARNING", "업무 집중", "백엔드 준비 작업이 한 명에게 몰려 있습니다."),
                risk(3, "INFO", "회의 기록 안정", "회의 기록과 액션 아이템 흐름은 안정적입니다.")
        );
    }

    private Map<String, Object> risk(int id, String severity, String title, String body) {
        return Map.of(
                "id", id,
                "severity", severity,
                "title", title,
                "body", body,
                "action", "범위를 줄이거나 작업을 분리하세요."
        );
    }

    private List<Map<String, Object>> meetings() {
        return List.of(
                meeting(1, "2단계 설계 회의", "2026-04-06 19:00"),
                meeting(2, "구현 진입 회의", "2026-04-09 18:30")
        );
    }

    private Map<String, Object> meeting(int id, String title, String time) {
        return Map.of(
                "id", id,
                "title", title,
                "time", time,
                "agenda", "ERD, API 명세, 향후 일정 정리",
                "decisions", List.of("React + Spring 분리 구조 유지"),
                "actions", List.of("API 명세 보완", "프론트 연동 준비")
        );
    }

    private List<Map<String, Object>> activities() {
        return List.of(
                activity(1, "Codex", "2026-04-05 22:20", "React 프론트와 Spring Boot 백엔드 골격 생성"),
                activity(2, "Codex", "2026-04-05 21:40", "문서와 로드맵 초안 작성"),
                activity(3, "TeamPulse", "2026-04-05 19:10", "기획서 기반 요구사항 확정")
        );
    }

    private Map<String, Object> activity(int id, String actor, String at, String summary) {
        return Map.of("id", id, "actor", actor, "at", at, "summary", summary);
    }

    private List<Map<String, Object>> reports() {
        return List.of(
                report(1, "설계 단계 요약", "2026-04-01 ~ 2026-04-05", "READY"),
                report(2, "주간 활동 로그", "2026-04-06 ~ 2026-04-12", "GENERATING"),
                report(3, "프로젝트 리포트", "2026-04-01 ~ 2026-06-09", "FAILED")
        );
    }

    private Map<String, Object> report(int id, String label, String range, String status) {
        return Map.of("id", id, "label", label, "range", range, "status", status);
    }
}
