package com.teampulse.backend.workspace.application;


import com.teampulse.backend.workspace.dto.*;
public interface WorkspaceTeamUseCase {

    WorkspaceState updateTeam(UpdateTeamRequest request);

    WorkspaceState regenerateInviteCode();
}
