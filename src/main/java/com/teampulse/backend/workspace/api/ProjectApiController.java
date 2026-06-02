package com.teampulse.backend.workspace.api;

import com.teampulse.backend.common.api.ApiResponse;
import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.workspace.application.ProjectWorkspaceUseCase;
import com.teampulse.backend.workspace.application.service.WorkspaceReportPdfGenerator;
import com.teampulse.backend.workspace.dto.ActivityView;
import com.teampulse.backend.workspace.dto.ActivityLogSpecResponse;
import com.teampulse.backend.workspace.dto.BootstrapWorkspaceRequest;
import com.teampulse.backend.workspace.dto.DashboardResponse;
import com.teampulse.backend.workspace.dto.InvitationCreateResponse;
import com.teampulse.backend.workspace.dto.MemberView;
import com.teampulse.backend.workspace.dto.MemberSpecResponse;
import com.teampulse.backend.workspace.dto.ProjectCreateRequest;
import com.teampulse.backend.workspace.dto.ProjectCreateResponse;
import com.teampulse.backend.workspace.dto.ProjectDetailView;
import com.teampulse.backend.workspace.dto.ProjectSummaryView;
import com.teampulse.backend.workspace.dto.ProjectUpdateRequest;
import com.teampulse.backend.workspace.dto.ProjectUpdateResponse;
import com.teampulse.backend.workspace.dto.ReportCreateRequest;
import com.teampulse.backend.workspace.dto.ReportCreateResponse;
import com.teampulse.backend.workspace.dto.ReportView;
import com.teampulse.backend.workspace.dto.RiskView;
import com.teampulse.backend.workspace.dto.TaskView;
import com.teampulse.backend.workspace.dto.TeamProfile;
import com.teampulse.backend.workspace.dto.UpdateTeamRequest;
import com.teampulse.backend.workspace.dto.UserMeResponse;
import com.teampulse.backend.workspace.dto.UserProfile;
import com.teampulse.backend.workspace.dto.WorkspaceState;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
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
    private static final String DEFAULT_FRONTEND_PUBLIC_BASE_URL = "https://team-pulse-frontend.vercel.app";

    private final ProjectWorkspaceUseCase projectWorkspaceUseCase;
    private final WorkspaceReportPdfGenerator reportPdfGenerator;
    private final String frontendPublicBaseUrl;

    public ProjectApiController(
            ProjectWorkspaceUseCase projectWorkspaceUseCase,
            WorkspaceReportPdfGenerator reportPdfGenerator,
            @Value("${app.frontend.public-base-url:" + DEFAULT_FRONTEND_PUBLIC_BASE_URL + "}") String frontendPublicBaseUrl
    ) {
        this.projectWorkspaceUseCase = projectWorkspaceUseCase;
        this.reportPdfGenerator = reportPdfGenerator;
        this.frontendPublicBaseUrl = normalizeBaseUrl(frontendPublicBaseUrl);
    }

    @GetMapping("/users/me")
    public SpecResponse<UserMeResponse> getCurrentUser(Authentication authentication) {
        return SpecResponse.ok(SUCCESS_MESSAGE, userMe(requireAuthUser(authentication)));
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
            @Valid @RequestBody ProjectUpdateRequest request,
            Authentication authentication
    ) {
        requireProjectLeader(projectWorkspaceUseCase.getProjectWorkspace(projectId), authentication);
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
        if (target.role() == TeamRole.LEADER && workspace.members().size() > 1) {
            throw new IllegalArgumentException("Project leader cannot leave before all other members leave.");
        }
        if (workspace.members().size() == 1) {
            projectWorkspaceUseCase.resetProjectWorkspace(projectId);
        } else {
            projectWorkspaceUseCase.deleteProjectMember(projectId, target.id());
        }
        return SpecResponse.ok("\uD300\uC5D0\uC11C \uD0C8\uD1F4\uD588\uC2B5\uB2C8\uB2E4.", null);
    }

    @PostMapping("/projects/{projectId}/invitations")
    public SpecResponse<InvitationCreateResponse> createInvitation(
            @PathVariable long projectId,
            Authentication authentication
    ) {
        requireProjectLeader(projectWorkspaceUseCase.getProjectWorkspace(projectId), authentication);
        var workspace = projectWorkspaceUseCase.regenerateProjectInviteCode(projectId);
        return SpecResponse.ok(SUCCESS_MESSAGE, invitationResponse(projectId, workspace.team()));
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

    @GetMapping("/reports/{reportId}/download")
    public void downloadReport(@PathVariable long reportId, HttpServletResponse response) throws IOException {
        writeReportDownloadResponse(projectWorkspaceUseCase.getProjectWorkspaceByReportId(reportId), reportId, response);
    }

    private void writeReportDownloadResponse(WorkspaceState workspace, long reportId, HttpServletResponse response) throws IOException {
        var report = workspace.reports().stream()
                .filter(candidate -> candidate.id() == reportId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Report not found."));
        var body = reportPdfGenerator.generate(workspace, report);

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
        var authUser = requireAuthUser(authentication);
        if (isProjectOwner(workspace, authUser)) {
            return TeamRole.LEADER;
        }
        return workspace.members().stream()
                .filter(member -> sameMember(member, authUser))
                .map(MemberView::role)
                .findFirst()
                .orElse(TeamRole.MEMBER);
    }

    private void requireProjectLeader(WorkspaceState workspace, Authentication authentication) {
        var authUser = requireAuthUser(authentication);
        if (isProjectOwner(workspace, authUser) || hasLeaderMembership(workspace, authUser)) {
            return;
        }
        throw new AccessDeniedException("Only project leaders can manage this project.");
    }

    private boolean isProjectOwner(WorkspaceState workspace, AuthUser authUser) {
        return !authUser.email().isBlank() && workspace.user().email().equalsIgnoreCase(authUser.email());
    }

    private boolean hasLeaderMembership(WorkspaceState workspace, AuthUser authUser) {
        return workspace.members().stream()
                .filter(member -> member.role() == TeamRole.LEADER)
                .anyMatch(member -> sameMember(member, authUser));
    }

    private boolean sameMember(MemberView member, AuthUser authUser) {
        return !member.email().isBlank()
                && !authUser.email().isBlank()
                && member.email().equalsIgnoreCase(authUser.email());
    }

    private UserMeResponse userMe(AuthUser authUser) {
        return new UserMeResponse(
                authUser.id(),
                authUser.email(),
                authUser.email(),
                authUser.name(),
                authUser.university(),
                authUser.phone());
    }

    private UserProfile currentUser(Authentication authentication) {
        var authUser = requireAuthUser(authentication);
        return new UserProfile(authUser.name(), authUser.email(), authUser.university(), authUser.phone());
    }

    private AuthUser requireAuthUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser) {
            return authUser;
        }
        throw new AccessDeniedException("Authentication user is required.");
    }

    private DashboardResponse dashboard(WorkspaceState workspace) {
        var tasks = workspace.tasks();
        var dashboardRisks = workspace.risks().stream()
                .map(risk -> dashboardRisk(risk, workspace))
                .toList();

        return new DashboardResponse(
                workspace.projectId(),
                workspace.team().name(),
                taskSummary(tasks),
                scheduleSummary(workspace, tasks),
                memberWorkload(workspace, tasks),
                riskSummary(dashboardRisks),
                dashboardRisks);
    }

    private DashboardResponse.TaskSummary taskSummary(List<TaskView> tasks) {
        var doneCount = (int) tasks.stream().filter(task -> task.status() == TaskStatus.DONE).count();
        var todoCount = (int) tasks.stream().filter(task -> task.status() == TaskStatus.TODO).count();
        var doingCount = (int) tasks.stream().filter(task -> task.status() == TaskStatus.DOING).count();
        var totalCount = tasks.size();
        var progressRate = totalCount == 0 ? 0.0 : Math.round((doneCount * 1000.0 / totalCount)) / 10.0;
        return new DashboardResponse.TaskSummary(totalCount, todoCount, doingCount, doneCount, progressRate);
    }

    private DashboardResponse.ScheduleSummary scheduleSummary(WorkspaceState workspace, List<TaskView> tasks) {
        var today = LocalDate.now();
        return new DashboardResponse.ScheduleSummary(
                workspace.team().startDate(),
                workspace.team().dueDate(),
                remainingDays(workspace.team().dueDate(), today),
                overdueTaskCount(tasks, today),
                dueSoonTaskCount(tasks, today));
    }

    private List<DashboardResponse.MemberWorkload> memberWorkload(WorkspaceState workspace, List<TaskView> tasks) {
        return workspace.members().stream()
                .map(member -> new DashboardResponse.MemberWorkload(
                        member.id(),
                        member.name(),
                        memberTaskCount(tasks, member),
                        memberDoneTaskCount(tasks, member)))
                .toList();
    }

    private DashboardResponse.RiskSummary riskSummary(List<DashboardResponse.DashboardRisk> dashboardRisks) {
        var dangerCount = (int) dashboardRisks.stream().filter(risk -> risk.level().equals("DANGER")).count();
        var warningCount = (int) dashboardRisks.stream().filter(risk -> risk.level().equals("WARNING")).count();
        var cautionCount = (int) dashboardRisks.stream().filter(risk -> risk.level().equals("CAUTION")).count();
        return new DashboardResponse.RiskSummary(
                dashboardRisks.size(),
                cautionCount,
                warningCount,
                dangerCount,
                dangerCount > 0);
    }

    private int overdueTaskCount(List<TaskView> tasks, LocalDate today) {
        return (int) tasks.stream()
                .filter(task -> task.status() != TaskStatus.DONE)
                .filter(task -> parseDateOrMax(task.dueDate()).isBefore(today))
                .count();
    }

    private int dueSoonTaskCount(List<TaskView> tasks, LocalDate today) {
        return (int) tasks.stream()
                .filter(task -> task.status() != TaskStatus.DONE)
                .filter(task -> isDueWithinThreeDays(task, today))
                .count();
    }

    private boolean isDueWithinThreeDays(TaskView task, LocalDate today) {
        var dueDate = parseDateOrMax(task.dueDate());
        return !dueDate.equals(LocalDate.MAX)
                && !dueDate.isBefore(today)
                && ChronoUnit.DAYS.between(today, dueDate) <= 3;
    }

    private int memberTaskCount(List<TaskView> tasks, MemberView member) {
        return (int) tasks.stream()
                .filter(task -> task.owner().equalsIgnoreCase(member.name()))
                .count();
    }

    private int memberDoneTaskCount(List<TaskView> tasks, MemberView member) {
        return (int) tasks.stream()
                .filter(task -> task.owner().equalsIgnoreCase(member.name()))
                .filter(task -> task.status() == TaskStatus.DONE)
                .count();
    }

    private DashboardResponse.DashboardRisk dashboardRisk(RiskView risk, WorkspaceState workspace) {
        var level = switch (risk.severity()) {
            case CRITICAL -> "DANGER";
            case WARNING -> "WARNING";
            case INFO -> "CAUTION";
        };
        var relatedTask = firstAffectedTask(risk, workspace.tasks());
        var relatedMember = relatedTask == null ? null : memberByName(workspace.members(), relatedTask.owner());
        return new DashboardResponse.DashboardRisk(
                riskType(risk),
                level,
                risk.body(),
                relatedTask == null ? null : relatedTask.id(),
                relatedTask == null ? null : relatedTask.title(),
                relatedMember == null ? null : relatedMember.id(),
                relatedMember == null ? null : relatedMember.name(),
                risk.affectedTaskIds(),
                risk.suggestedActions());
    }

    private String riskType(RiskView risk) {
        return switch ((int) risk.id()) {
            case 101 -> "PROGRESS_STALLED";
            case 102 -> "SCHEDULE_DELAY";
            case 103 -> "DEPENDENCY_BLOCKED";
            case 104 -> "WORKLOAD_CONCENTRATION";
            case 105 -> "UPDATE_GAP";
            default -> "RISK_REVIEW";
        };
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
                activity.at(),
                activity.updatedAt());
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

    private InvitationCreateResponse invitationResponse(long projectId, TeamProfile team) {
        return new InvitationCreateResponse(
                Math.abs((long) team.inviteCode().hashCode()),
                projectId,
                team.inviteCode(),
                invitationUrl(team.inviteCode()),
                defaultText(team.inviteExpiresAt(), LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS).toString()));
    }

    private String invitationUrl(String inviteCode) {
        return frontendPublicBaseUrl + "/invite/" + inviteCode;
    }

    private static String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_FRONTEND_PUBLIC_BASE_URL;
        }
        return value.trim().replaceAll("/+$", "");
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

    private MemberView memberByName(List<MemberView> members, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return members.stream()
                .filter(member -> member.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
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
