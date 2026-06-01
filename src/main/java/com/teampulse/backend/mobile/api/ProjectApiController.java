package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.ApiResponse;
import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.mobile.application.ProjectWorkspaceUseCase;
import com.teampulse.backend.mobile.dto.ActivityView;
import com.teampulse.backend.mobile.dto.ActivityLogSpecResponse;
import com.teampulse.backend.mobile.dto.BootstrapWorkspaceRequest;
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
import com.teampulse.backend.mobile.dto.RiskView;
import com.teampulse.backend.mobile.dto.TaskView;
import com.teampulse.backend.mobile.dto.TeamProfile;
import com.teampulse.backend.mobile.dto.UpdateTeamRequest;
import com.teampulse.backend.mobile.dto.UserMeResponse;
import com.teampulse.backend.mobile.dto.UserProfile;
import com.teampulse.backend.mobile.dto.WorkspaceState;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
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

    private static final String REPORT_FONT_RESOURCE = "/fonts/malgun.ttf";
    private static final String DEFAULT_OWNER_NAME = "Demo Leader";
    private static final String DEFAULT_OWNER_EMAIL = "leader@teampulse.app";
    private static final String DEFAULT_SEMESTER = "2026-1";
    private static final String SUCCESS_MESSAGE = "\uC694\uCCAD\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4.";
    private static final String PROJECT_CREATED_MESSAGE = "\uD504\uB85C\uC81D\uD2B8\uAC00 \uC0DD\uC131\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
    private static final String REPORT_CREATED_MESSAGE = "\uB9AC\uD3EC\uD2B8\uAC00 \uC0DD\uC131\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
    private static final String DEFAULT_FRONTEND_PUBLIC_BASE_URL = "https://team-pulse-frontend.vercel.app";
    private static final Color REPORT_INK = new Color(17, 24, 39);
    private static final Color REPORT_MUTED = new Color(107, 114, 128);
    private static final Color REPORT_LINE = new Color(226, 232, 240);
    private static final Color REPORT_PANEL = new Color(248, 250, 252);
    private static final Color REPORT_BRAND = new Color(37, 99, 235);
    private static final Color REPORT_BRAND_DARK = new Color(15, 23, 42);
    private static final Color REPORT_WARNING = new Color(180, 83, 9);
    private static final Color REPORT_DANGER = new Color(185, 28, 28);

    private final ProjectWorkspaceUseCase projectWorkspaceUseCase;
    private final String frontendPublicBaseUrl;

    public ProjectApiController(
            ProjectWorkspaceUseCase projectWorkspaceUseCase,
            @Value("${app.frontend.public-base-url:" + DEFAULT_FRONTEND_PUBLIC_BASE_URL + "}") String frontendPublicBaseUrl
    ) {
        this.projectWorkspaceUseCase = projectWorkspaceUseCase;
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

    @PostMapping("/projects/{projectId}/invitations")
    public SpecResponse<InvitationCreateResponse> createInvitation(
            @PathVariable long projectId,
            Authentication authentication
    ) {
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
        var authUser = requireAuthUser(authentication);
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
                LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS).toString());
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

    private byte[] reportPdf(WorkspaceState workspace, ReportView report) throws IOException {
        try {
            var output = new ByteArrayOutputStream();
            var document = new Document(PageSize.A4, 36, 36, 38, 42);
            PdfWriter.getInstance(document, output);
            document.open();

            var baseFont = koreanBaseFont();
            var titleFont = pdfFont(baseFont, 22, Font.BOLD, Color.WHITE);
            var subtitleFont = pdfFont(baseFont, 9, Font.NORMAL, new Color(203, 213, 225));
            var sectionFont = pdfFont(baseFont, 13, Font.BOLD, REPORT_INK);
            var bodyFont = pdfFont(baseFont, 9, Font.NORMAL, REPORT_INK);
            var smallFont = pdfFont(baseFont, 8, Font.NORMAL, REPORT_MUTED);
            var labelFont = pdfFont(baseFont, 8, Font.BOLD, REPORT_MUTED);
            var valueFont = pdfFont(baseFont, 11, Font.BOLD, REPORT_INK);

            addReportHeader(document, workspace, report, titleFont, subtitleFont);
            addProjectSnapshot(document, workspace, report, labelFont, bodyFont);
            addMetricGrid(document, workspace, labelFont, valueFont, smallFont);
            addMembers(document, workspace, sectionFont, bodyFont, smallFont);
            addTaskTable(document, workspace, sectionFont, bodyFont, smallFont, labelFont);
            addMeetingSection(document, workspace, sectionFont, bodyFont, smallFont, labelFont);
            addRiskSection(document, workspace, sectionFont, bodyFont, smallFont, labelFont);
            addActivityTable(document, workspace, sectionFont, bodyFont, smallFont, labelFont);

            document.close();
            return output.toByteArray();
        } catch (DocumentException exception) {
            throw new IOException("Failed to create report PDF.", exception);
        }
    }

    private Font pdfFont(BaseFont baseFont, float size, int style, Color color) {
        return new Font(baseFont, size, style, color);
    }

    private void addReportHeader(
            Document document,
            WorkspaceState workspace,
            ReportView report,
            Font titleFont,
            Font subtitleFont
    ) throws DocumentException {
        var table = fullWidthTable(1);
        var cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(REPORT_BRAND_DARK);
        cell.setPaddingTop(22);
        cell.setPaddingRight(22);
        cell.setPaddingBottom(20);
        cell.setPaddingLeft(22);

        var title = new Paragraph("TeamPulse Report", titleFont);
        title.setSpacingAfter(8);
        cell.addElement(title);
        cell.addElement(new Paragraph(textOrDash(workspace.team().name()) + " / " + textOrDash(workspace.team().courseName()), subtitleFont));
        cell.addElement(new Paragraph("Generated " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                + " / Range " + textOrDash(report.range())
                + " / Status " + textOrDash(report.status()), subtitleFont));
        table.addCell(cell);
        table.setSpacingAfter(14);
        document.add(table);
    }

    private void addProjectSnapshot(
            Document document,
            WorkspaceState workspace,
            ReportView report,
            Font labelFont,
            Font bodyFont
    ) throws DocumentException {
        var table = fullWidthTable(4);
        table.setWidths(new float[]{1.1f, 2.2f, 1.1f, 2.2f});
        addInfoCell(table, "Project", labelFont);
        addInfoCell(table, textOrDash(workspace.team().name()), bodyFont);
        addInfoCell(table, "Subject", labelFont);
        addInfoCell(table, textOrDash(workspace.team().courseName()), bodyFont);
        addInfoCell(table, "Schedule", labelFont);
        addInfoCell(table, textOrDash(workspace.team().startDate()) + " ~ " + textOrDash(workspace.team().dueDate()), bodyFont);
        addInfoCell(table, "Report", labelFont);
        addInfoCell(table, textOrDash(report.range()), bodyFont);
        table.setSpacingAfter(14);
        document.add(table);
    }

    private void addMetricGrid(
            Document document,
            WorkspaceState workspace,
            Font labelFont,
            Font valueFont,
            Font smallFont
    ) throws DocumentException {
        var table = fullWidthTable(4);
        table.setWidths(new float[]{1, 1, 1, 1});
        addMetricCell(table, "Tasks", String.valueOf(workspace.tasks().size()),
                "TODO " + countTasksByStatus(workspace, TaskStatus.TODO)
                        + " / DOING " + countTasksByStatus(workspace, TaskStatus.DOING)
                        + " / DONE " + countTasksByStatus(workspace, TaskStatus.DONE), labelFont, valueFont, smallFont);
        addMetricCell(table, "Overdue", String.valueOf(countOverdueTasks(workspace)), "unfinished tasks past due", labelFont, valueFont, smallFont);
        addMetricCell(table, "Meetings", String.valueOf(workspace.meetings().size()), "recorded notes", labelFont, valueFont, smallFont);
        addMetricCell(table, "Risks", String.valueOf(workspace.risks().size()), "active signals", labelFont, valueFont, smallFont);
        table.setSpacingAfter(16);
        document.add(table);
    }

    private void addMembers(Document document, WorkspaceState workspace, Font sectionFont, Font bodyFont, Font smallFont) throws DocumentException {
        addSectionTitle(document, "Members", sectionFont);
        var members = workspace.members().isEmpty()
                ? "No members recorded."
                : workspace.members().stream()
                .map(member -> member.name() + " (" + member.role() + ")")
                .collect(java.util.stream.Collectors.joining("   "));
        var paragraph = new Paragraph(members, workspace.members().isEmpty() ? smallFont : bodyFont);
        paragraph.setSpacingAfter(12);
        document.add(paragraph);
    }

    private void addTaskTable(
            Document document,
            WorkspaceState workspace,
            Font sectionFont,
            Font bodyFont,
            Font smallFont,
            Font labelFont
    ) throws DocumentException {
        addSectionTitle(document, "Task Details", sectionFont);
        if (workspace.tasks().isEmpty()) {
            addEmptyState(document, "No tasks recorded.", smallFont);
            return;
        }

        var table = fullWidthTable(5);
        table.setWidths(new float[]{1.1f, 3.4f, 1.6f, 1.3f, 1.2f});
        addHeaderCell(table, "Status", labelFont);
        addHeaderCell(table, "Task", labelFont);
        addHeaderCell(table, "Owner", labelFont);
        addHeaderCell(table, "Due", labelFont);
        addHeaderCell(table, "Priority", labelFont);

        workspace.tasks().stream()
                .sorted(Comparator.comparing(TaskView::dueDate))
                .limit(12)
                .forEach(task -> {
                    addBodyCell(table, task.status().name(), bodyFont);
                    addBodyCell(table, taskTitle(task), bodyFont);
                    addBodyCell(table, textOrDash(task.owner()), bodyFont);
                    addBodyCell(table, textOrDash(task.dueDate()), bodyFont);
                    addBodyCell(table, textOrDash(task.priority()), bodyFont);
                });
        table.setSpacingAfter(14);
        document.add(table);
    }

    private void addMeetingSection(
            Document document,
            WorkspaceState workspace,
            Font sectionFont,
            Font bodyFont,
            Font smallFont,
            Font labelFont
    ) throws DocumentException {
        addSectionTitle(document, "Meeting Notes", sectionFont);
        if (workspace.meetings().isEmpty()) {
            addEmptyState(document, "No meetings recorded.", smallFont);
            return;
        }

        for (var meeting : workspace.meetings().stream()
                .sorted(Comparator.comparing(MeetingView::time).reversed())
                .limit(4)
                .toList()) {
            var card = cardTable();
            var cell = cardCell();
            cell.addElement(new Paragraph(textOrDash(meeting.time()) + "  /  " + textOrDash(meeting.title()), bodyFont));
            cell.addElement(new Paragraph("Agenda: " + textOrDash(meeting.agenda()), smallFont));
            if (!meeting.decisions().isEmpty()) {
                cell.addElement(new Paragraph("Decisions: " + String.join(", ", meeting.decisions()), smallFont));
            }
            if (!meeting.actionItems().isEmpty()) {
                cell.addElement(new Paragraph("Action items: " + meeting.actionItems().stream()
                        .map(MeetingActionItemView::content)
                        .collect(java.util.stream.Collectors.joining(", ")), smallFont));
            }
            card.addCell(cell);
            card.setSpacingAfter(8);
            document.add(card);
        }
    }

    private void addRiskSection(
            Document document,
            WorkspaceState workspace,
            Font sectionFont,
            Font bodyFont,
            Font smallFont,
            Font labelFont
    ) throws DocumentException {
        addSectionTitle(document, "Risk Signals", sectionFont);
        if (workspace.risks().isEmpty()) {
            addEmptyState(document, "No active risk signals.", smallFont);
            return;
        }

        for (var risk : workspace.risks().stream().limit(5).toList()) {
            var card = cardTable();
            var cell = cardCell();
            cell.setBorderColor(riskColor(risk));
            cell.setBorderWidthLeft(4);
            cell.addElement(new Paragraph("[" + risk.severity() + "] " + textOrDash(risk.title()), bodyFont));
            cell.addElement(new Paragraph("Basis: " + textOrDash(risk.body()), smallFont));
            cell.addElement(new Paragraph("Suggested action: " + textOrDash(risk.action()), smallFont));
            card.addCell(cell);
            card.setSpacingAfter(8);
            document.add(card);
        }
    }

    private void addActivityTable(
            Document document,
            WorkspaceState workspace,
            Font sectionFont,
            Font bodyFont,
            Font smallFont,
            Font labelFont
    ) throws DocumentException {
        addSectionTitle(document, "Recent Activity", sectionFont);
        if (workspace.activities().isEmpty()) {
            addEmptyState(document, "No activity logs recorded.", smallFont);
            return;
        }

        var table = fullWidthTable(3);
        table.setWidths(new float[]{1.5f, 1.4f, 4.1f});
        addHeaderCell(table, "Time", labelFont);
        addHeaderCell(table, "Actor", labelFont);
        addHeaderCell(table, "Summary", labelFont);
        workspace.activities().stream()
                .limit(8)
                .forEach(activity -> {
                    addBodyCell(table, textOrDash(activity.at()), smallFont);
                    addBodyCell(table, textOrDash(activity.actor()), bodyFont);
                    addBodyCell(table, textOrDash(activity.summary()), bodyFont);
                });
        document.add(table);
    }

    private PdfPTable fullWidthTable(int columns) {
        var table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        return table;
    }

    private PdfPTable cardTable() {
        return fullWidthTable(1);
    }

    private PdfPCell cardCell() {
        var cell = new PdfPCell();
        cell.setBackgroundColor(REPORT_PANEL);
        cell.setBorderColor(REPORT_LINE);
        cell.setPadding(10);
        return cell;
    }

    private void addSectionTitle(Document document, String title, Font font) throws DocumentException {
        var paragraph = new Paragraph(title, font);
        paragraph.setSpacingBefore(4);
        paragraph.setSpacingAfter(7);
        document.add(paragraph);
    }

    private void addEmptyState(Document document, String text, Font font) throws DocumentException {
        var paragraph = new Paragraph(text, font);
        paragraph.setSpacingAfter(12);
        document.add(paragraph);
    }

    private void addInfoCell(PdfPTable table, String text, Font font) {
        var cell = baseCell(text, font);
        cell.setBackgroundColor(REPORT_PANEL);
        table.addCell(cell);
    }

    private void addMetricCell(PdfPTable table, String label, String value, String helper, Font labelFont, Font valueFont, Font smallFont) {
        var cell = new PdfPCell();
        cell.setBorderColor(REPORT_LINE);
        cell.setBackgroundColor(Color.WHITE);
        cell.setPadding(11);
        var labelParagraph = new Paragraph(label, labelFont);
        labelParagraph.setSpacingAfter(5);
        var valueParagraph = new Paragraph(value, valueFont);
        valueParagraph.setSpacingAfter(4);
        cell.addElement(labelParagraph);
        cell.addElement(valueParagraph);
        cell.addElement(new Paragraph(helper, smallFont));
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        var cell = baseCell(text, font);
        cell.setBackgroundColor(new Color(241, 245, 249));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font) {
        table.addCell(baseCell(text, font));
    }

    private PdfPCell baseCell(String text, Font font) {
        var cell = new PdfPCell(new Phrase(textOrDash(text), font));
        cell.setBorderColor(REPORT_LINE);
        cell.setPaddingTop(7);
        cell.setPaddingRight(8);
        cell.setPaddingBottom(7);
        cell.setPaddingLeft(8);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private String taskTitle(TaskView task) {
        var details = new java.util.ArrayList<String>();
        details.add("#" + task.id() + " " + textOrDash(task.title()));
        if (!task.blockers().isEmpty()) {
            details.add("Blockers: " + String.join(", ", task.blockers()));
        }
        if (task.note() != null && !task.note().isBlank()) {
            details.add("Note: " + task.note());
        }
        return String.join("\n", details);
    }

    private Color riskColor(RiskView risk) {
        return switch (risk.severity()) {
            case CRITICAL -> REPORT_DANGER;
            case WARNING -> REPORT_WARNING;
            case INFO -> REPORT_BRAND;
        };
    }

    private BaseFont koreanBaseFont() throws IOException, DocumentException {
        try (var fontStream = ProjectApiController.class.getResourceAsStream(REPORT_FONT_RESOURCE)) {
            if (fontStream == null) {
                throw new IOException("Report font resource not found: " + REPORT_FONT_RESOURCE);
            }
            return BaseFont.createFont(
                    "malgun.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,
                    fontStream.readAllBytes(),
                    null);
        }
    }

    private void addPdfParagraph(Document document, String text, Font font) throws DocumentException {
        document.add(new Paragraph(text == null ? "" : text, font));
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
