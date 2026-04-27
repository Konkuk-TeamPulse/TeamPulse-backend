package com.teampulse.backend.mobile.application;


import com.teampulse.backend.mobile.dto.*;
public interface MobileMeetingUseCase {

    WorkspaceState createMeeting(CreateMeetingRequest request);
}
