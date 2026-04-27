package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.ApiResponse;
import com.teampulse.backend.mobile.application.MobileMeetingUseCase;
import com.teampulse.backend.mobile.application.WorkspaceQueryUseCase;
import com.teampulse.backend.mobile.dto.CreateMeetingRequest;
import com.teampulse.backend.mobile.dto.MeetingView;
import com.teampulse.backend.mobile.dto.WorkspaceState;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/meetings")
public class ProjectMeetingApiController {

    private static final long DEMO_PROJECT_ID = 1L;

    private final WorkspaceQueryUseCase workspaceQueryUseCase;
    private final MobileMeetingUseCase mobileMeetingUseCase;

    public ProjectMeetingApiController(WorkspaceQueryUseCase workspaceQueryUseCase, MobileMeetingUseCase mobileMeetingUseCase) {
        this.workspaceQueryUseCase = workspaceQueryUseCase;
        this.mobileMeetingUseCase = mobileMeetingUseCase;
    }

    @GetMapping
    public ApiResponse<List<MeetingView>> listMeetings(@PathVariable long projectId) {
        requireDemoProject(projectId);
        return ApiResponse.ok(workspaceQueryUseCase.getWorkspace().meetings());
    }

    @PostMapping
    public ApiResponse<WorkspaceState> createMeeting(
            @PathVariable long projectId,
            @Valid @RequestBody CreateMeetingRequest request
    ) {
        requireDemoProject(projectId);
        return ApiResponse.ok(mobileMeetingUseCase.createMeeting(request));
    }

    @GetMapping("/{meetingId}")
    public ApiResponse<MeetingView> getMeeting(@PathVariable long projectId, @PathVariable long meetingId) {
        requireDemoProject(projectId);
        var meeting = workspaceQueryUseCase.getWorkspace().meetings().stream()
                .filter(candidate -> candidate.id() == meetingId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found."));
        return ApiResponse.ok(meeting);
    }

    private void requireDemoProject(long projectId) {
        if (projectId != DEMO_PROJECT_ID) {
            throw new IllegalArgumentException("Only demo project 1 is available in the MVP backend.");
        }
    }
}
