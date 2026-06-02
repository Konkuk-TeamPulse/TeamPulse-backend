package com.teampulse.backend.workspace.api;

import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.workspace.application.ProjectWorkspaceUseCase;
import com.teampulse.backend.workspace.dto.MeetingActionItemResponse;
import com.teampulse.backend.workspace.dto.MeetingAttendeeResponse;
import com.teampulse.backend.workspace.dto.MeetingDetailResponse;
import com.teampulse.backend.workspace.dto.MeetingView;
import com.teampulse.backend.workspace.dto.MemberView;
import com.teampulse.backend.workspace.dto.WorkspaceState;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meetings")
public class MeetingApiController {

    private static final String SUCCESS_MESSAGE = "\uC694\uCCAD\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4.";

    private final ProjectWorkspaceUseCase projectWorkspaceUseCase;

    public MeetingApiController(ProjectWorkspaceUseCase projectWorkspaceUseCase) {
        this.projectWorkspaceUseCase = projectWorkspaceUseCase;
    }

    @GetMapping("/{meetingId}")
    public SpecResponse<MeetingDetailResponse> getMeeting(@PathVariable long meetingId) {
        var workspace = projectWorkspaceUseCase.getProjectWorkspaceByMeetingId(meetingId);
        var meeting = workspace.meetings().stream()
                .filter(candidate -> candidate.id() == meetingId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found."));
        return SpecResponse.ok(SUCCESS_MESSAGE, detailResponse(workspace, meeting));
    }

    private MeetingDetailResponse detailResponse(WorkspaceState workspace, MeetingView meeting) {
        var membersById = workspace.members().stream()
                .collect(Collectors.toMap(MemberView::id, Function.identity()));
        return new MeetingDetailResponse(
                meeting.id(),
                workspace.projectId(),
                meeting.title(),
                meeting.time(),
                meeting.agenda(),
                meeting.content(),
                String.join("\n", meeting.decisions()),
                attendees(meeting, membersById),
                actionItems(meeting, membersById),
                meeting.createdAt(),
                meeting.updatedAt());
    }

    private List<MeetingAttendeeResponse> attendees(MeetingView meeting, Map<Long, MemberView> membersById) {
        return meeting.attendeeIds().stream()
                .map(memberId -> {
                    var member = membersById.get(memberId);
                    return new MeetingAttendeeResponse(memberId, member == null ? "" : member.name());
                })
                .toList();
    }

    private List<MeetingActionItemResponse> actionItems(MeetingView meeting, Map<Long, MemberView> membersById) {
        var source = meeting.actionItems();
        return java.util.stream.IntStream.range(0, source.size())
                .mapToObj(index -> {
                    var item = source.get(index);
                    var member = item.assigneeId() == null ? null : membersById.get(item.assigneeId());
                    return new MeetingActionItemResponse(
                            index + 1L,
                            item.content(),
                            item.assigneeId(),
                            member == null ? "" : member.name(),
                            item.dueDate(),
                            false);
                })
                .toList();
    }
}
