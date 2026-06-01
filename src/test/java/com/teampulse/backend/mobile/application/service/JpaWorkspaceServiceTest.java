package com.teampulse.backend.mobile.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.mobile.dto.BootstrapWorkspaceRequest;
import com.teampulse.backend.mobile.dto.CreateMeetingRequest;
import com.teampulse.backend.mobile.dto.CreateMemberRequest;
import com.teampulse.backend.mobile.dto.CreateTaskRequest;
import com.teampulse.backend.mobile.dto.MeetingActionItemView;
import com.teampulse.backend.mobile.dto.TaskDependencyRequest;
import com.teampulse.backend.mobile.dto.UpdateTaskRequest;
import com.teampulse.backend.mobile.dto.UpdateTaskStatusRequest;
import com.teampulse.backend.mobile.dto.UpdateTeamRequest;
import com.teampulse.backend.mobile.persistence.MobileActivityEntity;
import com.teampulse.backend.mobile.persistence.MobileMeetingEntity;
import com.teampulse.backend.mobile.persistence.MobileMemberEntity;
import com.teampulse.backend.mobile.persistence.MobileReportEntity;
import com.teampulse.backend.mobile.persistence.MobileTaskEntity;
import com.teampulse.backend.mobile.persistence.MobileWorkspaceEntity;
import com.teampulse.backend.mobile.persistence.MobileWorkspaceRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JpaWorkspaceServiceTest {

    @Mock
    private MobileWorkspaceRepository workspaceRepository;

    @Mock
    private RiskEngine riskEngine;

    private JpaWorkspaceService service;
    private final AtomicLong workspaceIds = new AtomicLong(100);
    private final AtomicLong memberIds = new AtomicLong(200);
    private final AtomicLong taskIds = new AtomicLong(300);
    private final AtomicLong meetingIds = new AtomicLong(400);
    private final AtomicLong activityIds = new AtomicLong(500);
    private final AtomicLong reportIds = new AtomicLong(600);

    @BeforeEach
    void setUp() {
        service = new JpaWorkspaceService(workspaceRepository, riskEngine);
        when(riskEngine.deriveRisks(any(), any(), any())).thenReturn(List.of());
        when(workspaceRepository.saveAndFlush(any(MobileWorkspaceEntity.class)))
                .thenAnswer(invocation -> persist(invocation.getArgument(0)));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createProjectWorkspaceInitializesDraftForAuthenticatedOwner() {
        signIn("owner@example.com");
        var draft = new MobileWorkspaceEntity();
        draft.setId(9L);
        draft.setOwnerEmail("owner@example.com");
        draft.setInviteCode("OLD123");
        when(workspaceRepository.findFirstByOwnerEmailIgnoreCaseAndInitializedFalseOrderByIdAsc("owner@example.com"))
                .thenReturn(Optional.of(draft));

        var state = service.createProjectWorkspace(new BootstrapWorkspaceRequest(
                " Owner ",
                "form@example.com",
                " TeamPulse ",
                " Capstone ",
                "",
                "2026-06-30",
                " Project description ",
                "2026-06-01"
        ));

        assertThat(state.projectId()).isEqualTo(9L);
        assertThat(state.initialized()).isTrue();
        assertThat(state.user().name()).isEqualTo("Owner");
        assertThat(state.user().email()).isEqualTo("form@example.com");
        assertThat(state.team().name()).isEqualTo("TeamPulse");
        assertThat(state.team().courseName()).isEqualTo("Capstone");
        assertThat(state.team().semester()).isEqualTo("2026-1");
        assertThat(state.team().description()).isEqualTo("Project description");
        assertThat(state.members()).singleElement()
                .satisfies(member -> {
                    assertThat(member.name()).isEqualTo("Owner");
                    assertThat(member.email()).isEqualTo("form@example.com");
                    assertThat(member.role()).isEqualTo(TeamRole.LEADER);
                });
        assertThat(state.activities()).singleElement()
                .satisfies(activity -> assertThat(activity.summary()).isEqualTo("TeamPulse workspace started."));
    }

    @Test
    void createProjectWorkspaceRejectsMissingAndInvalidDates() {
        signIn("owner@example.com");

        assertThatThrownBy(() -> service.createProjectWorkspace(new BootstrapWorkspaceRequest(
                "", "owner@example.com", "Team", "Course", "2026-1", "2026-06-30")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name is required.");
        assertThatThrownBy(() -> service.createProjectWorkspace(new BootstrapWorkspaceRequest(
                "Owner", "owner@example.com", "Team", "Course", "2026-1", "2026-02-31")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Due date must use a real yyyy-MM-dd date.");
        assertThatThrownBy(() -> service.createProjectWorkspace(new BootstrapWorkspaceRequest(
                "Owner", "owner@example.com", "Team", "Course", "2026-1", "2026-06-30", "", "2026-02-31")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start date must use a real yyyy-MM-dd date.");

        verify(workspaceRepository, never()).saveAndFlush(any());
    }

    @Test
    void getProjectWorkspaceChecksOwnerMemberAndAnonymousAccessBranches() {
        var owned = workspace(1L, "owner@example.com");
        addMember(owned, 11L, "Member", "member@example.com", TeamRole.MEMBER);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(owned));

        signIn("owner@example.com");
        assertThat(service.getProjectWorkspace(1L).projectId()).isEqualTo(1L);

        signIn("member@example.com");
        assertThat(service.getProjectWorkspace(1L).members()).hasSize(1);

        signIn("outsider@example.com");
        assertThatThrownBy(() -> service.getProjectWorkspace(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Current user is not a project member.");

        signOut();
        assertThatThrownBy(() -> service.getProjectWorkspace(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authentication user is required.");
    }

    @Test
    void getProjectWorkspaceRejectsMissingOrUninitializedProject() {
        when(workspaceRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getProjectWorkspace(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project not found.");

        var draft = workspace(2L, "");
        draft.setInitialized(false);
        when(workspaceRepository.findById(2L)).thenReturn(Optional.of(draft));
        assertThatThrownBy(() -> service.getProjectWorkspace(2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project not found.");
    }

    @Test
    void getProjectWorkspacesUsesAuthenticatedAndAnonymousLookupBranches() {
        var owned = workspace(1L, "owner@example.com");
        signIn("owner@example.com");
        when(workspaceRepository.findAccessibleInitializedByEmail("owner@example.com")).thenReturn(List.of(owned));
        assertThat(service.getProjectWorkspaces()).extracting(state -> state.projectId()).containsExactly(1L);

        signOut();
        var anonymous = workspace(2L, "");
        when(workspaceRepository.findFirstByOwnerEmailIgnoreCaseOrderByIdAsc("")).thenReturn(Optional.of(anonymous));
        assertThat(service.getProjectWorkspaces()).extracting(state -> state.projectId()).containsExactly(2L);

        var draft = workspace(3L, "");
        draft.setInitialized(false);
        when(workspaceRepository.findFirstByOwnerEmailIgnoreCaseOrderByIdAsc("")).thenReturn(Optional.of(draft));
        assertThat(service.getProjectWorkspaces()).isEmpty();
    }

    @Test
    void createProjectTaskCoversSuccessAndValidationFailures() {
        signIn("owner@example.com");
        var workspace = workspace(1L, "owner@example.com");
        addMember(workspace, 11L, "Owner", "owner@example.com", TeamRole.LEADER);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));

        var state = service.createProjectTask(1L, new CreateTaskRequest(
                " Build API ",
                " Owner ",
                "2026-06-05",
                Arrays.asList("  Design ", "", null),
                "  note "
        ));

        assertThat(state.tasks()).singleElement()
                .satisfies(task -> {
                    assertThat(task.title()).isEqualTo("Build API");
                    assertThat(task.owner()).isEqualTo("Owner");
                    assertThat(task.status()).isEqualTo(TaskStatus.TODO);
                    assertThat(task.dueDate()).isEqualTo("2026-06-05");
                    assertThat(task.priority()).isEqualTo("HIGH");
                    assertThat(task.blockers()).containsExactly("Design");
                    assertThat(task.note()).isEqualTo("note");
                });

        assertThatThrownBy(() -> service.createProjectTask(1L, new CreateTaskRequest("", "Owner", "2026-06-05", List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task title is required.");
        assertThatThrownBy(() -> service.createProjectTask(1L, new CreateTaskRequest("Task", "", "2026-06-05", List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task owner is required.");
        assertThatThrownBy(() -> service.createProjectTask(1L, new CreateTaskRequest("Task", "Owner", "2026-02-31", List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task due date must use a real yyyy-MM-dd date.");
        assertThatThrownBy(() -> service.createProjectTask(1L, new CreateTaskRequest("Task", "Unknown", "2026-06-05", List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task owner must be an existing team member.");
    }

    @Test
    void updateTaskByIdUpdatesOptionalFieldsAndRejectsInvalidBranches() {
        signIn("owner@example.com");
        var workspace = workspace(1L, "owner@example.com");
        addMember(workspace, 11L, "Owner", "owner@example.com", TeamRole.LEADER);
        addMember(workspace, 12L, "Reviewer", "reviewer@example.com", TeamRole.MEMBER);
        addTask(workspace, 31L, "Old", "Owner", TaskStatus.TODO, "2026-06-01", List.of(), List.of(), "old");
        when(workspaceRepository.findAccessibleInitializedByEmail("owner@example.com")).thenReturn(List.of(workspace));

        var state = service.updateTaskById(31L, new UpdateTaskRequest(
                " New title ",
                " Reviewer ",
                TaskStatus.DOING,
                "2026-06-10",
                " HIGH ",
                List.of(" Plan "),
                List.of(" Ship "),
                " Updated note "
        ));

        assertThat(state.tasks()).singleElement()
                .satisfies(task -> {
                    assertThat(task.title()).isEqualTo("New title");
                    assertThat(task.owner()).isEqualTo("Reviewer");
                    assertThat(task.status()).isEqualTo(TaskStatus.DOING);
                    assertThat(task.dueDate()).isEqualTo("2026-06-10");
                    assertThat(task.priority()).isEqualTo("HIGH");
                    assertThat(task.blockers()).containsExactly("Plan");
                    assertThat(task.next()).containsExactly("Ship");
                    assertThat(task.note()).isEqualTo("Updated note");
                });

        assertThatThrownBy(() -> service.updateTaskById(999L, new UpdateTaskRequest(null, null, null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task not found.");
        assertThatThrownBy(() -> service.updateTaskById(31L, new UpdateTaskRequest(null, "Missing", null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task owner must be an existing team member.");
        assertThatThrownBy(() -> service.updateTaskById(31L, new UpdateTaskRequest(null, null, null, "2026-02-31", null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task due date must use a real yyyy-MM-dd date.");
    }

    @Test
    void updateStatusDeleteAndDependencyOperationsCoverSuccessAndFailures() {
        signIn("owner@example.com");
        var workspace = workspace(1L, "owner@example.com");
        addMember(workspace, 11L, "Owner", "owner@example.com", TeamRole.LEADER);
        addTask(workspace, 31L, "Design", "Owner", TaskStatus.TODO, "2026-06-01", List.of(), List.of(), "");
        addTask(workspace, 32L, "API", "Owner", TaskStatus.TODO, "2026-06-02", List.of("Design"), List.of(), "");
        addTask(workspace, 33L, "UI", "Owner", TaskStatus.TODO, "2026-06-03", List.of("API"), List.of(), "");
        when(workspaceRepository.findAccessibleInitializedByEmail("owner@example.com")).thenReturn(List.of(workspace));

        assertThat(service.updateTaskStatusById(31L, new UpdateTaskStatusRequest(TaskStatus.DONE)).tasks())
                .filteredOn(task -> task.id() == 31L)
                .singleElement()
                .satisfies(task -> assertThat(task.status()).isEqualTo(TaskStatus.DONE));
        assertThatThrownBy(() -> service.updateTaskStatusById(31L, new UpdateTaskStatusRequest(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task status is required.");

        assertThat(service.addTaskDependencyById(31L, new TaskDependencyRequest("Docs")).tasks())
                .filteredOn(task -> task.id() == 31L)
                .singleElement()
                .satisfies(task -> assertThat(task.blockers()).containsExactly("Docs"));
        service.addTaskDependencyById(31L, new TaskDependencyRequest("Docs"));
        assertThat(workspace.getTasks().stream().filter(task -> task.getId() == 31L).findFirst().orElseThrow().getBlockers())
                .containsExactly("Docs");
        assertThatThrownBy(() -> service.addTaskDependencyById(31L, new TaskDependencyRequest("Design")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task cannot depend on itself.");
        assertThatThrownBy(() -> service.addTaskDependencyById(32L, new TaskDependencyRequest("UI")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task dependency cycle is not allowed.");
        assertThatThrownBy(() -> service.addTaskDependencyById(31L, new TaskDependencyRequest(" ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dependency title is required.");

        assertThat(service.deleteTaskDependencyById(31L, "docs").tasks())
                .filteredOn(task -> task.id() == 31L)
                .singleElement()
                .satisfies(task -> assertThat(task.blockers()).isEmpty());
        assertThatThrownBy(() -> service.deleteTaskDependencyById(31L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dependency title is required.");

        service.deleteTaskById(33L);
        assertThat(workspace.getTasks()).extracting(MobileTaskEntity::getId).doesNotContain(33L);
        assertThatThrownBy(() -> service.deleteTaskById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task not found.");
    }

    @Test
    void createProjectMeetingCoversActionTaskBranchAndValidationFailures() {
        signIn("owner@example.com");
        var workspace = workspace(1L, "owner@example.com");
        addMember(workspace, 11L, "Owner", "owner@example.com", TeamRole.LEADER);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));

        var state = service.createProjectMeeting(1L, new CreateMeetingRequest(
                " Sprint plan ",
                "2026-06-03T10:30",
                " Plan work ",
                List.of("  Use API "),
                List.of("  Build task ", ""),
                "",
                true,
                "  Content ",
                Arrays.asList(11L, 0L, null),
                List.of(new MeetingActionItemView("  Follow up ", 11L, "2026-06-06"), new MeetingActionItemView("", 11L, ""))
        ));

        assertThat(state.meetings()).singleElement()
                .satisfies(meeting -> {
                    assertThat(meeting.title()).isEqualTo("Sprint plan");
                    assertThat(meeting.time()).isEqualTo("2026-06-03T10:30");
                    assertThat(meeting.decisions()).containsExactly("Use API");
                    assertThat(meeting.actions()).containsExactly("Build task");
                    assertThat(meeting.content()).isEqualTo("Content");
                    assertThat(meeting.attendeeIds()).containsExactly(11L);
                    assertThat(meeting.actionItems()).containsExactly(new MeetingActionItemView("Follow up", 11L, "2026-06-06"));
                });
        assertThat(state.tasks()).singleElement()
                .satisfies(task -> {
                    assertThat(task.title()).isEqualTo("Build task");
                    assertThat(task.owner()).isEqualTo("Owner");
                    assertThat(task.dueDate()).isEqualTo("2026-06-10");
                });

        assertThatThrownBy(() -> service.createProjectMeeting(1L, new CreateMeetingRequest("", "2026-06-03", "Agenda", List.of(), List.of(), null, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Meeting title is required.");
        assertThatThrownBy(() -> service.createProjectMeeting(1L, new CreateMeetingRequest("Meeting", "", "Agenda", List.of(), List.of(), null, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Meeting time is required.");
        assertThatThrownBy(() -> service.createProjectMeeting(1L, new CreateMeetingRequest("Meeting", "2026-02-31", "Agenda", List.of(), List.of(), null, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Meeting time must use a real yyyy-MM-dd or yyyy-MM-ddTHH:mm value.");
        assertThatThrownBy(() -> service.createProjectMeeting(1L, new CreateMeetingRequest("Meeting", "2026-06-03", "", List.of(), List.of(), null, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Meeting agenda is required.");
        assertThatThrownBy(() -> service.createProjectMeeting(1L, new CreateMeetingRequest("Meeting", "2026-06-03", "Agenda", List.of(), List.of("Task"), "Missing", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Action owner must be an existing team member.");
    }

    @Test
    void lookupWorkspaceByMeetingTaskAndReportIdsCoverFoundAndMissing() {
        signIn("owner@example.com");
        var workspace = workspace(1L, "owner@example.com");
        addMember(workspace, 11L, "Owner", "owner@example.com", TeamRole.LEADER);
        addTask(workspace, 31L, "Task", "Owner", TaskStatus.TODO, "2026-06-01", List.of(), List.of(), "");
        addMeeting(workspace, 41L, "Meeting");
        addReport(workspace, 61L, "READY");
        when(workspaceRepository.findAccessibleInitializedByEmail("owner@example.com")).thenReturn(List.of(workspace));

        assertThat(service.getProjectWorkspaceByTaskId(31L).projectId()).isEqualTo(1L);
        assertThat(service.getProjectWorkspaceByMeetingId(41L).projectId()).isEqualTo(1L);
        assertThat(service.getProjectWorkspaceByReportId(61L).projectId()).isEqualTo(1L);

        assertThatThrownBy(() -> service.getProjectWorkspaceByTaskId(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task not found.");
        assertThatThrownBy(() -> service.getProjectWorkspaceByMeetingId(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Meeting not found.");
        assertThatThrownBy(() -> service.getProjectWorkspaceByReportId(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Report not found.");
    }

    @Test
    void generateReportSetsStatusByWorkspaceContents() {
        signIn("owner@example.com");
        var empty = workspace(1L, "owner@example.com");
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(empty));
        assertThat(service.generateProjectReport(1L).reports()).singleElement()
                .satisfies(report -> assertThat(report.status()).isEqualTo("GENERATING"));

        var active = workspace(2L, "owner@example.com");
        addMember(active, 11L, "Owner", "owner@example.com", TeamRole.LEADER);
        addTask(active, 31L, "Task", "Owner", TaskStatus.TODO, "2026-06-01", List.of(), List.of(), "");
        when(workspaceRepository.findById(2L)).thenReturn(Optional.of(active));
        assertThat(service.generateProjectReport(2L).reports()).singleElement()
                .satisfies(report -> assertThat(report.status()).isEqualTo("READY"));
    }

    @Test
    void updateTeamAndInviteCodeCoverSuccessAndValidationFailures() {
        signIn("owner@example.com");
        var workspace = workspace(1L, "owner@example.com");
        addMember(workspace, 11L, "Owner", "owner@example.com", TeamRole.LEADER);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));

        var updated = service.updateProjectTeam(1L, new UpdateTeamRequest(
                " Team Updated ",
                " Course Updated ",
                "",
                "2026-07-01",
                " Description ",
                ""
        ));
        assertThat(updated.team().name()).isEqualTo("Team Updated");
        assertThat(updated.team().semester()).isEqualTo("2026-1");
        assertThat(updated.team().startDate()).isEqualTo("2026-06-01");

        var oldCode = updated.team().inviteCode();
        var regenerated = service.regenerateProjectInviteCode(1L);
        assertThat(regenerated.team().inviteCode()).isNotBlank().isNotEqualTo(oldCode);

        assertThatThrownBy(() -> service.updateProjectTeam(1L, new UpdateTeamRequest("", "Course", "2026-1", "2026-07-01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Team name is required.");
        assertThatThrownBy(() -> service.updateProjectTeam(1L, new UpdateTeamRequest("Team", "", "2026-1", "2026-07-01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Course name is required.");
        assertThatThrownBy(() -> service.updateProjectTeam(1L, new UpdateTeamRequest("Team", "Course", "2026-1", "2026-02-31")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Team due date must use a real yyyy-MM-dd date.");
        assertThatThrownBy(() -> service.updateProjectTeam(1L, new UpdateTeamRequest("Team", "Course", "2026-1", "2026-07-01", "", "2026-02-31")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Team start date must use a real yyyy-MM-dd date.");
    }

    @Test
    void invitationLookupAndAcceptCoverExistingLegacyNewAndInvalidBranches() {
        var workspace = workspace(1L, "owner@example.com");
        workspace.setInviteCode("ABC123");
        addMember(workspace, 11L, "Owner", "owner@example.com", TeamRole.LEADER);
        addMember(workspace, 12L, "Legacy", "", TeamRole.MEMBER);
        when(workspaceRepository.findFirstByInviteCodeIgnoreCaseAndInitializedTrueOrderByIdAsc("ABC123"))
                .thenReturn(Optional.of(workspace));
        when(workspaceRepository.findFirstByInviteCodeIgnoreCaseAndInitializedTrueOrderByIdAsc("MISSING"))
                .thenReturn(Optional.empty());

        assertThat(service.getWorkspaceByInviteCode("ABC123").team().name()).isEqualTo("TeamPulse");
        assertThatThrownBy(() -> service.getWorkspaceByInviteCode(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invitation code is required.");
        assertThatThrownBy(() -> service.getWorkspaceByInviteCode("MISSING"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invitation is invalid or expired.");

        var existing = service.acceptInvitation("ABC123", "Owner", "owner@example.com", TeamRole.LEADER);
        assertThat(existing.members()).hasSize(2);

        var legacy = service.acceptInvitation("ABC123", " Legacy ", "legacy@example.com", TeamRole.LEADER);
        assertThat(legacy.members()).filteredOn(member -> member.name().equals("Legacy"))
                .singleElement()
                .satisfies(member -> {
                    assertThat(member.email()).isEqualTo("legacy@example.com");
                    assertThat(member.role()).isEqualTo(TeamRole.MEMBER);
                });

        var joined = service.acceptInvitation("ABC123", " Newbie ", "Newbie@Example.com", null);
        assertThat(joined.members()).filteredOn(member -> member.name().equals("Newbie"))
                .singleElement()
                .satisfies(member -> {
                    assertThat(member.email()).isEqualTo("newbie@example.com");
                    assertThat(member.role()).isEqualTo(TeamRole.MEMBER);
                });

        assertThatThrownBy(() -> service.acceptInvitation("", "Name", "email@example.com", TeamRole.MEMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invitation code is required.");
        assertThatThrownBy(() -> service.acceptInvitation("ABC123", "", "email@example.com", TeamRole.MEMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Member name is required.");
        assertThatThrownBy(() -> service.acceptInvitation("ABC123", "Name", "", TeamRole.MEMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Member email is required.");
    }

    @Test
    void addAndDeleteMembersCoverDuplicateLastOpenTaskAndSuccessBranches() {
        var workspace = workspace(1L, "");
        addMember(workspace, 11L, "Owner", "", TeamRole.LEADER);
        when(workspaceRepository.findFirstByOwnerEmailIgnoreCaseOrderByIdAsc("")).thenReturn(Optional.of(workspace));

        assertThat(service.addMember(new CreateMemberRequest(" Member ", null)).members())
                .filteredOn(member -> member.name().equals("Member"))
                .singleElement()
                .satisfies(member -> assertThat(member.role()).isEqualTo(TeamRole.MEMBER));
        assertThatThrownBy(() -> service.addMember(new CreateMemberRequest("member", TeamRole.MEMBER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Member already exists.");
        assertThatThrownBy(() -> service.addMember(new CreateMemberRequest("", TeamRole.MEMBER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Member name is required.");

        addTask(workspace, 31L, "Open", "Member", TaskStatus.TODO, "2026-06-01", List.of(), List.of(), "");
        assertThatThrownBy(() -> service.deleteMember(workspace.getMembers().stream()
                .filter(member -> member.getName().equals("Member"))
                .findFirst()
                .orElseThrow()
                .getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reassign open tasks before removing this member.");

        workspace.getTasks().get(0).setStatus(TaskStatus.DONE);
        var memberId = workspace.getMembers().stream()
                .filter(member -> member.getName().equals("Member"))
                .findFirst()
                .orElseThrow()
                .getId();
        service.deleteMember(memberId);
        assertThat(workspace.getMembers()).extracting(MobileMemberEntity::getName).doesNotContain("Member");

        assertThatThrownBy(() -> service.deleteMember(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one member must remain.");
    }

    private MobileWorkspaceEntity persist(MobileWorkspaceEntity workspace) {
        if (workspace.getId() == null) {
            workspace.setId(workspaceIds.incrementAndGet());
        }
        workspace.getMembers().forEach(member -> {
            if (member.getId() == null) {
                member.setId(memberIds.incrementAndGet());
            }
        });
        workspace.getTasks().forEach(task -> {
            if (task.getId() == null) {
                task.setId(taskIds.incrementAndGet());
            }
        });
        workspace.getMeetings().forEach(meeting -> {
            if (meeting.getId() == null) {
                meeting.setId(meetingIds.incrementAndGet());
            }
        });
        workspace.getActivities().forEach(activity -> {
            if (activity.getId() == null) {
                activity.setId(activityIds.incrementAndGet());
            }
        });
        workspace.getReports().forEach(report -> {
            if (report.getId() == null) {
                report.setId(reportIds.incrementAndGet());
            }
        });
        return workspace;
    }

    private void signIn(String email) {
        var user = new AuthUser(1L, email, "hash", "User", "", "");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }

    private void signOut() {
        SecurityContextHolder.clearContext();
    }

    private MobileWorkspaceEntity workspace(long id, String ownerEmail) {
        var workspace = new MobileWorkspaceEntity();
        workspace.setId(id);
        workspace.setInitialized(true);
        workspace.setOwnerEmail(ownerEmail);
        workspace.setUserName("Owner");
        workspace.setUserEmail(ownerEmail);
        workspace.setTeamName("TeamPulse");
        workspace.setCourseName("Capstone");
        workspace.setSemester("2026-1");
        workspace.setDueDate("2026-06-30");
        workspace.setDescription("Description");
        workspace.setStartDate("2026-06-01");
        workspace.setInviteCode("INV123");
        return workspace;
    }

    private void addMember(MobileWorkspaceEntity workspace, Long id, String name, String email, TeamRole role) {
        var member = new MobileMemberEntity();
        member.setId(id);
        member.setWorkspace(workspace);
        member.setName(name);
        member.setEmail(email);
        member.setRole(role);
        workspace.getMembers().add(member);
    }

    private void addTask(
            MobileWorkspaceEntity workspace,
            Long id,
            String title,
            String owner,
            TaskStatus status,
            String dueDate,
            List<String> blockers,
            List<String> next,
            String note
    ) {
        var task = new MobileTaskEntity();
        task.setId(id);
        task.setWorkspace(workspace);
        task.setTitle(title);
        task.setOwner(owner);
        task.setStatus(status);
        task.setDueDate(dueDate);
        task.setPriority("MEDIUM");
        task.setBlockers(blockers);
        task.setNext(next);
        task.setNote(note);
        workspace.getTasks().add(task);
    }

    private void addMeeting(MobileWorkspaceEntity workspace, Long id, String title) {
        var meeting = new MobileMeetingEntity();
        meeting.setId(id);
        meeting.setWorkspace(workspace);
        meeting.setTitle(title);
        meeting.setTime("2026-06-01");
        meeting.setAgenda("Agenda");
        meeting.setContent("");
        meeting.setDecisions(List.of());
        meeting.setActions(List.of());
        meeting.setAttendeeIds(List.of());
        meeting.setActionItems(List.of());
        workspace.getMeetings().add(meeting);
    }

    private void addReport(MobileWorkspaceEntity workspace, Long id, String status) {
        var report = new MobileReportEntity();
        report.setId(id);
        report.setWorkspace(workspace);
        report.setLabel("Report");
        report.setRangeValue("2026-06-01 ~ 2026-06-30");
        report.setStatus(status);
        workspace.getReports().add(report);
    }
}
