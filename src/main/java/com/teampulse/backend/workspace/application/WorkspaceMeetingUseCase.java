package com.teampulse.backend.workspace.application;


import com.teampulse.backend.workspace.dto.*;
public interface WorkspaceMeetingUseCase {

    WorkspaceState createMeeting(CreateMeetingRequest request);
}
