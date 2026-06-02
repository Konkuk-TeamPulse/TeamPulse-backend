package com.teampulse.backend.workspace.application;


import com.teampulse.backend.workspace.dto.*;
public interface WorkspaceMemberUseCase {

    WorkspaceState addMember(CreateMemberRequest request);

    WorkspaceState deleteMember(long memberId);
}
