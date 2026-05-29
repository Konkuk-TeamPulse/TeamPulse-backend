package com.teampulse.backend.mobile.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.mobile.dto.BootstrapWorkspaceRequest;
import com.teampulse.backend.mobile.dto.CreateMeetingRequest;
import com.teampulse.backend.mobile.dto.CreateMemberRequest;
import com.teampulse.backend.mobile.dto.CreateTaskRequest;
import com.teampulse.backend.mobile.dto.MeetingActionItemView;
import com.teampulse.backend.mobile.dto.TaskDependencyRequest;
import com.teampulse.backend.mobile.dto.TaskView;
import com.teampulse.backend.mobile.dto.UpdateTaskRequest;
import com.teampulse.backend.mobile.dto.UpdateTaskStatusRequest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryWorkspaceServiceTest {

    private InMemoryWorkspaceService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryWorkspaceService(new RiskEngine());
    }

    @Test
    void projectAccessorsRequireInitializedWorkspaceAndCorrectProjectId() {
        assertThat(service.getProjectWorkspaces()).isEmpty();
        assertThatThrownBy(() -> service.getProjectWorkspace(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Workspace is not initialized yet.");
        assertThatThrownBy(() -> service.resetProjectWorkspace(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project not found.");

        var workspace = bootstrap();

        assertThat(workspace.initialized()).isTrue();
        assertThat(service.getProjectWorkspaces()).hasSize(1);
        assertThat(service.getProjectWorkspace(1L).team().name()).isEqualTo("TeamPulse");
    }

    @Test
    void taskOperationsCoverValidationDefaultsAndDependencyBranches() {
        bootstrap();

        assertThatThrownBy(() -> service.createTask(new CreateTaskRequest("Ghost", "Missing", "2026-05-10", List.of(), "")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task owner must be an existing team member.");
        assertThatThrownBy(() -> service.createTask(new CreateTaskRequest("Bad date", "Owner", "2026-99-99", List.of(), "")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task due date must use a real yyyy-MM-dd date.");

        service.createTask(new CreateTaskRequest("Base", "Owner", "2026-05-10", null, null));
        service.createTask(new CreateTaskRequest("Follow", "Owner", "2026-05-11", Arrays.asList(" ", null), " Follow note "));
        var baseTask = taskByTitle("Base");
        var followTask = taskByTitle("Follow");

        service.addTaskDependency(followTask.id(), new TaskDependencyRequest("Base"));
        service.addTaskDependency(followTask.id(), new TaskDependencyRequest("Base"));
        assertThat(taskByTitle("Follow").blockers()).containsExactly("Base");

        assertThatThrownBy(() -> service.addTaskDependency(baseTask.id(), new TaskDependencyRequest("Base")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task cannot depend on itself.");
        assertThatThrownBy(() -> service.addTaskDependency(999L, new TaskDependencyRequest("Base")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task not found.");

        service.deleteTaskDependency(followTask.id(), "base");
        assertThat(taskByTitle("Follow").blockers()).isEmpty();
        assertThatThrownBy(() -> service.deleteTaskDependency(999L, "Base"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task not found.");

        assertThatThrownBy(() -> service.updateTaskStatus(baseTask.id(), new UpdateTaskStatusRequest(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task status is required.");
        service.updateTask(baseTask.id(), new UpdateTaskRequest(
                null,
                null,
                TaskStatus.DONE,
                "2026-05-12",
                null,
                null,
                List.of("Follow"),
                null));
        assertThat(taskByTitle("Base").status()).isEqualTo(TaskStatus.DONE);
        assertThat(taskByTitle("Base").next()).containsExactly("Follow");
    }

    @Test
    void meetingInviteMemberAndReportOperationsCoverEdgeBranches() {
        bootstrap();

        assertThatThrownBy(() -> service.createMeeting(new CreateMeetingRequest(
                "Bad Meeting", "2026-99-99", "Agenda", List.of(), List.of(), null, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Meeting time must use a real yyyy-MM-dd or yyyy-MM-ddTHH:mm value.");

        var workspaceWithMeeting = service.createMeeting(new CreateMeetingRequest(
                "Planning",
                "2026-05-01T10:00",
                "Plan next proof",
                List.of("", "Decision"),
                List.of(" Capture evidence ", " "),
                "",
                true,
                "Content",
                Arrays.asList(null, -1L, 10L),
                Arrays.asList(null, new MeetingActionItemView(" ", 1L, null), new MeetingActionItemView("Ship proof", 1L, null))));

        assertThat(workspaceWithMeeting.meetings()).hasSize(1);
        assertThat(workspaceWithMeeting.meetings().get(0).decisions()).containsExactly("Decision");
        assertThat(workspaceWithMeeting.meetings().get(0).attendeeIds()).containsExactly(10L);
        assertThat(workspaceWithMeeting.meetings().get(0).actionItems())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.content()).isEqualTo("Ship proof");
                    assertThat(item.dueDate()).isEmpty();
                });
        assertThat(workspaceWithMeeting.tasks()).extracting(TaskView::title).contains("Capture evidence");

        assertThatThrownBy(() -> service.createMeeting(new CreateMeetingRequest(
                "Action Owner", "2026-05-02", "Agenda", List.of(), List.of("Task"), "Missing", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Action owner must be an existing team member.");

        var inviteCode = service.getWorkspace().team().inviteCode();
        assertThatThrownBy(() -> service.getWorkspaceByInviteCode("wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invitation is invalid or expired.");
        service.acceptInvitation(inviteCode, "Owner", "owner@example.com", TeamRole.LEADER);
        var duplicateInvite = service.acceptInvitation(inviteCode, "Owner", "OWNER@example.com", TeamRole.MEMBER);
        assertThat(duplicateInvite.members()).hasSize(1);

        service.addMember(new CreateMemberRequest("Reviewer", null));
        assertThatThrownBy(() -> service.addMember(new CreateMemberRequest("reviewer", TeamRole.MEMBER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Member already exists.");

        var reviewerId = service.getWorkspace().members().stream()
                .filter(member -> member.name().equals("Reviewer"))
                .findFirst()
                .orElseThrow()
                .id();
        service.deleteMember(reviewerId);
        assertThat(service.getWorkspace().members()).extracting(member -> member.name()).doesNotContain("Reviewer");

        var report = service.generateReport();
        assertThat(report.reports()).hasSize(1);
        assertThat(service.getProjectWorkspaceByReportId(report.reports().get(0).id()).reports()).hasSize(1);
        assertThatThrownBy(() -> service.getProjectWorkspaceByReportId(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Report not found.");
    }

    private com.teampulse.backend.mobile.dto.WorkspaceState bootstrap() {
        return service.createProjectWorkspace(new BootstrapWorkspaceRequest(
                "Owner",
                "owner@example.com",
                "TeamPulse",
                "Advanced Project",
                "",
                "2026-06-09",
                null,
                null));
    }

    private TaskView taskByTitle(String title) {
        return service.getWorkspace().tasks().stream()
                .filter(task -> task.title().equals(title))
                .findFirst()
                .orElseThrow();
    }
}
