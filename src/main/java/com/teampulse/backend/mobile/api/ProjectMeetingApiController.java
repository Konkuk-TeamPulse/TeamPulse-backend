package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.mobile.application.MobileMeetingUseCase;
import com.teampulse.backend.mobile.application.WorkspaceQueryUseCase;
import com.teampulse.backend.mobile.dto.CreateMeetingRequest;
import com.teampulse.backend.mobile.dto.MeetingSpecResponse;
import com.teampulse.backend.mobile.dto.MeetingView;
import com.teampulse.backend.mobile.dto.WorkspaceState;
import jakarta.validation.Valid;
import java.util.Comparator;
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
    private static final String SUCCESS_MESSAGE = "\uC694\uCCAD\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4.";
    private static final String MEETING_CREATED_MESSAGE = "\uD68C\uC758\uB85D\uC774 \uC0DD\uC131\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";

    private final WorkspaceQueryUseCase workspaceQueryUseCase;
    private final MobileMeetingUseCase mobileMeetingUseCase;

    public ProjectMeetingApiController(WorkspaceQueryUseCase workspaceQueryUseCase, MobileMeetingUseCase mobileMeetingUseCase) {
        this.workspaceQueryUseCase = workspaceQueryUseCase;
        this.mobileMeetingUseCase = mobileMeetingUseCase;
    }

    @GetMapping
    public SpecResponse<List<MeetingSpecResponse>> listMeetings(@PathVariable long projectId) {
        requireDemoProject(projectId);
        var meetings = workspaceQueryUseCase.getWorkspace().meetings().stream()
                .map(MeetingSpecResponse::from)
                .toList();
        return SpecResponse.ok(SUCCESS_MESSAGE, meetings);
    }

    @PostMapping
    public SpecResponse<MeetingSpecResponse> createMeeting(
            @PathVariable long projectId,
            @Valid @RequestBody CreateMeetingRequest request
    ) {
        requireDemoProject(projectId);
        var workspace = mobileMeetingUseCase.createMeeting(request);
        return SpecResponse.ok(MEETING_CREATED_MESSAGE, MeetingSpecResponse.from(latestMeeting(workspace)));
    }

    @GetMapping("/{meetingId}")
    public SpecResponse<MeetingSpecResponse> getMeeting(@PathVariable long projectId, @PathVariable long meetingId) {
        requireDemoProject(projectId);
        var meeting = workspaceQueryUseCase.getWorkspace().meetings().stream()
                .filter(candidate -> candidate.id() == meetingId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found."));
        return SpecResponse.ok(SUCCESS_MESSAGE, MeetingSpecResponse.from(meeting));
    }

    private MeetingView latestMeeting(WorkspaceState workspace) {
        return workspace.meetings().stream()
                .max(Comparator.comparingLong(MeetingView::id))
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found."));
    }

    private void requireDemoProject(long projectId) {
        if (projectId != DEMO_PROJECT_ID) {
            throw new IllegalArgumentException("Only demo project 1 is available in the MVP backend.");
        }
    }
}
