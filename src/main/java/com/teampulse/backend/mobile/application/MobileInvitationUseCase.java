package com.teampulse.backend.mobile.application;

import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.mobile.dto.WorkspaceState;

public interface MobileInvitationUseCase {

    WorkspaceState getWorkspaceByInviteCode(String inviteCode);

    WorkspaceState acceptInvitation(String inviteCode, String memberName, String memberEmail, TeamRole role);
}
