package com.teampulse.backend.mobile.application;

public interface WorkspaceService extends
        WorkspaceQueryUseCase,
        WorkspaceLifecycleUseCase,
        MobileTaskUseCase,
        MobileMeetingUseCase,
        MobileReportUseCase,
        MobileTeamUseCase,
        MobileMemberUseCase {
}
