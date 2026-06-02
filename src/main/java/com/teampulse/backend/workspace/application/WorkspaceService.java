package com.teampulse.backend.workspace.application;

public interface WorkspaceService extends
        WorkspaceQueryUseCase,
        WorkspaceLifecycleUseCase,
        WorkspaceTaskUseCase,
        WorkspaceMeetingUseCase,
        WorkspaceReportUseCase,
        WorkspaceTeamUseCase,
        WorkspaceMemberUseCase,
        WorkspaceInvitationUseCase,
        ProjectWorkspaceUseCase {
}
