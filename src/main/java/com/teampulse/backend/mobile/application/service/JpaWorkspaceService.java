package com.teampulse.backend.mobile.application.service;


import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.mobile.application.WorkspaceService;
import com.teampulse.backend.mobile.dto.*;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.mobile.persistence.MobileActivityEntity;
import com.teampulse.backend.mobile.persistence.MobileMeetingEntity;
import com.teampulse.backend.mobile.persistence.MobileMemberEntity;
import com.teampulse.backend.mobile.persistence.MobileReportEntity;
import com.teampulse.backend.mobile.persistence.MobileTaskEntity;
import com.teampulse.backend.mobile.persistence.MobileWorkspaceEntity;
import com.teampulse.backend.mobile.persistence.MobileWorkspaceRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("mysql")
@Transactional
public class JpaWorkspaceService implements WorkspaceService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String ACTION_ITEM_SEPARATOR = "\u001F";
    private final MobileWorkspaceRepository workspaceRepository;
    private final RiskEngine riskEngine;

    public JpaWorkspaceService(MobileWorkspaceRepository workspaceRepository, RiskEngine riskEngine) {
        this.workspaceRepository = workspaceRepository;
        this.riskEngine = riskEngine;
    }

    @Override
    public WorkspaceState getWorkspace() {
        return toWorkspaceState(getOrCreateWorkspace());
    }

    @Override
    public List<WorkspaceState> getProjectWorkspaces() {
        return accessibleInitializedWorkspaces().stream()
                .map(this::toWorkspaceState)
                .toList();
    }

    @Override
    public WorkspaceState getProjectWorkspace(long projectId) {
        return toWorkspaceState(requireInitializedWorkspace(projectId));
    }

    @Override
    public WorkspaceState getProjectWorkspaceByTaskId(long taskId) {
        return toWorkspaceState(requireWorkspaceContainingTask(taskId));
    }

    @Override
    public WorkspaceState getProjectWorkspaceByMeetingId(long meetingId) {
        return toWorkspaceState(requireWorkspaceContainingMeeting(meetingId));
    }

    @Override
    public WorkspaceState getProjectWorkspaceByReportId(long reportId) {
        return toWorkspaceState(requireWorkspaceContainingReport(reportId));
    }

    @Override
    public WorkspaceState createProjectWorkspace(BootstrapWorkspaceRequest request) {
        requireText(request.name(), "Name is required.");
        requireText(request.email(), "Email is required.");
        requireText(request.teamName(), "Team name is required.");
        requireText(request.courseName(), "Course name is required.");
        requireText(request.dueDate(), "Due date is required.");
        validateLocalDate(request.dueDate(), "Due date must use a real yyyy-MM-dd date.");
        validateOptionalLocalDate(request.startDate(), "Start date must use a real yyyy-MM-dd date.");

        var ownerEmail = defaultText(currentOwnerEmail(), request.email().trim()).toLowerCase();
        var workspace = workspaceRepository.findFirstByOwnerEmailIgnoreCaseAndInitializedFalseOrderByIdAsc(ownerEmail)
                .orElseGet(() -> emptyWorkspace(ownerEmail));
        initializeWorkspace(workspace, request, ownerEmail);
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState resetProjectWorkspace(long projectId) {
        var workspace = requireInitializedWorkspace(projectId);
        clearWorkspace(workspace, currentOwnerEmail());
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState reset() {
        var workspace = getOrCreateWorkspace();
        workspace.setInitialized(false);
        workspace.setUserName("");
        workspace.setUserEmail("");
        workspace.setUserUniversity("");
        workspace.setUserPhone("");
        workspace.setOwnerEmail(currentOwnerEmail());
        workspace.setTeamName("");
        workspace.setCourseName("");
        workspace.setSemester("2026-1");
        workspace.setDueDate("");
        workspace.setDescription("");
        workspace.setStartDate("");
        workspace.setInviteCode(inviteCode());
        workspace.getMembers().clear();
        workspace.getTasks().clear();
        workspace.getMeetings().clear();
        workspace.getActivities().clear();
        workspace.getReports().clear();
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState bootstrap(BootstrapWorkspaceRequest request) {
        requireText(request.name(), "Name is required.");
        requireText(request.email(), "Email is required.");
        requireText(request.teamName(), "Team name is required.");
        requireText(request.courseName(), "Course name is required.");
        requireText(request.dueDate(), "Due date is required.");
        validateLocalDate(request.dueDate(), "Due date must use a real yyyy-MM-dd date.");
        validateOptionalLocalDate(request.startDate(), "Start date must use a real yyyy-MM-dd date.");

        var workspace = getOrCreateWorkspace();
        workspace.setOwnerEmail(defaultText(currentOwnerEmail(), request.email().trim()));
        workspace.setInitialized(true);
        workspace.setUserName(request.name().trim());
        workspace.setUserEmail(request.email().trim());
        workspace.setUserUniversity("");
        workspace.setUserPhone("");
        workspace.setTeamName(request.teamName().trim());
        workspace.setCourseName(request.courseName().trim());
        workspace.setSemester(defaultText(request.semester(), "2026-1"));
        workspace.setDueDate(request.dueDate().trim());
        workspace.setDescription(defaultText(request.description(), ""));
        workspace.setStartDate(defaultText(request.startDate(), ""));
        workspace.setInviteCode(inviteCode());
        workspace.getMembers().clear();
        workspace.getTasks().clear();
        workspace.getMeetings().clear();
        workspace.getActivities().clear();
        workspace.getReports().clear();

        workspace.getMembers().add(member(workspace, request.name().trim(), request.email().trim(), TeamRole.LEADER));
        workspace.getActivities().add(activity(workspace, request.name().trim(), request.teamName().trim() + " workspace started."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState loadSample() {
        var workspace = getOrCreateWorkspace();
        workspace.setOwnerEmail(defaultText(currentOwnerEmail(), "leader@teampulse.app"));
        workspace.setInitialized(true);
        workspace.setUserName("Kim");
        workspace.setUserEmail("leader@teampulse.app");
        workspace.setUserUniversity("Konkuk University");
        workspace.setUserPhone("010-0000-0000");
        workspace.setTeamName("TeamPulse");
        workspace.setCourseName("AI Coding Tool");
        workspace.setSemester("2026-1");
        workspace.setDueDate("2026-04-12");
        workspace.setDescription("TeamPulse demo project");
        workspace.setStartDate("2026-04-01");
        workspace.setInviteCode(inviteCode());
        workspace.getMembers().clear();
        workspace.getTasks().clear();
        workspace.getMeetings().clear();
        workspace.getActivities().clear();
        workspace.getReports().clear();

        workspace.getMembers().add(member(workspace, "Kim", "leader@teampulse.app", TeamRole.LEADER));
        workspace.getMembers().add(member(workspace, "Min", "", TeamRole.MEMBER));
        workspace.getTasks().add(task(workspace, "Read AI coding tool guide", "Kim", "2026-04-08", List.of(), TaskStatus.DOING));
        workspace.getTasks().add(task(workspace, "Build mobile-first UI", "Min", "2026-04-09", List.of("Read AI coding tool guide"), TaskStatus.TODO));
        workspace.getTasks().add(task(workspace, "Prepare weekly report", "Kim", "2026-04-10", List.of(), TaskStatus.DONE));
        workspace.getMeetings().add(meeting(
                workspace,
                "Week 6 planning",
                "2026-04-07T19:00",
                "Finalize task split and report scope.",
                List.of("Use TeamPulse as assignment app."),
                List.of("Wire frontend to backend"),
                "",
                List.of(),
                List.of()));
        workspace.getActivities().add(activity(workspace, "System", "Sample workspace loaded."));
        workspace.getActivities().add(activity(workspace, "Kim", "Week 6 planning meeting saved."));
        workspace.getActivities().add(activity(workspace, "Kim", "Read AI coding tool guide task completed."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState createTask(CreateTaskRequest request) {
        return createTaskInWorkspace(requireInitializedWorkspace(), request);
    }

    @Override
    public WorkspaceState createProjectTask(long projectId, CreateTaskRequest request) {
        return createTaskInWorkspace(requireInitializedWorkspace(projectId), request);
    }

    private WorkspaceState createTaskInWorkspace(MobileWorkspaceEntity workspace, CreateTaskRequest request) {
        requireText(request.title(), "Task title is required.");
        requireText(request.owner(), "Task owner is required.");
        requireText(request.dueDate(), "Task due date is required.");
        validateLocalDate(request.dueDate(), "Task due date must use a real yyyy-MM-dd date.");
        requireExistingMember(workspace, request.owner(), "Task owner must be an existing team member.");

        workspace.getTasks().add(task(
                workspace,
                request.title().trim(),
                request.owner().trim(),
                request.dueDate().trim(),
                safeList(request.blockers()),
                TaskStatus.TODO,
                defaultText(request.note(), "Created in assignment2 workspace flow.")));
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), request.title().trim() + " created."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState updateTask(long taskId, UpdateTaskRequest request) {
        return updateTaskInWorkspace(requireInitializedWorkspace(), taskId, request);
    }

    @Override
    public WorkspaceState updateTaskById(long taskId, UpdateTaskRequest request) {
        return updateTaskInWorkspace(requireWorkspaceContainingTask(taskId), taskId, request);
    }

    private WorkspaceState updateTaskInWorkspace(MobileWorkspaceEntity workspace, long taskId, UpdateTaskRequest request) {
        var target = findTask(workspace, taskId);

        if (request.title() != null && !request.title().isBlank()) {
            target.setTitle(request.title().trim());
        }
        if (request.owner() != null && !request.owner().isBlank()) {
            requireExistingMember(workspace, request.owner(), "Task owner must be an existing team member.");
            target.setOwner(request.owner().trim());
        }
        if (request.status() != null) {
            target.setStatus(request.status());
        }
        if (request.dueDate() != null && !request.dueDate().isBlank()) {
            validateLocalDate(request.dueDate(), "Task due date must use a real yyyy-MM-dd date.");
            target.setDueDate(request.dueDate().trim());
        }
        if (request.priority() != null && !request.priority().isBlank()) {
            target.setPriority(request.priority().trim());
        }
        if (request.blockers() != null) {
            target.setBlockers(safeList(request.blockers()));
        }
        if (request.next() != null) {
            target.setNext(safeList(request.next()));
        }
        if (request.note() != null) {
            target.setNote(request.note().trim());
        }

        workspace.getActivities().add(activity(workspace, workspace.getUserName(), target.getTitle() + " updated."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState updateTaskStatus(long taskId, UpdateTaskStatusRequest request) {
        return updateTaskStatusInWorkspace(requireInitializedWorkspace(), taskId, request);
    }

    @Override
    public WorkspaceState updateTaskStatusById(long taskId, UpdateTaskStatusRequest request) {
        return updateTaskStatusInWorkspace(requireWorkspaceContainingTask(taskId), taskId, request);
    }

    private WorkspaceState updateTaskStatusInWorkspace(MobileWorkspaceEntity workspace, long taskId, UpdateTaskStatusRequest request) {
        if (request.status() == null) {
            throw new IllegalArgumentException("Task status is required.");
        }

        var target = findTask(workspace, taskId);

        target.setStatus(request.status());
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), target.getTitle() + " moved to " + request.status() + "."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState deleteTask(long taskId) {
        return deleteTaskInWorkspace(requireInitializedWorkspace(), taskId);
    }

    @Override
    public WorkspaceState deleteTaskById(long taskId) {
        return deleteTaskInWorkspace(requireWorkspaceContainingTask(taskId), taskId);
    }

    private WorkspaceState deleteTaskInWorkspace(MobileWorkspaceEntity workspace, long taskId) {
        var removed = workspace.getTasks().removeIf(task -> task.getId() != null && task.getId() == taskId);
        if (!removed) {
            throw new IllegalArgumentException("Task not found.");
        }

        workspace.getActivities().add(activity(workspace, workspace.getUserName(), "Task deleted."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState addTaskDependency(long taskId, TaskDependencyRequest request) {
        return addTaskDependencyInWorkspace(requireInitializedWorkspace(), taskId, request);
    }

    @Override
    public WorkspaceState addTaskDependencyById(long taskId, TaskDependencyRequest request) {
        return addTaskDependencyInWorkspace(requireWorkspaceContainingTask(taskId), taskId, request);
    }

    private WorkspaceState addTaskDependencyInWorkspace(MobileWorkspaceEntity workspace, long taskId, TaskDependencyRequest request) {
        requireText(request.title(), "Dependency title is required.");
        var target = findTask(workspace, taskId);
        var dependency = request.title().trim();
        rejectCyclicDependency(workspace, target, dependency);
        var blockers = new ArrayList<>(safeList(target.getBlockers()));
        if (!blockers.contains(dependency)) {
            blockers.add(dependency);
        }
        target.setBlockers(blockers);
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), "Dependency added to " + target.getTitle() + "."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState deleteTaskDependency(long taskId, String dependencyTitle) {
        return deleteTaskDependencyInWorkspace(requireInitializedWorkspace(), taskId, dependencyTitle);
    }

    @Override
    public WorkspaceState deleteTaskDependencyById(long taskId, String dependencyTitle) {
        return deleteTaskDependencyInWorkspace(requireWorkspaceContainingTask(taskId), taskId, dependencyTitle);
    }

    private WorkspaceState deleteTaskDependencyInWorkspace(MobileWorkspaceEntity workspace, long taskId, String dependencyTitle) {
        requireText(dependencyTitle, "Dependency title is required.");
        var target = findTask(workspace, taskId);
        var blockers = new ArrayList<>(safeList(target.getBlockers()));
        blockers.removeIf(blocker -> blocker.equalsIgnoreCase(dependencyTitle.trim()));
        target.setBlockers(blockers);
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), "Dependency removed from " + target.getTitle() + "."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState createMeeting(CreateMeetingRequest request) {
        return createMeetingInWorkspace(requireInitializedWorkspace(), request);
    }

    @Override
    public WorkspaceState createProjectMeeting(long projectId, CreateMeetingRequest request) {
        return createMeetingInWorkspace(requireInitializedWorkspace(projectId), request);
    }

    private WorkspaceState createMeetingInWorkspace(MobileWorkspaceEntity workspace, CreateMeetingRequest request) {
        requireText(request.title(), "Meeting title is required.");
        requireText(request.time(), "Meeting time is required.");
        requireText(request.agenda(), "Meeting agenda is required.");
        validateMeetingTime(request.time(), "Meeting time must use a real yyyy-MM-dd or yyyy-MM-ddTHH:mm value.");

        workspace.getMeetings().add(meeting(
                workspace,
                request.title().trim(),
                request.time().trim(),
                request.agenda().trim(),
                safeList(request.decisions()),
                safeList(request.actions()),
                defaultText(request.content(), ""),
                safeLongList(request.attendeeIds()),
                safeActionItems(request.actionItems())));

        if (request.createTasks()) {
            var actionOwner = defaultText(request.actionOwner(), firstMemberName(workspace));
            requireExistingMember(workspace, actionOwner, "Action owner must be an existing team member.");
            for (var action : safeList(request.actions())) {
                workspace.getTasks().add(task(workspace, action, actionOwner, plusDays(request.time(), 7), List.of(), TaskStatus.TODO));
            }
        }

        workspace.getActivities().add(activity(workspace, workspace.getUserName(), request.title().trim() + " meeting saved."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState generateReport() {
        return generateReportInWorkspace(requireInitializedWorkspace());
    }

    @Override
    public WorkspaceState generateProjectReport(long projectId) {
        return generateReportInWorkspace(requireInitializedWorkspace(projectId));
    }

    private WorkspaceState generateReportInWorkspace(MobileWorkspaceEntity workspace) {
        workspace.getReports().add(report(workspace));
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), "Report draft refreshed."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState updateTeam(UpdateTeamRequest request) {
        return updateTeamInWorkspace(requireInitializedWorkspace(), request);
    }

    @Override
    public WorkspaceState updateProjectTeam(long projectId, UpdateTeamRequest request) {
        return updateTeamInWorkspace(requireInitializedWorkspace(projectId), request);
    }

    private WorkspaceState updateTeamInWorkspace(MobileWorkspaceEntity workspace, UpdateTeamRequest request) {
        requireText(request.name(), "Team name is required.");
        requireText(request.courseName(), "Course name is required.");
        requireText(request.dueDate(), "Team due date is required.");
        validateLocalDate(request.dueDate(), "Team due date must use a real yyyy-MM-dd date.");
        validateOptionalLocalDate(request.startDate(), "Team start date must use a real yyyy-MM-dd date.");

        workspace.setTeamName(request.name().trim());
        workspace.setCourseName(request.courseName().trim());
        workspace.setSemester(defaultText(request.semester(), workspace.getSemester()));
        workspace.setDueDate(request.dueDate().trim());
        workspace.setDescription(defaultText(request.description(), workspace.getDescription()));
        workspace.setStartDate(defaultText(request.startDate(), workspace.getStartDate()));
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), "Team profile updated."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState regenerateInviteCode() {
        return regenerateInviteCodeInWorkspace(requireInitializedWorkspace());
    }

    @Override
    public WorkspaceState regenerateProjectInviteCode(long projectId) {
        return regenerateInviteCodeInWorkspace(requireInitializedWorkspace(projectId));
    }

    private WorkspaceState regenerateInviteCodeInWorkspace(MobileWorkspaceEntity workspace) {
        workspace.setInviteCode(inviteCode());
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), "Invite code regenerated."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState getWorkspaceByInviteCode(String inviteCode) {
        requireText(inviteCode, "Invitation code is required.");
        return toWorkspaceState(findWorkspaceByInviteCode(inviteCode));
    }

    @Override
    public WorkspaceState acceptInvitation(String inviteCode, String memberName, String memberEmail, TeamRole role) {
        requireText(inviteCode, "Invitation code is required.");
        requireText(memberName, "Member name is required.");
        requireText(memberEmail, "Member email is required.");
        var workspace = findWorkspaceByInviteCode(inviteCode);
        var normalizedEmail = memberEmail.trim().toLowerCase();
        var normalizedName = memberName.trim();

        var existingByEmail = workspace.getMembers().stream()
                .filter(member -> member.getEmail().equalsIgnoreCase(normalizedEmail))
                .findFirst();
        if (existingByEmail.isPresent()) {
            return toWorkspaceState(workspace);
        }

        var legacyMember = workspace.getMembers().stream()
                .filter(member -> member.getEmail().isBlank() && member.getName().equalsIgnoreCase(normalizedName))
                .findFirst();
        if (legacyMember.isPresent()) {
            legacyMember.get().setEmail(normalizedEmail);
            legacyMember.get().setRole(role == null ? TeamRole.MEMBER : role);
            workspace.getActivities().add(activity(workspace, workspace.getUserName(), normalizedName + " accepted invitation."));
            return persistAndProject(workspace);
        }

        workspace.getMembers().add(member(workspace, normalizedName, normalizedEmail, role == null ? TeamRole.MEMBER : role));
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), normalizedName + " accepted invitation."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState addMember(CreateMemberRequest request) {
        return addMemberInWorkspace(requireInitializedWorkspace(), request);
    }

    private WorkspaceState addMemberInWorkspace(MobileWorkspaceEntity workspace, CreateMemberRequest request) {
        requireText(request.name(), "Member name is required.");

        var normalizedName = request.name().trim();
        var duplicate = workspace.getMembers().stream().anyMatch(member -> member.getName().equalsIgnoreCase(normalizedName));
        if (duplicate) {
            throw new IllegalArgumentException("Member already exists.");
        }

        workspace.getMembers().add(member(workspace, normalizedName, request.role() == null ? TeamRole.MEMBER : request.role()));
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), normalizedName + " added to team."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState deleteMember(long memberId) {
        return deleteMemberInWorkspace(requireInitializedWorkspace(), memberId);
    }

    @Override
    public WorkspaceState deleteProjectMember(long projectId, long memberId) {
        return deleteMemberInWorkspace(requireInitializedWorkspace(projectId), memberId);
    }

    private WorkspaceState deleteMemberInWorkspace(MobileWorkspaceEntity workspace, long memberId) {
        if (workspace.getMembers().size() == 1) {
            throw new IllegalArgumentException("At least one member must remain.");
        }

        var target = workspace.getMembers().stream()
                .filter(member -> member.getId() != null && member.getId() == memberId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));

        var targetName = target.getName();
        var openTasks = workspace.getTasks().stream()
                .anyMatch(task -> task.getOwner().equals(targetName) && task.getStatus() != TaskStatus.DONE);
        if (openTasks) {
            throw new IllegalArgumentException("Reassign open tasks before removing this member.");
        }

        workspace.getMembers().remove(target);
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), targetName + " removed from team."));
        return persistAndProject(workspace);
    }

    private void initializeWorkspace(MobileWorkspaceEntity workspace, BootstrapWorkspaceRequest request, String ownerEmail) {
        workspace.setOwnerEmail(defaultText(ownerEmail, request.email().trim()).toLowerCase());
        workspace.setInitialized(true);
        workspace.setUserName(request.name().trim());
        workspace.setUserEmail(request.email().trim());
        workspace.setUserUniversity("");
        workspace.setUserPhone("");
        workspace.setTeamName(request.teamName().trim());
        workspace.setCourseName(request.courseName().trim());
        workspace.setSemester(defaultText(request.semester(), "2026-1"));
        workspace.setDueDate(request.dueDate().trim());
        workspace.setDescription(defaultText(request.description(), ""));
        workspace.setStartDate(defaultText(request.startDate(), ""));
        workspace.setInviteCode(inviteCode());
        workspace.getMembers().clear();
        workspace.getTasks().clear();
        workspace.getMeetings().clear();
        workspace.getActivities().clear();
        workspace.getReports().clear();
        workspace.getMembers().add(member(workspace, request.name().trim(), request.email().trim(), TeamRole.LEADER));
        workspace.getActivities().add(activity(workspace, request.name().trim(), request.teamName().trim() + " workspace started."));
    }

    private void clearWorkspace(MobileWorkspaceEntity workspace, String ownerEmail) {
        workspace.setInitialized(false);
        workspace.setUserName("");
        workspace.setUserEmail(defaultText(ownerEmail, ""));
        workspace.setUserUniversity("");
        workspace.setUserPhone("");
        workspace.setOwnerEmail(defaultText(ownerEmail, ""));
        workspace.setTeamName("");
        workspace.setCourseName("");
        workspace.setSemester("2026-1");
        workspace.setDueDate("");
        workspace.setDescription("");
        workspace.setStartDate("");
        workspace.setInviteCode(inviteCode());
        workspace.getMembers().clear();
        workspace.getTasks().clear();
        workspace.getMeetings().clear();
        workspace.getActivities().clear();
        workspace.getReports().clear();
    }

    private MobileWorkspaceEntity requireInitializedWorkspace() {
        var workspace = getOrCreateWorkspace();
        if (!workspace.isInitialized()) {
            throw new IllegalArgumentException("Workspace is not initialized yet.");
        }
        return workspace;
    }

    private MobileWorkspaceEntity requireInitializedWorkspace(long projectId) {
        var workspace = workspaceRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found."));
        if (!workspace.isInitialized()) {
            throw new IllegalArgumentException("Project not found.");
        }
        requireWorkspaceAccess(workspace);
        return workspace;
    }

    private List<MobileWorkspaceEntity> accessibleInitializedWorkspaces() {
        var ownerEmail = currentOwnerEmail();
        if (ownerEmail.isBlank()) {
            return workspaceRepository.findFirstByOwnerEmailIgnoreCaseOrderByIdAsc("")
                    .filter(MobileWorkspaceEntity::isInitialized)
                    .stream()
                    .toList();
        }
        return workspaceRepository.findAccessibleInitializedByEmail(ownerEmail);
    }

    private void requireWorkspaceAccess(MobileWorkspaceEntity workspace) {
        var ownerEmail = currentOwnerEmail();
        if (ownerEmail.isBlank()) {
            if (!workspace.getOwnerEmail().isBlank()) {
                throw new IllegalArgumentException("Authentication user is required.");
            }
            return;
        }
        var accessible = workspace.getOwnerEmail().equalsIgnoreCase(ownerEmail)
                || workspace.getMembers().stream().anyMatch(member -> member.getEmail().equalsIgnoreCase(ownerEmail));
        if (!accessible) {
            throw new IllegalArgumentException("Current user is not a project member.");
        }
    }

    private MobileWorkspaceEntity requireWorkspaceContainingTask(long taskId) {
        return accessibleInitializedWorkspaces().stream()
                .filter(workspace -> workspace.getTasks().stream().anyMatch(task -> task.getId() != null && task.getId() == taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
    }

    private MobileWorkspaceEntity requireWorkspaceContainingMeeting(long meetingId) {
        return accessibleInitializedWorkspaces().stream()
                .filter(workspace -> workspace.getMeetings().stream().anyMatch(meeting -> meeting.getId() != null && meeting.getId() == meetingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found."));
    }

    private MobileWorkspaceEntity requireWorkspaceContainingReport(long reportId) {
        return accessibleInitializedWorkspaces().stream()
                .filter(workspace -> workspace.getReports().stream().anyMatch(report -> report.getId() != null && report.getId() == reportId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Report not found."));
    }

    private MobileTaskEntity findTask(MobileWorkspaceEntity workspace, long taskId) {
        return workspace.getTasks().stream()
                .filter(task -> task.getId() != null && task.getId() == taskId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
    }

    private void rejectCyclicDependency(MobileWorkspaceEntity workspace, MobileTaskEntity target, String dependencyTitle) {
        if (target.getTitle().equalsIgnoreCase(dependencyTitle)) {
            throw new IllegalArgumentException("Task cannot depend on itself.");
        }
        var dependency = workspace.getTasks().stream()
                .filter(task -> task.getTitle().equalsIgnoreCase(dependencyTitle))
                .findFirst();
        if (dependency.isPresent() && dependsOn(workspace, dependency.get(), target.getTitle(), new HashSet<>())) {
            throw new IllegalArgumentException("Task dependency cycle is not allowed.");
        }
    }

    private boolean dependsOn(MobileWorkspaceEntity workspace, MobileTaskEntity source, String targetTitle, Set<String> visitedTitles) {
        if (!visitedTitles.add(source.getTitle().toLowerCase())) {
            return false;
        }
        for (var blocker : safeList(source.getBlockers())) {
            if (blocker.equalsIgnoreCase(targetTitle)) {
                return true;
            }
            var blockerTask = workspace.getTasks().stream()
                    .filter(task -> task.getTitle().equalsIgnoreCase(blocker))
                    .findFirst();
            if (blockerTask.isPresent() && dependsOn(workspace, blockerTask.get(), targetTitle, visitedTitles)) {
                return true;
            }
        }
        return false;
    }

    private MobileWorkspaceEntity findWorkspaceByInviteCode(String inviteCode) {
        return workspaceRepository.findFirstByInviteCodeIgnoreCaseAndInitializedTrueOrderByIdAsc(inviteCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invitation is invalid or expired."));
    }

    private MobileWorkspaceEntity getOrCreateWorkspace() {
        var ownerEmail = currentOwnerEmail();
        if (!ownerEmail.isBlank()) {
            var ownerWorkspace = workspaceRepository.findFirstByOwnerEmailIgnoreCaseOrderByIdAsc(ownerEmail);
            if (ownerWorkspace.isPresent() && ownerWorkspace.get().isInitialized()) {
                return ownerWorkspace.get();
            }
            var invitedWorkspace = workspaceRepository.findInitializedByMemberEmail(ownerEmail).stream()
                    .findFirst();
            if (invitedWorkspace.isPresent()) {
                return invitedWorkspace.get();
            }
            return ownerWorkspace.orElseGet(() -> workspaceRepository.saveAndFlush(emptyWorkspace(ownerEmail)));
        }
        return workspaceRepository.findFirstByOwnerEmailIgnoreCaseOrderByIdAsc("")
                .orElseGet(() -> workspaceRepository.saveAndFlush(emptyWorkspace("")));
    }

    private WorkspaceState persistAndProject(MobileWorkspaceEntity workspace) {
        return toWorkspaceState(workspaceRepository.saveAndFlush(workspace));
    }

    private WorkspaceState toWorkspaceState(MobileWorkspaceEntity workspace) {
        var members = memberViews(workspace);
        var tasks = taskViews(workspace);
        var meetings = meetingViews(workspace);
        var activities = activityViews(workspace);
        var reports = reportViews(workspace);

        return new WorkspaceState(
                workspace.getId() == null ? 1L : workspace.getId(),
                workspace.isInitialized(),
                new UserProfile(
                        workspace.getUserName(),
                        workspace.getUserEmail(),
                        workspace.getUserUniversity(),
                        workspace.getUserPhone()),
                new TeamProfile(
                        workspace.getTeamName(),
                        workspace.getCourseName(),
                        workspace.getSemester(),
                        workspace.getDueDate(),
                        workspace.getInviteCode(),
                        workspace.getDescription(),
                        workspace.getStartDate()),
                List.copyOf(members),
                List.copyOf(tasks),
                List.copyOf(meetings),
                List.copyOf(activities),
                List.copyOf(reports),
                List.copyOf(riskEngine.deriveRisks(tasks, meetings, members)));
    }

    private List<MemberView> memberViews(MobileWorkspaceEntity workspace) {
        return workspace.getMembers().stream()
                .sorted(Comparator.comparing(MobileMemberEntity::getId))
                .map(member -> new MemberView(member.getId(), member.getName(), member.getEmail(), member.getRole()))
                .toList();
    }

    private List<TaskView> taskViews(MobileWorkspaceEntity workspace) {
        return workspace.getTasks().stream()
                .map(task -> new TaskView(
                        task.getId(),
                        task.getTitle(),
                        task.getOwner(),
                        task.getStatus(),
                        task.getDueDate(),
                        task.getPriority(),
                        safeList(task.getBlockers()),
                        safeList(task.getNext()),
                        task.getNote()))
                .sorted(Comparator.comparing(TaskView::status).thenComparing(TaskView::dueDate))
                .toList();
    }

    private List<MeetingView> meetingViews(MobileWorkspaceEntity workspace) {
        return workspace.getMeetings().stream()
                .sorted(Comparator.comparing(MobileMeetingEntity::getId).reversed())
                .map(meeting -> new MeetingView(
                        meeting.getId(),
                        meeting.getTitle(),
                        meeting.getTime(),
                        meeting.getAgenda(),
                        defaultText(meeting.getContent(), ""),
                        safeList(meeting.getDecisions()),
                        safeList(meeting.getActions()),
                        parseLongList(meeting.getAttendeeIds()),
                        decodeActionItems(meeting.getActionItems()),
                        defaultText(meeting.getCreatedAt(), meeting.getTime()),
                        defaultText(meeting.getUpdatedAt(), defaultText(meeting.getCreatedAt(), meeting.getTime()))))
                .toList();
    }

    private List<ActivityView> activityViews(MobileWorkspaceEntity workspace) {
        return workspace.getActivities().stream()
                .sorted(Comparator.comparing(MobileActivityEntity::getId).reversed())
                .map(activity -> new ActivityView(activity.getId(), activity.getActor(), activity.getAt(), activity.getSummary()))
                .toList();
    }

    private List<ReportView> reportViews(MobileWorkspaceEntity workspace) {
        return workspace.getReports().stream()
                .sorted(Comparator.comparing(MobileReportEntity::getId).reversed())
                .map(report -> new ReportView(report.getId(), report.getLabel(), report.getRangeValue(), report.getStatus()))
                .toList();
    }

    private MobileWorkspaceEntity emptyWorkspace() {
        var workspace = new MobileWorkspaceEntity();
        workspace.setInitialized(false);
        workspace.setSemester("2026-1");
        workspace.setInviteCode(inviteCode());
        return workspace;
    }

    private MobileWorkspaceEntity emptyWorkspace(String ownerEmail) {
        var workspace = emptyWorkspace();
        workspace.setOwnerEmail(ownerEmail);
        workspace.setUserEmail(ownerEmail);
        return workspace;
    }

    private String currentOwnerEmail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser) {
            return defaultText(authUser.email(), "").toLowerCase();
        }
        return "";
    }

    private MobileMemberEntity member(MobileWorkspaceEntity workspace, String name, TeamRole role) {
        return member(workspace, name, "", role);
    }

    private MobileMemberEntity member(MobileWorkspaceEntity workspace, String name, String email, TeamRole role) {
        var member = new MobileMemberEntity();
        member.setWorkspace(workspace);
        member.setName(name);
        member.setEmail(defaultText(email, "").toLowerCase());
        member.setRole(role);
        return member;
    }

    private MobileTaskEntity task(
            MobileWorkspaceEntity workspace,
            String title,
            String owner,
            String dueDate,
            List<String> blockers,
            TaskStatus status
    ) {
        return task(workspace, title, owner, dueDate, blockers, status, "Created in assignment2 workspace flow.");
    }

    private MobileTaskEntity task(
            MobileWorkspaceEntity workspace,
            String title,
            String owner,
            String dueDate,
            List<String> blockers,
            TaskStatus status,
            String note
    ) {
        var task = new MobileTaskEntity();
        task.setWorkspace(workspace);
        task.setTitle(title);
        task.setOwner(owner);
        task.setStatus(status);
        task.setDueDate(dueDate);
        task.setPriority(blockers.isEmpty() ? "MEDIUM" : "HIGH");
        task.setBlockers(List.copyOf(blockers));
        task.setNext(List.of());
        task.setNote(note);
        return task;
    }

    private MobileMeetingEntity meeting(
            MobileWorkspaceEntity workspace,
            String title,
            String time,
            String agenda,
            List<String> decisions,
            List<String> actions,
            String content,
            List<Long> attendeeIds,
            List<MeetingActionItemView> actionItems
    ) {
        var meeting = new MobileMeetingEntity();
        meeting.setWorkspace(workspace);
        meeting.setTitle(title);
        meeting.setTime(time);
        meeting.setAgenda(agenda);
        meeting.setContent(defaultText(content, ""));
        meeting.setDecisions(List.copyOf(decisions));
        meeting.setActions(List.copyOf(actions));
        meeting.setAttendeeIds(attendeeIds.stream().map(String::valueOf).toList());
        meeting.setActionItems(encodeActionItems(actionItems));
        var now = LocalDateTime.now().withNano(0).toString();
        meeting.setCreatedAt(now);
        meeting.setUpdatedAt(now);
        return meeting;
    }

    private MobileActivityEntity activity(MobileWorkspaceEntity workspace, String actor, String summary) {
        var activity = new MobileActivityEntity();
        activity.setWorkspace(workspace);
        activity.setActor(actor);
        activity.setAt(LocalDateTime.now().format(TIMESTAMP_FORMAT));
        activity.setSummary(summary);
        return activity;
    }

    private MobileReportEntity report(MobileWorkspaceEntity workspace) {
        var report = new MobileReportEntity();
        report.setWorkspace(workspace);
        report.setLabel("TeamPulse report draft");
        var start = LocalDate.now().withDayOfMonth(1);
        report.setRangeValue(start + " ~ " + LocalDate.now());
        report.setStatus(workspace.getTasks().isEmpty() && workspace.getMeetings().isEmpty() ? "GENERATING" : "READY");
        return report;
    }

    private List<String> safeList(List<String> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream().filter(item -> item != null && !item.isBlank()).map(String::trim).toList();
    }

    private List<Long> safeLongList(List<Long> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream().filter(item -> item != null && item > 0).toList();
    }

    private List<MeetingActionItemView> safeActionItems(List<MeetingActionItemView> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream()
                .filter(item -> item != null && item.content() != null && !item.content().isBlank())
                .map(item -> new MeetingActionItemView(item.content().trim(), item.assigneeId(), defaultText(item.dueDate(), "")))
                .toList();
    }

    private List<String> encodeActionItems(List<MeetingActionItemView> source) {
        return safeActionItems(source).stream()
                .map(item -> String.join(
                        ACTION_ITEM_SEPARATOR,
                        item.content(),
                        item.assigneeId() == null ? "" : String.valueOf(item.assigneeId()),
                        defaultText(item.dueDate(), "")))
                .toList();
    }

    private List<MeetingActionItemView> decodeActionItems(List<String> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(item -> item.split(ACTION_ITEM_SEPARATOR, -1))
                .map(parts -> new MeetingActionItemView(
                        parts.length > 0 ? parts[0] : "",
                        parts.length > 1 && !parts[1].isBlank() ? Long.valueOf(parts[1]) : null,
                        parts.length > 2 ? parts[2] : ""))
                .toList();
    }

    private List<Long> parseLongList(List<String> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(Long::valueOf)
                .toList();
    }

    private String plusDays(String value, int days) {
        if (value == null || value.isBlank()) {
            return "";
        }
        var normalized = value.contains("T") ? value.substring(0, 10) : value;
        return parseLocalDate(normalized, "Meeting time must use a real yyyy-MM-dd or yyyy-MM-ddTHH:mm value.").plusDays(days).toString();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireExistingMember(MobileWorkspaceEntity workspace, String name, String message) {
        var normalized = name == null ? "" : name.trim();
        var exists = workspace.getMembers().stream().anyMatch(member -> member.getName().equalsIgnoreCase(normalized));
        if (!exists) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateLocalDate(String value, String message) {
        parseLocalDate(value, message);
    }

    private void validateOptionalLocalDate(String value, String message) {
        if (value == null || value.isBlank()) {
            return;
        }
        validateLocalDate(value, message);
    }

    private void validateMeetingTime(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        if (value.contains("T")) {
            parseLocalDate(value.substring(0, 10), message);
            return;
        }
        parseLocalDate(value, message);
    }

    private LocalDate parseLocalDate(String value, String message) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(message);
        }
    }

    private String firstMemberName(MobileWorkspaceEntity workspace) {
        return workspace.getMembers().isEmpty() ? workspace.getUserName() : workspace.getMembers().get(0).getName();
    }

    private String inviteCode() {
        var alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        var builder = new StringBuilder();
        for (int index = 0; index < 6; index++) {
            builder.append(alphabet.charAt(ThreadLocalRandom.current().nextInt(alphabet.length())));
        }
        return builder.toString();
    }

}
