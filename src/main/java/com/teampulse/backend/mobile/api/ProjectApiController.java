package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.ApiResponse;
import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.mobile.application.MobileAccountUseCase;
import com.teampulse.backend.mobile.application.ProjectWorkspaceUseCase;
import com.teampulse.backend.mobile.application.WorkspaceQueryUseCase;
import com.teampulse.backend.mobile.dto.ActivityView;
import com.teampulse.backend.mobile.dto.ActivityLogSpecResponse;
import com.teampulse.backend.mobile.dto.BootstrapWorkspaceRequest;
import com.teampulse.backend.mobile.dto.CreateMemberRequest;
import com.teampulse.backend.mobile.dto.DashboardResponse;
import com.teampulse.backend.mobile.dto.InvitationCreateResponse;
import com.teampulse.backend.mobile.dto.MeetingActionItemView;
import com.teampulse.backend.mobile.dto.MeetingView;
import com.teampulse.backend.mobile.dto.MemberView;
import com.teampulse.backend.mobile.dto.MemberSpecResponse;
import com.teampulse.backend.mobile.dto.ProjectCreateRequest;
import com.teampulse.backend.mobile.dto.ProjectCreateResponse;
import com.teampulse.backend.mobile.dto.ProjectDetailView;
import com.teampulse.backend.mobile.dto.ProjectSummaryView;
import com.teampulse.backend.mobile.dto.ProjectUpdateRequest;
import com.teampulse.backend.mobile.dto.ProjectUpdateResponse;
import com.teampulse.backend.mobile.dto.ReportCreateRequest;
import com.teampulse.backend.mobile.dto.ReportCreateResponse;
import com.teampulse.backend.mobile.dto.ReportView;
import com.teampulse.backend.mobile.dto.RiskActionOption;
import com.teampulse.backend.mobile.dto.RiskView;
import com.teampulse.backend.mobile.dto.TaskView;
import com.teampulse.backend.mobile.dto.TeamProfile;
import com.teampulse.backend.mobile.dto.UpdateAccountRequest;
import com.teampulse.backend.mobile.dto.UpdateTeamRequest;
import com.teampulse.backend.mobile.dto.UserMeResponse;
import com.teampulse.backend.mobile.dto.UserProfile;
import com.teampulse.backend.mobile.dto.WorkspaceState;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProjectApiController {

    private static final String DEFAULT_OWNER_NAME = "Demo Leader";
    private static final String DEFAULT_OWNER_EMAIL = "leader@teampulse.app";
    private static final String DEFAULT_SEMESTER = "2026-1";
    private static final String SUCCESS_MESSAGE = "\uC694\uCCAD\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4.";
    private static final String PROJECT_CREATED_MESSAGE = "\uD504\uB85C\uC81D\uD2B8\uAC00 \uC0DD\uC131\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
    private static final String REPORT_CREATED_MESSAGE = "\uB9AC\uD3EC\uD2B8\uAC00 \uC0DD\uC131\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";

    private final WorkspaceQueryUseCase workspaceQueryUseCase;
    private final ProjectWorkspaceUseCase projectWorkspaceUseCase;
    private final MobileAccountUseCase mobileAccountUseCase;

    public ProjectApiController(
            WorkspaceQueryUseCase workspaceQueryUseCase,
            ProjectWorkspaceUseCase projectWorkspaceUseCase,
            MobileAccountUseCase mobileAccountUseCase
    ) {
        this.workspaceQueryUseCase = workspaceQueryUseCase;
        this.projectWorkspaceUseCase = projectWorkspaceUseCase;
        this.mobileAccountUseCase = mobileAccountUseCase;
    }

    @GetMapping("/account")
    public ApiResponse<UserProfile> getAccount() {
        return ApiResponse.ok(workspaceQueryUseCase.getWorkspace().user());
    }

    @PatchMapping("/account")
    public ApiResponse<UserProfile> updateAccount(@Valid @RequestBody UpdateAccountRequest request) {
        return ApiResponse.ok(mobileAccountUseCase.updateAccount(request).user());
    }

    @GetMapping("/users/me")
    public SpecResponse<UserMeResponse> getCurrentUser(Authentication authentication) {
        var workspace = workspaceQueryUseCase.getWorkspace();
        return SpecResponse.ok(SUCCESS_MESSAGE, userMe(workspace, authentication));
    }

    @GetMapping("/account/activities")
    public ApiResponse<List<ActivityView>> listAccountActivities() {
        var workspace = workspaceQueryUseCase.getWorkspace();
        var currentUser = workspace.user().name();
        return ApiResponse.ok(workspace.activities().stream()
                .filter(activity -> activity.actor().equalsIgnoreCase(currentUser))
                .toList());
    }

    @PostMapping("/projects")
    public SpecResponse<ProjectCreateResponse> createProject(
            @Valid @RequestBody ProjectCreateRequest request,
            Authentication authentication
    ) {
        var currentUser = currentUser(authentication);
        var workspace = projectWorkspaceUseCase.createProjectWorkspace(new BootstrapWorkspaceRequest(
                defaultText(currentUser.name(), DEFAULT_OWNER_NAME),
                defaultText(currentUser.email(), DEFAULT_OWNER_EMAIL),
                request.projectName(),
                request.subject(),
                DEFAULT_SEMESTER,
                request.endDate(),
                normalizeNullable(request.description()),
                normalizeNullable(request.startDate())
        ));
        return SpecResponse.ok(PROJECT_CREATED_MESSAGE, new ProjectCreateResponse(
                workspace.projectId(),
                workspace.team().name(),
                TeamRole.LEADER.name()
        ));
    }

    @GetMapping("/projects")
    public SpecResponse<List<ProjectSummaryView>> listProjects(Authentication authentication) {
        return SpecResponse.ok(SUCCESS_MESSAGE, projectWorkspaceUseCase.getProjectWorkspaces().stream()
                .map(workspace -> projectSummary(workspace, authentication))
                .toList());
    }

    @GetMapping("/projects/{projectId}")
    public SpecResponse<ProjectDetailView> getProject(@PathVariable long projectId) {
        return SpecResponse.ok(SUCCESS_MESSAGE, projectDetail(projectWorkspaceUseCase.getProjectWorkspace(projectId)));
    }

    @PatchMapping("/projects/{projectId}")
    public SpecResponse<ProjectUpdateResponse> updateProject(
            @PathVariable long projectId,
            @Valid @RequestBody ProjectUpdateRequest request
    ) {
        var workspace = projectWorkspaceUseCase.updateProjectTeam(projectId, new UpdateTeamRequest(
                request.projectName(),
                request.subject(),
                DEFAULT_SEMESTER,
                request.endDate(),
                normalizeNullable(request.description()),
                normalizeNullable(request.startDate())
        ));
        return SpecResponse.ok(SUCCESS_MESSAGE, projectUpdateResponse(workspace));
    }

    @GetMapping("/projects/{projectId}/dashboard")
    public SpecResponse<DashboardResponse> getDashboard(@PathVariable long projectId) {
        return SpecResponse.ok(SUCCESS_MESSAGE, dashboard(projectWorkspaceUseCase.getProjectWorkspace(projectId)));
    }

    @GetMapping("/projects/{projectId}/members")
    public SpecResponse<List<MemberSpecResponse>> listMembers(@PathVariable long projectId) {
        var workspace = projectWorkspaceUseCase.getProjectWorkspace(projectId);
        return SpecResponse.ok(SUCCESS_MESSAGE, workspace.members().stream()
                .map(member -> memberSpec(member, workspace))
                .toList());
    }

    @PostMapping("/projects/{projectId}/members")
    public ApiResponse<WorkspaceState> addMember(
            @PathVariable long projectId,
            @Valid @RequestBody CreateMemberRequest request
    ) {
        return ApiResponse.ok(projectWorkspaceUseCase.addProjectMember(projectId, request));
    }

    @DeleteMapping("/projects/{projectId}/members/me")
    public SpecResponse<Void> leaveProject(@PathVariable long projectId, Authentication authentication) {
        var workspace = projectWorkspaceUseCase.getProjectWorkspace(projectId);
        var currentUser = currentUser(authentication);
        var target = workspace.members().stream()
                .filter(member -> !currentUser.email().isBlank() && member.email().equalsIgnoreCase(currentUser.email()))
                .findFirst()
                .or(() -> workspace.members().stream()
                        .filter(member -> member.name().equalsIgnoreCase(currentUser.name()))
                        .findFirst())
                .orElseThrow(() -> new IllegalArgumentException("Current user is not a team member."));
        if (workspace.members().size() == 1) {
            projectWorkspaceUseCase.resetProjectWorkspace(projectId);
        } else {
            projectWorkspaceUseCase.deleteProjectMember(projectId, target.id());
        }
        return SpecResponse.ok("\uD300\uC5D0\uC11C \uD0C8\uD1F4\uD588\uC2B5\uB2C8\uB2E4.", null);
    }

    @DeleteMapping("/projects/{projectId}/members/{memberId}")
    public ApiResponse<WorkspaceState> deleteMember(@PathVariable long projectId, @PathVariable long memberId) {
        return ApiResponse.ok(projectWorkspaceUseCase.deleteProjectMember(projectId, memberId));
    }

    @PostMapping("/projects/{projectId}/invite-links")
    public ApiResponse<Map<String, Object>> createInviteLink(
            @PathVariable long projectId,
            Authentication authentication
    ) {
        var workspace = projectWorkspaceUseCase.regenerateProjectInviteCode(projectId);
        return ApiResponse.ok(invitePayload(workspace));
    }

    @PostMapping("/projects/{projectId}/invitations")
    public SpecResponse<InvitationCreateResponse> createInvitation(
            @PathVariable long projectId,
            Authentication authentication
    ) {
        var workspace = projectWorkspaceUseCase.regenerateProjectInviteCode(projectId);
        return SpecResponse.ok(SUCCESS_MESSAGE, invitationResponse(projectId, workspace.team()));
    }

    @GetMapping("/projects/{projectId}/activities")
    public ApiResponse<List<ActivityView>> listActivities(@PathVariable long projectId) {
        return ApiResponse.ok(projectWorkspaceUseCase.getProjectWorkspace(projectId).activities());
    }

    @GetMapping("/projects/{projectId}/activity-logs")
    public SpecResponse<List<ActivityLogSpecResponse>> listActivityLogs(@PathVariable long projectId) {
        return SpecResponse.ok(SUCCESS_MESSAGE, projectWorkspaceUseCase.getProjectWorkspace(projectId).activities().stream()
                .map(this::activityLog)
                .toList());
    }

    @GetMapping("/projects/{projectId}/risks")
    public ApiResponse<List<RiskView>> listRisks(@PathVariable long projectId) {
        return ApiResponse.ok(projectWorkspaceUseCase.getProjectWorkspace(projectId).risks());
    }

    @GetMapping("/projects/{projectId}/risks/{riskId}/actions")
    public ApiResponse<List<RiskActionOption>> listRiskActions(@PathVariable long projectId, @PathVariable long riskId) {
        var workspace = projectWorkspaceUseCase.getProjectWorkspace(projectId);
        var risk = workspace.risks().stream()
                .filter(candidate -> candidate.id() == riskId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Risk not found."));
        return ApiResponse.ok(riskActions(risk, workspace));
    }

    @PostMapping("/projects/{projectId}/reports")
    public SpecResponse<ReportCreateResponse> createReport(
            @PathVariable long projectId,
            @Valid @RequestBody(required = false) ReportCreateRequest request
    ) {
        if (request != null && request.reportType() != null && !request.reportType().isBlank()
                && !"PDF".equals(request.reportType())) {
            throw new IllegalArgumentException("Report type must be PDF.");
        }
        requireReportable(projectWorkspaceUseCase.getProjectWorkspace(projectId));
        var workspace = projectWorkspaceUseCase.generateProjectReport(projectId);
        var report = latestReport(workspace);
        return SpecResponse.ok(REPORT_CREATED_MESSAGE, new ReportCreateResponse(report.id(), "/api/reports/" + report.id() + "/download"));
    }

    @GetMapping("/projects/{projectId}/reports")
    public SpecResponse<List<ReportView>> listReports(@PathVariable long projectId) {
        return SpecResponse.ok(SUCCESS_MESSAGE, projectWorkspaceUseCase.getProjectWorkspace(projectId).reports());
    }

    @GetMapping("/projects/{projectId}/reports/{reportId}/download")
    public void downloadReport(
            @PathVariable long projectId,
            @PathVariable long reportId,
            HttpServletResponse response
    ) throws IOException {
        writeReportDownloadResponse(projectWorkspaceUseCase.getProjectWorkspace(projectId), reportId, response);
    }

    @GetMapping("/reports/{reportId}/download")
    public void downloadReport(@PathVariable long reportId, HttpServletResponse response) throws IOException {
        writeReportDownloadResponse(projectWorkspaceUseCase.getProjectWorkspaceByReportId(reportId), reportId, response);
    }

    private void writeReportDownloadResponse(WorkspaceState workspace, long reportId, HttpServletResponse response) throws IOException {
        var report = workspace.reports().stream()
                .filter(candidate -> candidate.id() == reportId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Report not found."));
        var body = reportPdf(workspace, report);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("teampulse-report.pdf")
                .build()
                .toString());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    private ProjectSummaryView projectSummary(WorkspaceState workspace, Authentication authentication) {
        return new ProjectSummaryView(
                workspace.projectId(),
                workspace.team().name(),
                workspace.team().courseName(),
                projectRole(workspace, authentication).name(),
                workspace.team().dueDate()
        );
    }

    private TeamRole projectRole(WorkspaceState workspace, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser) {
            if (workspace.user().email().equalsIgnoreCase(authUser.email())) {
                return TeamRole.LEADER;
            }
            return workspace.members().stream()
                    .filter(member -> member.email().equalsIgnoreCase(authUser.email())
                            || member.name().equalsIgnoreCase(authUser.name()))
                    .map(MemberView::role)
                    .findFirst()
                    .orElse(TeamRole.MEMBER);
        }
        return TeamRole.LEADER;
    }

    private UserMeResponse userMe(WorkspaceState workspace, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser) {
            return new UserMeResponse(
                    authUser.id(),
                    authUser.email(),
                    authUser.email(),
                    authUser.name(),
                    authUser.university(),
                    authUser.phone());
        }
        var leader = workspace.members().stream()
                .filter(member -> member.name().equalsIgnoreCase(workspace.user().name()))
                .findFirst();
        return new UserMeResponse(
                leader.map(MemberView::id).orElse(workspace.projectId()),
                workspace.user().email(),
                workspace.user().email(),
                workspace.user().name(),
                workspace.user().university(),
                workspace.user().phone());
    }

    private UserProfile currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser) {
            return new UserProfile(authUser.name(), authUser.email(), authUser.university(), authUser.phone());
        }
        return workspaceQueryUseCase.getWorkspace().user();
    }

    private DashboardResponse dashboard(WorkspaceState workspace) {
        var tasks = workspace.tasks();
        var doneCount = (int) tasks.stream().filter(task -> task.status() == TaskStatus.DONE).count();
        var todoCount = (int) tasks.stream().filter(task -> task.status() == TaskStatus.TODO).count();
        var doingCount = (int) tasks.stream().filter(task -> task.status() == TaskStatus.DOING).count();
        var totalCount = tasks.size();
        var progressRate = totalCount == 0 ? 0.0 : Math.round((doneCount * 1000.0 / totalCount)) / 10.0;
        var today = LocalDate.now();

        var taskSummary = new DashboardResponse.TaskSummary(totalCount, todoCount, doingCount, doneCount, progressRate);
        var scheduleSummary = new DashboardResponse.ScheduleSummary(
                workspace.team().startDate(),
                workspace.team().dueDate(),
                remainingDays(workspace.team().dueDate(), today),
                (int) tasks.stream()
                        .filter(task -> task.status() != TaskStatus.DONE)
                        .filter(task -> parseDateOrMax(task.dueDate()).isBefore(today))
                        .count(),
                (int) tasks.stream()
                        .filter(task -> task.status() != TaskStatus.DONE)
                        .filter(task -> {
                            var dueDate = parseDateOrMax(task.dueDate());
                            return !dueDate.equals(LocalDate.MAX)
                                    && !dueDate.isBefore(today)
                                    && ChronoUnit.DAYS.between(today, dueDate) <= 3;
                        })
                        .count());

        var memberWorkload = workspace.members().stream()
                .map(member -> new DashboardResponse.MemberWorkload(
                        member.id(),
                        member.name(),
                        (int) tasks.stream().filter(task -> task.owner().equalsIgnoreCase(member.name())).count(),
                        (int) tasks.stream()
                                .filter(task -> task.owner().equalsIgnoreCase(member.name()))
                                .filter(task -> task.status() == TaskStatus.DONE)
                                .count()))
                .toList();

        var dashboardRisks = workspace.risks().stream()
                .map(risk -> dashboardRisk(risk, workspace))
                .toList();
        var dangerCount = (int) dashboardRisks.stream().filter(risk -> risk.level().equals("DANGER")).count();
        var warningCount = (int) dashboardRisks.stream().filter(risk -> risk.level().equals("WARNING")).count();
        var cautionCount = (int) dashboardRisks.stream().filter(risk -> risk.level().equals("CAUTION")).count();
        var riskSummary = new DashboardResponse.RiskSummary(
                dashboardRisks.size(),
                cautionCount,
                warningCount,
                dangerCount,
                dangerCount > 0);

        return new DashboardResponse(
                workspace.projectId(),
                workspace.team().name(),
                taskSummary,
                scheduleSummary,
                memberWorkload,
                riskSummary,
                dashboardRisks);
    }

    private DashboardResponse.DashboardRisk dashboardRisk(RiskView risk, WorkspaceState workspace) {
        var level = switch (risk.severity()) {
            case CRITICAL -> "DANGER";
            case WARNING -> "WARNING";
            case INFO -> "CAUTION";
        };
        var relatedTask = firstAffectedTask(risk, workspace.tasks());
        return new DashboardResponse.DashboardRisk(
                risk.title().toUpperCase().replace(' ', '_'),
                level,
                risk.body(),
                relatedTask == null ? null : relatedTask.id(),
                relatedTask == null ? null : relatedTask.title(),
                null,
                null,
                risk.affectedTaskIds(),
                risk.suggestedActions());
    }

    private long remainingDays(String projectEndDate, LocalDate today) {
        var endDate = parseDateOrMax(projectEndDate);
        if (endDate.equals(LocalDate.MAX)) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(today, endDate));
    }

    private MemberSpecResponse memberSpec(MemberView member, WorkspaceState workspace) {
        return new MemberSpecResponse(
                member.id(),
                member.name(),
                !member.email().isBlank()
                        ? member.email()
                        : member.name().equalsIgnoreCase(workspace.user().name())
                        ? workspace.user().email()
                        : member.name().toLowerCase().replace(" ", ".") + "@example.com",
                member.role());
    }

    private ActivityLogSpecResponse activityLog(ActivityView activity) {
        return new ActivityLogSpecResponse(
                activity.id(),
                "ACTIVITY_RECORDED",
                activity.summary(),
                activity.actor(),
                activity.at());
    }

    private ProjectDetailView projectDetail(WorkspaceState workspace) {
        return new ProjectDetailView(
                workspace.projectId(),
                workspace.team().name(),
                workspace.team().courseName(),
                workspace.team().description(),
                workspace.team().startDate(),
                workspace.team().dueDate(),
                workspace.members().size()
        );
    }

    private ProjectUpdateResponse projectUpdateResponse(WorkspaceState workspace) {
        return new ProjectUpdateResponse(
                workspace.projectId(),
                workspace.team().name(),
                workspace.team().courseName(),
                workspace.team().description(),
                workspace.team().startDate(),
                workspace.team().dueDate(),
                LocalDateTime.now().toString()
        );
    }

    private Map<String, Object> invitePayload(WorkspaceState workspace) {
        return Map.of(
                "projectId", workspace.projectId(),
                "token", workspace.team().inviteCode(),
                "inviteCode", workspace.team().inviteCode(),
                "url", "/invite/" + workspace.team().inviteCode()
        );
    }

    private InvitationCreateResponse invitationResponse(long projectId, TeamProfile team) {
        return new InvitationCreateResponse(
                Math.abs((long) team.inviteCode().hashCode()),
                projectId,
                team.inviteCode(),
                "https://teampulse.com/invite/" + team.inviteCode(),
                LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS).toString());
    }

    private void requireReportable(WorkspaceState workspace) {
        if (workspace.tasks().isEmpty() && workspace.meetings().isEmpty()) {
            throw new IllegalArgumentException("Report data is insufficient.");
        }
    }

    private ReportView latestReport(WorkspaceState workspace) {
        return workspace.reports().stream()
                .max(Comparator.comparingLong(ReportView::id))
                .orElseThrow(() -> new IllegalArgumentException("Report not found."));
    }

    private byte[] reportPdf(WorkspaceState workspace, ReportView report) {
        var lines = new java.util.ArrayList<String>();
        addReportLine(lines, "TeamPulse Report");
        addReportLine(lines, "Generated at: " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        addReportLine(lines, "Project: " + textOrDash(workspace.team().name()));
        addReportLine(lines, "Subject: " + textOrDash(workspace.team().courseName()));
        addReportLine(lines, "Schedule: " + textOrDash(workspace.team().startDate()) + " ~ " + textOrDash(workspace.team().dueDate()));
        addReportLine(lines, "Report range: " + report.range() + " / Status: " + report.status());

        addReportSection(lines, "Executive Summary");
        addWrappedReportLine(lines, "Tasks: total " + workspace.tasks().size()
                + ", TODO " + countTasksByStatus(workspace, TaskStatus.TODO)
                + ", DOING " + countTasksByStatus(workspace, TaskStatus.DOING)
                + ", DONE " + countTasksByStatus(workspace, TaskStatus.DONE)
                + ", overdue " + countOverdueTasks(workspace) + ".");
        addWrappedReportLine(lines, "Meetings: " + workspace.meetings().size()
                + ", risks: " + workspace.risks().size()
                + ", recent activity logs: " + workspace.activities().size() + ".");
        addWrappedReportLine(lines, "Members: " + workspace.members().stream()
                .map(member -> member.name() + "(" + member.role() + ")")
                .collect(java.util.stream.Collectors.joining(", ")));

        addReportSection(lines, "Task Details");
        if (workspace.tasks().isEmpty()) {
            addReportLine(lines, "- No tasks recorded.");
        } else {
            workspace.tasks().stream()
                    .sorted(Comparator.comparing(TaskView::dueDate))
                    .limit(8)
                    .forEach(task -> {
                        addWrappedReportLine(lines, "- #" + task.id() + " [" + task.status() + "] "
                                + task.title() + " / owner: " + task.owner()
                                + " / due: " + task.dueDate()
                                + " / priority: " + task.priority());
                        if (!task.blockers().isEmpty()) {
                            addWrappedReportLine(lines, "  blockers: " + String.join(", ", task.blockers()));
                        }
                        if (task.note() != null && !task.note().isBlank()) {
                            addWrappedReportLine(lines, "  note: " + task.note());
                        }
                    });
        }

        addReportSection(lines, "Meeting Notes");
        if (workspace.meetings().isEmpty()) {
            addReportLine(lines, "- No meetings recorded.");
        } else {
            workspace.meetings().stream()
                    .sorted(Comparator.comparing(MeetingView::time).reversed())
                    .limit(4)
                    .forEach(meeting -> {
                        addWrappedReportLine(lines, "- " + meeting.time() + " / " + meeting.title());
                        addWrappedReportLine(lines, "  agenda: " + meeting.agenda());
                        if (!meeting.decisions().isEmpty()) {
                            addWrappedReportLine(lines, "  decisions: " + String.join(", ", meeting.decisions()));
                        }
                        if (!meeting.actionItems().isEmpty()) {
                            addWrappedReportLine(lines, "  action items: " + meeting.actionItems().stream()
                                    .map(MeetingActionItemView::content)
                                    .collect(java.util.stream.Collectors.joining(", ")));
                        }
                    });
        }

        addReportSection(lines, "Risk Signals");
        if (workspace.risks().isEmpty()) {
            addReportLine(lines, "- No active risk signals.");
        } else {
            workspace.risks().stream()
                    .limit(5)
                    .forEach(risk -> {
                        addWrappedReportLine(lines, "- [" + risk.severity() + "] " + risk.title());
                        addWrappedReportLine(lines, "  basis: " + risk.body());
                        addWrappedReportLine(lines, "  suggested action: " + risk.action());
                    });
        }

        addReportSection(lines, "Recent Activity");
        if (workspace.activities().isEmpty()) {
            addReportLine(lines, "- No activity logs recorded.");
        } else {
            workspace.activities().stream()
                    .limit(6)
                    .forEach(activity -> addWrappedReportLine(lines,
                "- " + activity.at() + " / " + activity.actor() + ": " + activity.summary()));
    }

        var stream = new StringBuilder("BT\n/F1 16 Tf\n50 790 Td\n");
        for (int index = 0; index < lines.size(); index++) {
            if (index == 1) {
                stream.append("/F1 9 Tf\n");
            }
            if (index > 0) {
                stream.append("0 -15 Td\n");
            }
            stream.append("(").append(pdfText(lines.get(index))).append(") Tj\n");
        }
        stream.append("ET");
        return minimalPdf(stream.toString());
    }

    private void addReportSection(List<String> lines, String title) {
        addReportLine(lines, "");
        addReportLine(lines, title);
    }

    private void addWrappedReportLine(List<String> lines, String value) {
        var clean = textOrDash(value).replaceAll("\\s+", " ").trim();
        var width = 92;
        while (clean.length() > width) {
            var splitAt = clean.lastIndexOf(' ', width);
            if (splitAt < 40) {
                splitAt = width;
            }
            addReportLine(lines, clean.substring(0, splitAt));
            clean = clean.substring(splitAt).trim();
        }
        addReportLine(lines, clean);
    }

    private void addReportLine(List<String> lines, String value) {
        if (lines.size() >= 51) {
            return;
        }
        lines.add(value == null ? "" : value);
    }

    private long countTasksByStatus(WorkspaceState workspace, TaskStatus status) {
        return workspace.tasks().stream()
                .filter(task -> task.status() == status)
                .count();
    }

    private long countOverdueTasks(WorkspaceState workspace) {
        var today = LocalDate.now();
        return workspace.tasks().stream()
                .filter(task -> task.status() != TaskStatus.DONE)
                .filter(task -> parseDateOrMax(task.dueDate()).isBefore(today))
                .count();
    }

    private String textOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private byte[] minimalPdf(String contentStream) {
        var objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + contentStream.length() + " >>\nstream\n" + contentStream + "\nendstream");
        var body = new StringBuilder("%PDF-1.4\n");
        var offsets = new java.util.ArrayList<Integer>();
        for (int index = 0; index < objects.size(); index++) {
            offsets.add(body.length());
            body.append(index + 1).append(" 0 obj\n")
                    .append(objects.get(index)).append("\n")
                    .append("endobj\n");
        }
        var xrefOffset = body.length();
        body.append("xref\n0 ").append(objects.size() + 1).append("\n")
                .append("0000000000 65535 f \n");
        for (var offset : offsets) {
            body.append("%010d 00000 n \n".formatted(offset));
        }
        body.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n")
                .append("startxref\n")
                .append(xrefOffset)
                .append("\n%%EOF\n");
        return body.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private String pdfText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        var builder = new StringBuilder();
        for (var character : value.toCharArray()) {
            if (character == '(' || character == ')' || character == '\\') {
                builder.append('\\').append(character);
            } else if (character >= 32 && character <= 126) {
                builder.append(character);
            } else {
                builder.append('?');
            }
        }
        return builder.toString();
    }

    private List<RiskActionOption> riskActions(RiskView risk, WorkspaceState workspace) {
        var affectedTask = firstAffectedTask(risk, workspace.tasks());
        var overdueOrDueSoon = affectedTask == null ? firstOpenTaskByDueDate(workspace.tasks()) : affectedTask;
        var reassignmentTarget = affectedTask == null ? firstOpenTaskByOwnerLoad(workspace.tasks()) : affectedTask;
        var leastLoadedOwner = leastLoadedOwner(workspace);

        return switch ((int) risk.id()) {
            case 101, 102 -> List.of(
                    new RiskActionOption(
                            "RESCHEDULE",
                            "일정 재조정",
                            "지연 또는 임박한 태스크의 마감일을 뒤로 조정합니다.",
                            overdueOrDueSoon == null ? null : overdueOrDueSoon.id(),
                            null,
                            overdueOrDueSoon == null ? null : suggestedDueDate(overdueOrDueSoon.dueDate(), 2)),
                    new RiskActionOption(
                            "REASSIGN",
                            "작업 재할당",
                            "마감 위험이 있는 태스크를 작업량이 적은 팀원에게 재배정합니다.",
                            overdueOrDueSoon == null ? null : overdueOrDueSoon.id(),
                            leastLoadedOwner,
                            null));
            case 103 -> List.of(
                    new RiskActionOption(
                            "UNBLOCK",
                            "선행 작업 정리",
                            "블로커를 가진 태스크의 선행 작업을 우선 처리 대상으로 지정합니다.",
                            overdueOrDueSoon == null ? null : overdueOrDueSoon.id(),
                            null,
                            null),
                    new RiskActionOption(
                            "SPLIT_TASK",
                            "작업 분할",
                            "막힌 작업을 더 작은 실행 단위로 나누어 병렬 진행 가능성을 높입니다.",
                            overdueOrDueSoon == null ? null : overdueOrDueSoon.id(),
                            null,
                            null));
            case 104 -> List.of(
                    new RiskActionOption(
                            "REASSIGN",
                            "작업 재할당",
                            "특정 팀원에게 집중된 태스크 일부를 다른 팀원에게 옮깁니다.",
                            reassignmentTarget == null ? null : reassignmentTarget.id(),
                            leastLoadedOwner,
                            null),
                    new RiskActionOption(
                            "RESCHEDULE",
                            "일정 재조정",
                            "담당자 과부하가 큰 태스크의 마감일을 조정합니다.",
                            reassignmentTarget == null ? null : reassignmentTarget.id(),
                            null,
                            reassignmentTarget == null ? null : suggestedDueDate(reassignmentTarget.dueDate(), 2)));
            case 105 -> List.of(
                    new RiskActionOption(
                            "SCHEDULE_MEETING",
                            "회의 일정 등록",
                            "누락된 회의록을 보완하기 위해 동기화 회의를 등록합니다.",
                            null,
                            null,
                            LocalDate.now().plusDays(1).toString()));
            default -> List.of(
                    new RiskActionOption(
                            "REVIEW",
                            "리스크 검토",
                            "담당자가 리스크 상태를 확인하고 필요한 대응을 선택합니다.",
                            null,
                            null,
                            null));
        };
    }

    private TaskView firstAffectedTask(RiskView risk, List<TaskView> tasks) {
        for (var affectedTaskId : risk.affectedTaskIds()) {
            for (var task : tasks) {
                if (task.id() == affectedTaskId) {
                    return task;
                }
            }
        }
        return null;
    }

    private TaskView firstOpenTaskByDueDate(List<TaskView> tasks) {
        return tasks.stream()
                .filter(task -> task.status() != TaskStatus.DONE)
                .min(Comparator.comparing(task -> parseDateOrMax(task.dueDate())))
                .orElse(null);
    }

    private TaskView firstOpenTaskByOwnerLoad(List<TaskView> tasks) {
        var busiestOwner = tasks.stream()
                .filter(task -> task.status() != TaskStatus.DONE)
                .collect(java.util.stream.Collectors.groupingBy(TaskView::owner, java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .max(Comparator.comparingLong(entry -> entry.getValue()))
                .map(Map.Entry::getKey)
                .orElse(null);
        if (busiestOwner == null) {
            return null;
        }
        return tasks.stream()
                .filter(task -> task.status() != TaskStatus.DONE)
                .filter(task -> task.owner().equals(busiestOwner))
                .findFirst()
                .orElse(null);
    }

    private String leastLoadedOwner(WorkspaceState workspace) {
        if (workspace.members().isEmpty()) {
            return null;
        }
        var openTaskCounts = workspace.tasks().stream()
                .filter(task -> task.status() != TaskStatus.DONE)
                .collect(java.util.stream.Collectors.groupingBy(TaskView::owner, java.util.stream.Collectors.counting()));
        return workspace.members().stream()
                .min(Comparator.comparingLong(member -> openTaskCounts.getOrDefault(member.name(), 0L)))
                .map(member -> member.name())
                .orElse(null);
    }

    private String suggestedDueDate(String dueDate, int plusDays) {
        var parsed = parseDateOrMax(dueDate);
        if (parsed.equals(LocalDate.MAX)) {
            parsed = LocalDate.now();
        }
        return parsed.plusDays(plusDays).toString();
    }

    private LocalDate parseDateOrMax(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.MAX;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return LocalDate.MAX;
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null ? "" : value.trim();
    }
}
