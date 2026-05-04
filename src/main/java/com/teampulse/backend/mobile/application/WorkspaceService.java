package com.teampulse.backend.mobile.application;

public interface WorkspaceService extends
        WorkspaceQueryUseCase,
        WorkspaceLifecycleUseCase,
        MobileAccountUseCase,
        MobileTaskUseCase,
        MobileMeetingUseCase,
        MobileReportUseCase,
        MobileTeamUseCase,
        MobileMemberUseCase,
        MobileInvitationUseCase,
        ProjectWorkspaceUseCase {
}
