package com.teampulse.backend.workspace.dto;

import java.util.List;

public record DashboardResponse(
        long projectId,
        String projectName,
        TaskSummary taskSummary,
        ScheduleSummary scheduleSummary,
        List<MemberWorkload> memberWorkload,
        RiskSummary riskSummary,
        List<DashboardRisk> risks
) {
    public record TaskSummary(
            int totalTaskCount,
            int todoCount,
            int inProgressCount,
            int doneCount,
            double progressRate
    ) {
    }

    public record ScheduleSummary(
            String projectStartDate,
            String projectEndDate,
            long remainingDays,
            int overdueTaskCount,
            int dueSoonTaskCount
    ) {
    }

    public record MemberWorkload(
            long memberId,
            String name,
            int assignedTaskCount,
            int doneTaskCount
    ) {
    }

    public record RiskSummary(
            int totalRiskCount,
            int cautionCount,
            int warningCount,
            int dangerCount,
            boolean hasDanger
    ) {
    }

    public record DashboardRisk(
            String type,
            String level,
            String message,
            Long relatedTaskId,
            String relatedTaskTitle,
            Long relatedMemberId,
            String relatedMemberName,
            List<Long> affectedTaskIds,
            List<String> suggestedActions
    ) {
    }
}
