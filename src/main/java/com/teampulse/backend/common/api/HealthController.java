package com.teampulse.backend.common.api;

import com.teampulse.backend.domain.risk.RiskSeverity;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.domain.team.TeamRole;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${app.runtime.storage-mode:demo}")
    private String storageMode;

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "status", "UP",
                "service", "teampulse-backend",
                "currentPhase", "Phase 0 -> Phase 1",
                "storageMode", storageMode,
                "publicApi", true,
                "deploymentTarget", Map.of(
                        "frontend", "Vercel Production",
                        "backend", "AWS Elastic Beanstalk",
                        "database", "AWS RDS MySQL"
                ),
                "enums", Map.of(
                        "teamRoles", TeamRole.values(),
                        "taskStatus", TaskStatus.values(),
                        "riskSeverity", RiskSeverity.values()
                )
        ));
    }

    // Deprecated: 프론트엔드는 상태 확인에 GET /api/health 만 사용합니다.
    @Deprecated
    @GetMapping("/roadmap")
    public ApiResponse<Map<String, Object>> roadmap() {
        return ApiResponse.ok(Map.of(
                "today", LocalDate.now().toString(),
                "steps", List.of(
                        "frontend Vercel build",
                        "backend AWS-ready config",
                        "auth and team APIs",
                        "task board and activity log",
                        "meetings, dependencies, pdf reports"
                )
        ));
    }
}
