package com.teampulse.backend.mobile.application;


import com.teampulse.backend.mobile.dto.*;
public interface MobileTeamUseCase {

    WorkspaceState updateTeam(UpdateTeamRequest request);

    WorkspaceState regenerateInviteCode();
}
