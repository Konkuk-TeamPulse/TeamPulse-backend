package com.teampulse.backend.workspace.application;

import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.workspace.dto.WorkspaceState;

public interface WorkspaceInvitationUseCase {

    WorkspaceState getWorkspaceByInviteCode(String inviteCode);

    WorkspaceState acceptInvitation(String inviteCode, String memberName, String memberEmail, TeamRole role);
}
