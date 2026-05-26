package com.teampulse.backend.common.api;

import com.teampulse.backend.domain.risk.RiskSeverity;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.domain.team.TeamRole;
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

}
