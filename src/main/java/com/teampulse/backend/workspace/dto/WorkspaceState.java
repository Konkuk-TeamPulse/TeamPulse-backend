package com.teampulse.backend.workspace.dto;

import java.util.List;

public record WorkspaceState(
        long projectId,
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
    public WorkspaceState(
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
        this(1L, initialized, user, team, members, tasks, meetings, activities, reports, risks);
    }
}
