package com.teampulse.backend.mobile.application.service;


import com.teampulse.backend.mobile.application.WorkspaceService;
import com.teampulse.backend.mobile.dto.*;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.domain.team.TeamRole;
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
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("demo")
public class InMemoryWorkspaceService implements WorkspaceService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final AtomicLong ids = new AtomicLong(1000);
    private final RiskEngine riskEngine;
    private WorkspaceState workspace = emptyWorkspace();

    public InMemoryWorkspaceService(RiskEngine riskEngine) {
        this.riskEngine = riskEngine;
    }

    public synchronized WorkspaceState getWorkspace() {
        return workspace;
    }

    @Override
    public synchronized List<WorkspaceState> getProjectWorkspaces() {
        return workspace.initialized() ? List.of(workspace) : List.of();
    }

    @Override
    public synchronized WorkspaceState getProjectWorkspace(long projectId) {
        requireProjectId(projectId);
        ensureInitialized();
        return workspace;
    }

    @Override
    public synchronized WorkspaceState getProjectWorkspaceByTaskId(long taskId) {
        requireTask(taskId);
        return workspace;
    }

    @Override
    public synchronized WorkspaceState getProjectWorkspaceByMeetingId(long meetingId) {
        requireMeeting(meetingId);
        return workspace;
    }

    @Override
    public synchronized WorkspaceState getProjectWorkspaceByReportId(long reportId) {
        requireReport(reportId);
        return workspace;
    }

    @Override
    public synchronized WorkspaceState createProjectWorkspace(BootstrapWorkspaceRequest request) {
        return bootstrap(request);
    }

    @Override
    public synchronized WorkspaceState resetProjectWorkspace(long projectId) {
        requireProjectId(projectId);
        return reset();
    }

    public synchronized WorkspaceState reset() {
        workspace = emptyWorkspace();
        return workspace;
    }

    public synchronized WorkspaceState bootstrap(BootstrapWorkspaceRequest request) {
        requireText(request.name(), "Name is required.");
        requireText(request.email(), "Email is required.");
        requireText(request.teamName(), "Team name is required.");
        requireText(request.courseName(), "Course name is required.");
        requireText(request.dueDate(), "Due date is required.");
        validateLocalDate(request.dueDate(), "Due date must use a real yyyy-MM-dd date.");
        validateOptionalLocalDate(request.startDate(), "Start date must use a real yyyy-MM-dd date.");

        var leader = new MemberView(nextId(), request.name().trim(), TeamRole.LEADER);
        var members = new ArrayList<>(List.of(leader));
        var activities = new ArrayList<>(List.of(activity(request.name().trim(), request.teamName().trim() + " workspace started.")));

        workspace = new WorkspaceState(
                true,
                new UserProfile(request.name().trim(), request.email().trim()),
                new TeamProfile(
                        request.teamName().trim(),
                        request.courseName().trim(),
                        defaultText(request.semester(), "2026-1"),
                        request.dueDate().trim(),
                        inviteCode(),
                        defaultText(request.description(), ""),
                        defaultText(request.startDate(), "")),
                members,
                new ArrayList<>(),
                new ArrayList<>(),
                activities,
                new ArrayList<>(),
                new ArrayList<>());

        return recompute();
    }

    public synchronized WorkspaceState loadSample() {
        var leader = new MemberView(nextId(), "Kim", TeamRole.LEADER);
        var member = new MemberView(nextId(), "Min", TeamRole.MEMBER);
        var user = new UserProfile("Kim", "leader@teampulse.app");
        var team = new TeamProfile("TeamPulse", "AI Coding Tool", "2026-1", "2026-04-12", inviteCode());

        var tasks = new ArrayList<>(List.of(
                task("Read AI coding tool guide", "Kim", "2026-04-08", List.of(), TaskStatus.DOING),
                task("Build mobile-first UI", "Min", "2026-04-09", List.of("Read AI coding tool guide"), TaskStatus.TODO),
                task("Prepare weekly report", "Kim", "2026-04-10", List.of(), TaskStatus.DONE)));

        var meetings = new ArrayList<>(List.of(
                new MeetingView(nextId(), "Week 6 planning", "2026-04-07T19:00", "Finalize task split and report scope.", List.of("Use TeamPulse as assignment app."), List.of("Wire frontend to backend"))));

        var activities = new ArrayList<>(List.of(
                activity("System", "Sample workspace loaded."),
                activity("Kim", "Week 6 planning meeting saved."),
                activity("Kim", "Read AI coding tool guide task completed.")));

        workspace = new WorkspaceState(
                true,
                user,
                team,
                new ArrayList<>(List.of(leader, member)),
                tasks,
                meetings,
                activities,
                new ArrayList<>(),
                new ArrayList<>());

        return recompute();
    }

    public synchronized WorkspaceState createTask(CreateTaskRequest request) {
        ensureInitialized();
        requireText(request.title(), "Task title is required.");
        requireText(request.owner(), "Task owner is required.");
        requireText(request.dueDate(), "Task due date is required.");
        validateLocalDate(request.dueDate(), "Task due date must use a real yyyy-MM-dd date.");
        requireExistingMember(request.owner(), "Task owner must be an existing team member.");

        var tasks = new ArrayList<>(workspace.tasks());
        tasks.add(0, task(
                request.title().trim(),
                request.owner().trim(),
                request.dueDate().trim(),
                safeList(request.blockers()),
                TaskStatus.TODO,
                defaultText(request.note(), "Created in assignment2 workspace flow.")));

        var activities = prependActivity(workspace.activities(), activity(workspace.user().name(), request.title().trim() + " created."));
        workspace = copy(workspace, tasks, workspace.meetings(), activities, workspace.reports(), workspace.members(), workspace.team());
        return recompute();
    }

    public synchronized WorkspaceState updateTask(long taskId, UpdateTaskRequest request) {
        ensureInitialized();

        var tasks = new ArrayList<TaskView>();
        TaskView target = null;
        for (var task : workspace.tasks()) {
            if (task.id() == taskId) {
                var owner = defaultText(request.owner(), task.owner());
                requireExistingMember(owner, "Task owner must be an existing team member.");
                var dueDate = defaultText(request.dueDate(), task.dueDate());
                validateLocalDate(dueDate, "Task due date must use a real yyyy-MM-dd date.");

                target = new TaskView(
                        task.id(),
                        defaultText(request.title(), task.title()),
                        owner.trim(),
                        request.status() == null ? task.status() : request.status(),
                        dueDate.trim(),
                        defaultText(request.priority(), task.priority()),
                        request.blockers() == null ? task.blockers() : safeList(request.blockers()),
                        request.next() == null ? task.next() : safeList(request.next()),
                        defaultText(request.note(), task.note()));
                tasks.add(target);
            } else {
                tasks.add(task);
            }
        }

        if (target == null) {
            throw new IllegalArgumentException("Task not found.");
        }

        var activities = prependActivity(workspace.activities(), activity(workspace.user().name(), target.title() + " updated."));
        workspace = copy(workspace, tasks, workspace.meetings(), activities, workspace.reports(), workspace.members(), workspace.team());
        return recompute();
    }

    public synchronized WorkspaceState updateTaskStatus(long taskId, UpdateTaskStatusRequest request) {
        ensureInitialized();
        if (request.status() == null) {
            throw new IllegalArgumentException("Task status is required.");
        }

        var tasks = new ArrayList<TaskView>();
        TaskView target = null;
        for (var task : workspace.tasks()) {
            if (task.id() == taskId) {
                target = new TaskView(task.id(), task.title(), task.owner(), request.status(), task.dueDate(), task.priority(), task.blockers(), task.next(), task.note());
                tasks.add(target);
            } else {
                tasks.add(task);
            }
        }

        if (target == null) {
            throw new IllegalArgumentException("Task not found.");
        }

        var activities = prependActivity(workspace.activities(), activity(workspace.user().name(), target.title() + " moved to " + request.status() + "."));
        workspace = copy(workspace, tasks, workspace.meetings(), activities, workspace.reports(), workspace.members(), workspace.team());
        return recompute();
    }

    public synchronized WorkspaceState deleteTask(long taskId) {
        ensureInitialized();
        var tasks = new ArrayList<>(workspace.tasks());
        var removed = tasks.removeIf(task -> task.id() == taskId);
        if (!removed) {
            throw new IllegalArgumentException("Task not found.");
        }

        var activities = prependActivity(workspace.activities(), activity(workspace.user().name(), "Task deleted."));
        workspace = copy(workspace, tasks, workspace.meetings(), activities, workspace.reports(), workspace.members(), workspace.team());
        return recompute();
    }

    public synchronized WorkspaceState addTaskDependency(long taskId, TaskDependencyRequest request) {
        ensureInitialized();
        requireText(request.title(), "Dependency title is required.");
        var dependency = request.title().trim();
        var currentTarget = workspace.tasks().stream()
                .filter(task -> task.id() == taskId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
        rejectCyclicDependency(currentTarget, dependency);
        var tasks = new ArrayList<TaskView>();
        TaskView target = null;
        for (var task : workspace.tasks()) {
            if (task.id() == taskId) {
                var blockers = new ArrayList<>(task.blockers());
                if (!blockers.contains(dependency)) {
                    blockers.add(dependency);
                }
                target = new TaskView(task.id(), task.title(), task.owner(), task.status(), task.dueDate(), task.priority(), blockers, task.next(), task.note());
                tasks.add(target);
            } else {
                tasks.add(task);
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("Task not found.");
        }
        var activities = prependActivity(workspace.activities(), activity(workspace.user().name(), "Dependency added to " + target.title() + "."));
        workspace = copy(workspace, tasks, workspace.meetings(), activities, workspace.reports(), workspace.members(), workspace.team());
        return recompute();
    }

    public synchronized WorkspaceState deleteTaskDependency(long taskId, String dependencyTitle) {
        ensureInitialized();
        requireText(dependencyTitle, "Dependency title is required.");
        var tasks = new ArrayList<TaskView>();
        TaskView target = null;
        for (var task : workspace.tasks()) {
            if (task.id() == taskId) {
                var blockers = new ArrayList<>(task.blockers());
                blockers.removeIf(blocker -> blocker.equalsIgnoreCase(dependencyTitle.trim()));
                target = new TaskView(task.id(), task.title(), task.owner(), task.status(), task.dueDate(), task.priority(), blockers, task.next(), task.note());
                tasks.add(target);
            } else {
                tasks.add(task);
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("Task not found.");
        }
        var activities = prependActivity(workspace.activities(), activity(workspace.user().name(), "Dependency removed from " + target.title() + "."));
        workspace = copy(workspace, tasks, workspace.meetings(), activities, workspace.reports(), workspace.members(), workspace.team());
        return recompute();
    }

    @Override
    public synchronized WorkspaceState createProjectTask(long projectId, CreateTaskRequest request) {
        requireProjectId(projectId);
        return createTask(request);
    }

    @Override
    public synchronized WorkspaceState updateTaskById(long taskId, UpdateTaskRequest request) {
        return updateTask(taskId, request);
    }

    @Override
    public synchronized WorkspaceState updateTaskStatusById(long taskId, UpdateTaskStatusRequest request) {
        return updateTaskStatus(taskId, request);
    }

    @Override
    public synchronized WorkspaceState deleteTaskById(long taskId) {
        return deleteTask(taskId);
    }

    @Override
    public synchronized WorkspaceState addTaskDependencyById(long taskId, TaskDependencyRequest request) {
        return addTaskDependency(taskId, request);
    }

    @Override
    public synchronized WorkspaceState deleteTaskDependencyById(long taskId, String dependencyTitle) {
        return deleteTaskDependency(taskId, dependencyTitle);
    }

    public synchronized WorkspaceState createMeeting(CreateMeetingRequest request) {
        ensureInitialized();
        requireText(request.title(), "Meeting title is required.");
        requireText(request.time(), "Meeting time is required.");
        requireText(request.agenda(), "Meeting agenda is required.");
        validateMeetingTime(request.time(), "Meeting time must use a real yyyy-MM-dd or yyyy-MM-ddTHH:mm value.");

        var now = LocalDateTime.now().withNano(0).toString();
        var meeting = new MeetingView(
                nextId(),
                request.title().trim(),
                request.time().trim(),
                request.agenda().trim(),
                defaultText(request.content(), ""),
                safeList(request.decisions()),
                safeList(request.actions()),
                safeLongList(request.attendeeIds()),
                safeActionItems(request.actionItems()),
                now,
                now);

        var meetings = new ArrayList<>(workspace.meetings());
        meetings.add(0, meeting);

        var tasks = new ArrayList<>(workspace.tasks());
        if (request.createTasks()) {
            var actionOwner = defaultText(request.actionOwner(), firstMemberName());
            requireExistingMember(actionOwner, "Action owner must be an existing team member.");
            for (var action : safeList(request.actions())) {
                tasks.add(0, task(action, actionOwner, plusDays(request.time(), 7), List.of(), TaskStatus.TODO));
            }
        }

        var activities = prependActivity(workspace.activities(), activity(workspace.user().name(), meeting.title() + " meeting saved."));
        workspace = copy(workspace, tasks, meetings, activities, workspace.reports(), workspace.members(), workspace.team());
        return recompute();
    }

    public synchronized WorkspaceState generateReport() {
        ensureInitialized();
        var reports = new ArrayList<>(workspace.reports());
        reports.add(0, report());
        var activities = prependActivity(workspace.activities(), activity(workspace.user().name(), "Report draft refreshed."));
        workspace = copy(workspace, workspace.tasks(), workspace.meetings(), activities, reports, workspace.members(), workspace.team());
        return recompute();
    }

    @Override
    public synchronized WorkspaceState createProjectMeeting(long projectId, CreateMeetingRequest request) {
        requireProjectId(projectId);
        return createMeeting(request);
    }

    @Override
    public synchronized WorkspaceState generateProjectReport(long projectId) {
        requireProjectId(projectId);
        return generateReport();
    }

    public synchronized WorkspaceState updateTeam(UpdateTeamRequest request) {
        ensureInitialized();
        requireText(request.name(), "Team name is required.");
        requireText(request.courseName(), "Course name is required.");
        requireText(request.dueDate(), "Team due date is required.");
        validateLocalDate(request.dueDate(), "Team due date must use a real yyyy-MM-dd date.");
        validateOptionalLocalDate(request.startDate(), "Team start date must use a real yyyy-MM-dd date.");

        var team = new TeamProfile(
                request.name().trim(),
                request.courseName().trim(),
                defaultText(request.semester(), workspace.team().semester()),
                request.dueDate().trim(),
                workspace.team().inviteCode(),
                defaultText(request.description(), workspace.team().description()),
                defaultText(request.startDate(), workspace.team().startDate()));

        var activities = prependActivity(workspace.activities(), activity(workspace.user().name(), "Team profile updated."));
        workspace = copy(workspace, workspace.tasks(), workspace.meetings(), activities, workspace.reports(), workspace.members(), team);
        return recompute();
    }

    public synchronized WorkspaceState regenerateInviteCode() {
        ensureInitialized();
        var team = new TeamProfile(
                workspace.team().name(),
                workspace.team().courseName(),
                workspace.team().semester(),
                workspace.team().dueDate(),
                inviteCode(),
                workspace.team().description(),
                workspace.team().startDate());

        var activities = prependActivity(workspace.activities(), activity(workspace.user().name(), "Invite code regenerated."));
        workspace = copy(workspace, workspace.tasks(), workspace.meetings(), activities, workspace.reports(), workspace.members(), team);
        return recompute();
    }

    @Override
    public synchronized WorkspaceState updateProjectTeam(long projectId, UpdateTeamRequest request) {
        requireProjectId(projectId);
        return updateTeam(request);
    }

    @Override
    public synchronized WorkspaceState regenerateProjectInviteCode(long projectId) {
        requireProjectId(projectId);
        return regenerateInviteCode();
    }

    @Override
    public synchronized WorkspaceState getWorkspaceByInviteCode(String inviteCode) {
        ensureInitialized();
        requireValidInviteCode(inviteCode);
        return workspace;
    }

    @Override
    public synchronized WorkspaceState acceptInvitation(String inviteCode, String memberName, String memberEmail, TeamRole role) {
        ensureInitialized();
        requireValidInviteCode(inviteCode);
        requireText(memberName, "Member name is required.");
        requireText(memberEmail, "Member email is required.");
        var normalizedName = memberName.trim();
        var normalizedEmail = memberEmail.trim().toLowerCase();
        var duplicateEmail = workspace.members().stream().anyMatch(member -> member.email().equalsIgnoreCase(normalizedEmail));
        if (duplicateEmail) {
            return workspace;
        }

        var members = new ArrayList<>(workspace.members());
        var claimedLegacyMember = false;
        for (int index = 0; index < members.size(); index++) {
            var member = members.get(index);
            if (member.email().isBlank() && member.name().equalsIgnoreCase(normalizedName)) {
                members.set(index, new MemberView(member.id(), normalizedName, normalizedEmail, invitationMemberRole(role)));
                claimedLegacyMember = true;
                break;
            }
        }
        if (!claimedLegacyMember) {
            members.add(new MemberView(nextId(), normalizedName, normalizedEmail, invitationMemberRole(role)));
        }
        var activities = prependActivity(workspace.activities(), activity(workspace.user().name(), normalizedName + " accepted invitation."));
        workspace = copy(workspace, workspace.tasks(), workspace.meetings(), activities, workspace.reports(), members, workspace.team());
        return recompute();
    }

    public synchronized WorkspaceState addMember(CreateMemberRequest request) {
        ensureInitialized();
        requireText(request.name(), "Member name is required.");

        var normalizedName = request.name().trim();
        var duplicate = workspace.members().stream().anyMatch(member -> member.name().equalsIgnoreCase(normalizedName));
        if (duplicate) {
            throw new IllegalArgumentException("Member already exists.");
        }

        var members = new ArrayList<>(workspace.members());
        members.add(new MemberView(nextId(), normalizedName, request.role() == null ? TeamRole.MEMBER : request.role()));

        var activities = prependActivity(workspace.activities(), activity(workspace.user().name(), normalizedName + " added to team."));
        workspace = copy(workspace, workspace.tasks(), workspace.meetings(), activities, workspace.reports(), members, workspace.team());
        return recompute();
    }

    public synchronized WorkspaceState deleteMember(long memberId) {
        ensureInitialized();
        if (workspace.members().size() == 1) {
            throw new IllegalArgumentException("At least one member must remain.");
        }

        MemberView target = null;
        for (var member : workspace.members()) {
            if (member.id() == memberId) {
                target = member;
                break;
            }
        }

        if (target == null) {
            throw new IllegalArgumentException("Member not found.");
        }

        var targetName = target.name();
        var openTasks = workspace.tasks().stream().anyMatch(task -> task.owner().equals(targetName) && task.status() != TaskStatus.DONE);
        if (openTasks) {
            throw new IllegalArgumentException("Reassign open tasks before removing this member.");
        }

        var members = new ArrayList<>(workspace.members());
        members.removeIf(member -> member.id() == memberId);

        var activities = prependActivity(workspace.activities(), activity(workspace.user().name(), target.name() + " removed from team."));
        workspace = copy(workspace, workspace.tasks(), workspace.meetings(), activities, workspace.reports(), members, workspace.team());
        return recompute();
    }

    @Override
    public synchronized WorkspaceState deleteProjectMember(long projectId, long memberId) {
        requireProjectId(projectId);
        return deleteMember(memberId);
    }

    private WorkspaceState recompute() {
        var tasks = new ArrayList<>(workspace.tasks());
        tasks.sort(Comparator.comparing(TaskView::status).thenComparing(TaskView::dueDate));
        var risks = riskEngine.deriveRisks(tasks, workspace.meetings(), workspace.members());
        workspace = new WorkspaceState(
                workspace.initialized(),
                workspace.user(),
                workspace.team(),
                List.copyOf(workspace.members()),
                List.copyOf(tasks),
                List.copyOf(workspace.meetings()),
                List.copyOf(workspace.activities()),
                List.copyOf(workspace.reports()),
                List.copyOf(risks));
        return workspace;
    }

    private WorkspaceState copy(
            WorkspaceState current,
            List<TaskView> tasks,
            List<MeetingView> meetings,
            List<ActivityView> activities,
            List<ReportView> reports,
            List<MemberView> members,
            TeamProfile team
    ) {
        return copy(current, current.user(), tasks, meetings, activities, reports, members, team);
    }

    private WorkspaceState copy(
            WorkspaceState current,
            UserProfile user,
            List<TaskView> tasks,
            List<MeetingView> meetings,
            List<ActivityView> activities,
            List<ReportView> reports,
            List<MemberView> members,
            TeamProfile team
    ) {
        return new WorkspaceState(
                current.initialized(),
                user,
                team,
                new ArrayList<>(members),
                new ArrayList<>(tasks),
                new ArrayList<>(meetings),
                new ArrayList<>(activities),
                new ArrayList<>(reports),
                new ArrayList<>(current.risks()));
    }

    private WorkspaceState emptyWorkspace() {
        return new WorkspaceState(
                false,
                new UserProfile("", ""),
                new TeamProfile("", "", "2026-1", "", inviteCode()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    private TaskView task(String title, String owner, String dueDate, List<String> blockers, TaskStatus status) {
        return task(title, owner, dueDate, blockers, status, "Created in assignment2 workspace flow.");
    }

    private TaskView task(String title, String owner, String dueDate, List<String> blockers, TaskStatus status, String note) {
        return new TaskView(
                nextId(),
                title,
                owner,
                status,
                dueDate,
                blockers.isEmpty() ? "MEDIUM" : "HIGH",
                List.copyOf(blockers),
                List.of(),
                note);
    }

    private ActivityView activity(String actor, String summary) {
        return new ActivityView(nextId(), actor, LocalDateTime.now().format(TIMESTAMP_FORMAT), summary);
    }

    private ReportView report() {
        var start = LocalDate.now().withDayOfMonth(1);
        return new ReportView(nextId(), "TeamPulse report draft", start + " ~ " + LocalDate.now(), workspace.tasks().isEmpty() && workspace.meetings().isEmpty() ? "GENERATING" : "READY");
    }

    private List<ActivityView> prependActivity(List<ActivityView> current, ActivityView next) {
        var activities = new ArrayList<ActivityView>();
        activities.add(next);
        activities.addAll(current);
        return activities;
    }

    private String plusDays(String value, int days) {
        if (value == null || value.isBlank()) {
            return "";
        }
        var normalized = value.contains("T") ? value.substring(0, 10) : value;
        return parseLocalDate(normalized, "Meeting time must use a real yyyy-MM-dd or yyyy-MM-ddTHH:mm value.").plusDays(days).toString();
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

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireExistingMember(String name, String message) {
        var normalized = name == null ? "" : name.trim();
        var exists = workspace.members().stream().anyMatch(member -> member.name().equalsIgnoreCase(normalized));
        if (!exists) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireValidInviteCode(String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank() || !workspace.team().inviteCode().equalsIgnoreCase(inviteCode.trim())) {
            throw new IllegalArgumentException("Invitation is invalid or expired.");
        }
    }

    private void rejectCyclicDependency(TaskView target, String dependencyTitle) {
        if (target.title().equalsIgnoreCase(dependencyTitle)) {
            throw new IllegalArgumentException("Task cannot depend on itself.");
        }
        var dependency = workspace.tasks().stream()
                .filter(task -> task.title().equalsIgnoreCase(dependencyTitle))
                .findFirst();
        if (dependency.isPresent() && dependsOn(dependency.get(), target.title(), new HashSet<>())) {
            throw new IllegalArgumentException("Task dependency cycle is not allowed.");
        }
    }

    private boolean dependsOn(TaskView source, String targetTitle, Set<String> visitedTitles) {
        if (!visitedTitles.add(source.title().toLowerCase())) {
            return false;
        }
        for (var blocker : safeList(source.blockers())) {
            if (blocker.equalsIgnoreCase(targetTitle)) {
                return true;
            }
            var blockerTask = workspace.tasks().stream()
                    .filter(task -> task.title().equalsIgnoreCase(blocker))
                    .findFirst();
            if (blockerTask.isPresent() && dependsOn(blockerTask.get(), targetTitle, visitedTitles)) {
                return true;
            }
        }
        return false;
    }

    private void ensureInitialized() {
        if (!workspace.initialized()) {
            throw new IllegalArgumentException("Workspace is not initialized yet.");
        }
    }

    private void requireProjectId(long projectId) {
        if (projectId != workspace.projectId()) {
            throw new IllegalArgumentException("Project not found.");
        }
        ensureInitialized();
    }

    private TaskView requireTask(long taskId) {
        ensureInitialized();
        return workspace.tasks().stream()
                .filter(task -> task.id() == taskId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
    }

    private MeetingView requireMeeting(long meetingId) {
        ensureInitialized();
        return workspace.meetings().stream()
                .filter(meeting -> meeting.id() == meetingId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found."));
    }

    private ReportView requireReport(long reportId) {
        ensureInitialized();
        return workspace.reports().stream()
                .filter(report -> report.id() == reportId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Report not found."));
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

    private String firstMemberName() {
        return workspace.members().isEmpty() ? workspace.user().name() : workspace.members().get(0).name();
    }

    private String inviteCode() {
        var alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        var builder = new StringBuilder();
        for (int index = 0; index < 6; index++) {
            builder.append(alphabet.charAt(ThreadLocalRandom.current().nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private TeamRole invitationMemberRole(TeamRole ignoredRequestedRole) {
        return TeamRole.MEMBER;
    }

    private long nextId() {
        return ids.incrementAndGet();
    }
}
