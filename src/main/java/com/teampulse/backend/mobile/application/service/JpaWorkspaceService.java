package com.teampulse.backend.mobile.application.service;


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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("mysql")
@Transactional
public class JpaWorkspaceService implements WorkspaceService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
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
    public WorkspaceState reset() {
        var workspace = getOrCreateWorkspace();
        workspace.setInitialized(false);
        workspace.setUserName("");
        workspace.setUserEmail("");
        workspace.setTeamName("");
        workspace.setCourseName("");
        workspace.setSemester("2026-1");
        workspace.setDueDate("");
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

        var workspace = getOrCreateWorkspace();
        workspace.setInitialized(true);
        workspace.setUserName(request.name().trim());
        workspace.setUserEmail(request.email().trim());
        workspace.setTeamName(request.teamName().trim());
        workspace.setCourseName(request.courseName().trim());
        workspace.setSemester(defaultText(request.semester(), "2026-1"));
        workspace.setDueDate(request.dueDate().trim());
        workspace.setInviteCode(inviteCode());
        workspace.getMembers().clear();
        workspace.getTasks().clear();
        workspace.getMeetings().clear();
        workspace.getActivities().clear();
        workspace.getReports().clear();

        workspace.getMembers().add(member(workspace, request.name().trim(), TeamRole.LEADER));
        workspace.getActivities().add(activity(workspace, request.name().trim(), request.teamName().trim() + " workspace started."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState loadSample() {
        var workspace = getOrCreateWorkspace();
        workspace.setInitialized(true);
        workspace.setUserName("Kim");
        workspace.setUserEmail("leader@teampulse.app");
        workspace.setTeamName("TeamPulse");
        workspace.setCourseName("AI Coding Tool");
        workspace.setSemester("2026-1");
        workspace.setDueDate("2026-04-12");
        workspace.setInviteCode(inviteCode());
        workspace.getMembers().clear();
        workspace.getTasks().clear();
        workspace.getMeetings().clear();
        workspace.getActivities().clear();
        workspace.getReports().clear();

        workspace.getMembers().add(member(workspace, "Kim", TeamRole.LEADER));
        workspace.getMembers().add(member(workspace, "Min", TeamRole.MEMBER));
        workspace.getTasks().add(task(workspace, "Read AI coding tool guide", "Kim", "2026-04-08", List.of(), TaskStatus.DOING));
        workspace.getTasks().add(task(workspace, "Build mobile-first UI", "Min", "2026-04-09", List.of("Read AI coding tool guide"), TaskStatus.TODO));
        workspace.getTasks().add(task(workspace, "Prepare weekly report", "Kim", "2026-04-10", List.of(), TaskStatus.DONE));
        workspace.getMeetings().add(meeting(
                workspace,
                "Week 6 planning",
                "2026-04-07T19:00",
                "Finalize task split and report scope.",
                List.of("Use TeamPulse as assignment app."),
                List.of("Wire frontend to backend")));
        workspace.getActivities().add(activity(workspace, "System", "Sample workspace loaded."));
        workspace.getActivities().add(activity(workspace, "Kim", "Week 6 planning meeting saved."));
        workspace.getActivities().add(activity(workspace, "Kim", "Read AI coding tool guide task completed."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState createTask(CreateTaskRequest request) {
        var workspace = requireInitializedWorkspace();
        requireText(request.title(), "Task title is required.");
        requireText(request.owner(), "Task owner is required.");
        requireText(request.dueDate(), "Task due date is required.");
        validateLocalDate(request.dueDate(), "Task due date must use a real yyyy-MM-dd date.");
        requireExistingMember(workspace, request.owner(), "Task owner must be an existing team member.");

        workspace.getTasks().add(task(workspace, request.title().trim(), request.owner().trim(), request.dueDate().trim(), safeList(request.blockers()), TaskStatus.TODO));
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), request.title().trim() + " created."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState updateTaskStatus(long taskId, UpdateTaskStatusRequest request) {
        var workspace = requireInitializedWorkspace();
        if (request.status() == null) {
            throw new IllegalArgumentException("Task status is required.");
        }

        var target = workspace.getTasks().stream()
                .filter(task -> task.getId() != null && task.getId() == taskId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));

        target.setStatus(request.status());
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), target.getTitle() + " moved to " + request.status() + "."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState deleteTask(long taskId) {
        var workspace = requireInitializedWorkspace();
        var removed = workspace.getTasks().removeIf(task -> task.getId() != null && task.getId() == taskId);
        if (!removed) {
            throw new IllegalArgumentException("Task not found.");
        }

        workspace.getActivities().add(activity(workspace, workspace.getUserName(), "Task deleted."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState createMeeting(CreateMeetingRequest request) {
        var workspace = requireInitializedWorkspace();
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
                safeList(request.actions())));

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
        var workspace = requireInitializedWorkspace();
        workspace.getReports().add(report(workspace));
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), "Report draft refreshed."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState updateTeam(UpdateTeamRequest request) {
        var workspace = requireInitializedWorkspace();
        requireText(request.name(), "Team name is required.");
        requireText(request.courseName(), "Course name is required.");
        requireText(request.dueDate(), "Team due date is required.");
        validateLocalDate(request.dueDate(), "Team due date must use a real yyyy-MM-dd date.");

        workspace.setTeamName(request.name().trim());
        workspace.setCourseName(request.courseName().trim());
        workspace.setSemester(defaultText(request.semester(), workspace.getSemester()));
        workspace.setDueDate(request.dueDate().trim());
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), "Team profile updated."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState regenerateInviteCode() {
        var workspace = requireInitializedWorkspace();
        workspace.setInviteCode(inviteCode());
        workspace.getActivities().add(activity(workspace, workspace.getUserName(), "Invite code regenerated."));
        return persistAndProject(workspace);
    }

    @Override
    public WorkspaceState addMember(CreateMemberRequest request) {
        var workspace = requireInitializedWorkspace();
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
        var workspace = requireInitializedWorkspace();
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

    private MobileWorkspaceEntity requireInitializedWorkspace() {
        var workspace = getOrCreateWorkspace();
        if (!workspace.isInitialized()) {
            throw new IllegalArgumentException("Workspace is not initialized yet.");
        }
        return workspace;
    }

    private MobileWorkspaceEntity getOrCreateWorkspace() {
        return workspaceRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> workspaceRepository.saveAndFlush(emptyWorkspace()));
    }

    private WorkspaceState persistAndProject(MobileWorkspaceEntity workspace) {
        return toWorkspaceState(workspaceRepository.saveAndFlush(workspace));
    }

    private WorkspaceState toWorkspaceState(MobileWorkspaceEntity workspace) {
        var members = workspace.getMembers().stream()
                .sorted(Comparator.comparing(MobileMemberEntity::getId))
                .map(member -> new MemberView(member.getId(), member.getName(), member.getRole()))
                .toList();

        var tasks = workspace.getTasks().stream()
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

        var meetings = workspace.getMeetings().stream()
                .sorted(Comparator.comparing(MobileMeetingEntity::getId).reversed())
                .map(meeting -> new MeetingView(
                        meeting.getId(),
                        meeting.getTitle(),
                        meeting.getTime(),
                        meeting.getAgenda(),
                        safeList(meeting.getDecisions()),
                        safeList(meeting.getActions())))
                .toList();

        var activities = workspace.getActivities().stream()
                .sorted(Comparator.comparing(MobileActivityEntity::getId).reversed())
                .map(activity -> new ActivityView(activity.getId(), activity.getActor(), activity.getAt(), activity.getSummary()))
                .toList();

        var reports = workspace.getReports().stream()
                .sorted(Comparator.comparing(MobileReportEntity::getId).reversed())
                .map(report -> new ReportView(report.getId(), report.getLabel(), report.getRangeValue(), report.getStatus()))
                .toList();

        return new WorkspaceState(
                workspace.isInitialized(),
                new UserProfile(workspace.getUserName(), workspace.getUserEmail()),
                new TeamProfile(
                        workspace.getTeamName(),
                        workspace.getCourseName(),
                        workspace.getSemester(),
                        workspace.getDueDate(),
                        workspace.getInviteCode()),
                List.copyOf(members),
                List.copyOf(tasks),
                List.copyOf(meetings),
                List.copyOf(activities),
                List.copyOf(reports),
                List.copyOf(riskEngine.deriveRisks(tasks, meetings, members)));
    }

    private MobileWorkspaceEntity emptyWorkspace() {
        var workspace = new MobileWorkspaceEntity();
        workspace.setInitialized(false);
        workspace.setSemester("2026-1");
        workspace.setInviteCode(inviteCode());
        return workspace;
    }

    private MobileMemberEntity member(MobileWorkspaceEntity workspace, String name, TeamRole role) {
        var member = new MobileMemberEntity();
        member.setWorkspace(workspace);
        member.setName(name);
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
        var task = new MobileTaskEntity();
        task.setWorkspace(workspace);
        task.setTitle(title);
        task.setOwner(owner);
        task.setStatus(status);
        task.setDueDate(dueDate);
        task.setPriority(blockers.isEmpty() ? "MEDIUM" : "HIGH");
        task.setBlockers(List.copyOf(blockers));
        task.setNext(List.of());
        task.setNote("Created in assignment2 workspace flow.");
        return task;
    }

    private MobileMeetingEntity meeting(
            MobileWorkspaceEntity workspace,
            String title,
            String time,
            String agenda,
            List<String> decisions,
            List<String> actions
    ) {
        var meeting = new MobileMeetingEntity();
        meeting.setWorkspace(workspace);
        meeting.setTitle(title);
        meeting.setTime(time);
        meeting.setAgenda(agenda);
        meeting.setDecisions(List.copyOf(decisions));
        meeting.setActions(List.copyOf(actions));
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
