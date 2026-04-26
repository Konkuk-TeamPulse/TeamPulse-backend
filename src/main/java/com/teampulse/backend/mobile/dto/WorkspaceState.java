package com.teampulse.backend.mobile.dto;

import java.util.List;

public record WorkspaceState(
        boolean initialized,
        UserProfile user,
        TeamProfile team,
        List<MemberView> members,
        List<TaskView> tasks,
        List<MeetingView> meetings,
        List<ActivityView> activities,
        List<ReportView> reports,
        List<RiskView> risks
) {
}
