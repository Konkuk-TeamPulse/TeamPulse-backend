package com.teampulse.backend.mobile.application;


import com.teampulse.backend.mobile.dto.*;
public interface MobileMemberUseCase {

    WorkspaceState addMember(CreateMemberRequest request);

    WorkspaceState deleteMember(long memberId);
}
