package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.ApiResponse;
import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.mobile.application.MobileAccountUseCase;
import com.teampulse.backend.mobile.application.MobileMemberUseCase;
import com.teampulse.backend.mobile.application.MobileReportUseCase;
import com.teampulse.backend.mobile.application.MobileTeamUseCase;
import com.teampulse.backend.mobile.application.WorkspaceLifecycleUseCase;
import com.teampulse.backend.mobile.application.WorkspaceQueryUseCase;
import com.teampulse.backend.mobile.dto.ActivityView;
import com.teampulse.backend.mobile.dto.BootstrapWorkspaceRequest;
import com.teampulse.backend.mobile.dto.CreateMemberRequest;
import com.teampulse.backend.mobile.dto.MemberView;
import com.teampulse.backend.mobile.dto.ProjectCreateRequest;
import com.teampulse.backend.mobile.dto.ProjectCreateResponse;
import com.teampulse.backend.mobile.dto.ProjectDetailView;
import com.teampulse.backend.mobile.dto.ProjectSummaryView;
import com.teampulse.backend.mobile.dto.ProjectUpdateRequest;
import com.teampulse.backend.mobile.dto.ProjectUpdateResponse;
import com.teampulse.backend.mobile.dto.ReportView;
import com.teampulse.backend.mobile.dto.RiskActionOption;
import com.teampulse.backend.mobile.dto.RiskView;
import com.teampulse.backend.mobile.dto.TaskView;
import com.teampulse.backend.mobile.dto.TeamProfile;
import com.teampulse.backend.mobile.dto.UpdateAccountRequest;
import com.teampulse.backend.mobile.dto.UpdateTeamRequest;
import com.teampulse.backend.mobile.dto.UserProfile;
import com.teampulse.backend.mobile.dto.WorkspaceState;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private static final long DEMO_PROJECT_ID = 1L;
    private static final String DEFAULT_OWNER_NAME = "Demo Leader";
    private static final String DEFAULT_OWNER_EMAIL = "leader@teampulse.app";
    private static final String DEFAULT_SEMESTER = "2026-1";
    private static final String SUCCESS_MESSAGE = "\uC694\uCCAD\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4.";
    private static final String PROJECT_CREATED_MESSAGE = "\uD504\uB85C\uC81D\uD2B8\uAC00 \uC0DD\uC131\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";

    private final WorkspaceQueryUseCase workspaceQueryUseCase;
    private final WorkspaceLifecycleUseCase workspaceLifecycleUseCase;
    private final MobileAccountUseCase mobileAccountUseCase;
    private final MobileTeamUseCase mobileTeamUseCase;
    private final MobileMemberUseCase mobileMemberUseCase;
    private final MobileReportUseCase mobileReportUseCase;

    public ProjectApiController(
            WorkspaceQueryUseCase workspaceQueryUseCase,
            WorkspaceLifecycleUseCase workspaceLifecycleUseCase,
            MobileAccountUseCase mobileAccountUseCase,
            MobileTeamUseCase mobileTeamUseCase,
            MobileMemberUseCase mobileMemberUseCase,
            MobileReportUseCase mobileReportUseCase
    ) {
        this.workspaceQueryUseCase = workspaceQueryUseCase;
        this.workspaceLifecycleUseCase = workspaceLifecycleUseCase;
        this.mobileAccountUseCase = mobileAccountUseCase;
        this.mobileTeamUseCase = mobileTeamUseCase;
        this.mobileMemberUseCase = mobileMemberUseCase;
        this.mobileReportUseCase = mobileReportUseCase;
    }

    @GetMapping("/account")
    public ApiResponse<UserProfile> getAccount() {
        return ApiResponse.ok(workspaceQueryUseCase.getWorkspace().user());
    }

    @PatchMapping("/account")
    public ApiResponse<UserProfile> updateAccount(@Valid @RequestBody UpdateAccountRequest request) {
        return ApiResponse.ok(mobileAccountUseCase.updateAccount(request).user());
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
    public SpecResponse<ProjectCreateResponse> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        var currentUser = workspaceQueryUseCase.getWorkspace().user();
        var workspace = workspaceLifecycleUseCase.bootstrap(new BootstrapWorkspaceRequest(
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
                DEMO_PROJECT_ID,
                workspace.team().name(),
                TeamRole.LEADER.name()
        ));
    }

    @GetMapping("/projects")
    public SpecResponse<List<ProjectSummaryView>> listProjects() {
        var workspace = workspaceQueryUseCase.getWorkspace();
        if (!workspace.initialized()) {
            return SpecResponse.ok(SUCCESS_MESSAGE, List.of());
        }
        return SpecResponse.ok(SUCCESS_MESSAGE, List.of(projectSummary(workspace)));
    }

    @GetMapping("/projects/{projectId}")
    public SpecResponse<ProjectDetailView> getProject(@PathVariable long projectId) {
        requireDemoProject(projectId);
        return SpecResponse.ok(SUCCESS_MESSAGE, projectDetail(workspaceQueryUseCase.getWorkspace()));
    }

    @PatchMapping("/projects/{projectId}")
    public SpecResponse<ProjectUpdateResponse> updateProject(
            @PathVariable long projectId,
            @Valid @RequestBody ProjectUpdateRequest request
    ) {
        requireDemoProject(projectId);
        var workspace = mobileTeamUseCase.updateTeam(new UpdateTeamRequest(
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
    public ApiResponse<WorkspaceState> getDashboard(@PathVariable long projectId) {
        requireDemoProject(projectId);
        return ApiResponse.ok(workspaceQueryUseCase.getWorkspace());
    }

    @GetMapping("/projects/{projectId}/members")
    public ApiResponse<List<MemberView>> listMembers(@PathVariable long projectId) {
        requireDemoProject(projectId);
        return ApiResponse.ok(workspaceQueryUseCase.getWorkspace().members());
    }

    @PostMapping("/projects/{projectId}/members")
    public ApiResponse<WorkspaceState> addMember(
            @PathVariable long projectId,
            @Valid @RequestBody CreateMemberRequest request
    ) {
        requireDemoProject(projectId);
        return ApiResponse.ok(mobileMemberUseCase.addMember(request));
    }

    @DeleteMapping("/projects/{projectId}/members/me")
    public ApiResponse<WorkspaceState> leaveProject(@PathVariable long projectId) {
        requireDemoProject(projectId);
        var workspace = workspaceQueryUseCase.getWorkspace();
        var currentUser = workspace.user().name();
        var target = workspace.members().stream()
                .filter(member -> member.name().equalsIgnoreCase(currentUser))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Current user is not a team member."));
        return ApiResponse.ok(mobileMemberUseCase.deleteMember(target.id()));
    }

    @DeleteMapping("/projects/{projectId}/members/{memberId}")
    public ApiResponse<WorkspaceState> deleteMember(@PathVariable long projectId, @PathVariable long memberId) {
        requireDemoProject(projectId);
        return ApiResponse.ok(mobileMemberUseCase.deleteMember(memberId));
    }

    @PostMapping("/projects/{projectId}/invite-links")
    public ApiResponse<Map<String, Object>> createInviteLink(@PathVariable long projectId) {
        requireDemoProject(projectId);
        var workspace = mobileTeamUseCase.regenerateInviteCode();
        return ApiResponse.ok(invitePayload(workspace.team()));
    }

    @GetMapping("/projects/{projectId}/activities")
    public ApiResponse<List<ActivityView>> listActivities(@PathVariable long projectId) {
        requireDemoProject(projectId);
        return ApiResponse.ok(workspaceQueryUseCase.getWorkspace().activities());
    }

    @GetMapping("/projects/{projectId}/risks")
    public ApiResponse<List<RiskView>> listRisks(@PathVariable long projectId) {
        requireDemoProject(projectId);
        return ApiResponse.ok(workspaceQueryUseCase.getWorkspace().risks());
    }

    @GetMapping("/projects/{projectId}/risks/{riskId}/actions")
    public ApiResponse<List<RiskActionOption>> listRiskActions(@PathVariable long projectId, @PathVariable long riskId) {
        requireDemoProject(projectId);
        var workspace = workspaceQueryUseCase.getWorkspace();
        var risk = workspace.risks().stream()
                .filter(candidate -> candidate.id() == riskId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Risk not found."));
        return ApiResponse.ok(riskActions(risk, workspace));
    }

    @PostMapping("/projects/{projectId}/reports")
    public ApiResponse<WorkspaceState> createReport(@PathVariable long projectId) {
        requireDemoProject(projectId);
        return ApiResponse.ok(mobileReportUseCase.generateReport());
    }

    @GetMapping("/projects/{projectId}/reports/{reportId}/download")
    public ResponseEntity<byte[]> downloadReport(@PathVariable long projectId, @PathVariable long reportId) {
        requireDemoProject(projectId);
        var workspace = workspaceQueryUseCase.getWorkspace();
        var report = workspace.reports().stream()
                .filter(candidate -> candidate.id() == reportId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Report not found."));
        var body = """
                TeamPulse Report
                Project: %s
                Range: %s
                Status: %s

                Tasks: %d
                Meetings: %d
                Risks: %d
                """.formatted(
                workspace.team().name(),
                report.range(),
                report.status(),
                workspace.tasks().size(),
                workspace.meetings().size(),
                workspace.risks().size()
        ).getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("teampulse-report-" + reportId + ".txt")
                        .build()
                        .toString())
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    private ProjectSummaryView projectSummary(WorkspaceState workspace) {
        return new ProjectSummaryView(
                DEMO_PROJECT_ID,
                workspace.team().name(),
                workspace.team().courseName(),
                TeamRole.LEADER.name(),
                workspace.team().dueDate()
        );
    }

    private ProjectDetailView projectDetail(WorkspaceState workspace) {
        return new ProjectDetailView(
                DEMO_PROJECT_ID,
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
                DEMO_PROJECT_ID,
                workspace.team().name(),
                workspace.team().courseName(),
                workspace.team().description(),
                workspace.team().startDate(),
                workspace.team().dueDate(),
                LocalDateTime.now().toString()
        );
    }

    private Map<String, Object> invitePayload(TeamProfile team) {
        return Map.of(
                "projectId", DEMO_PROJECT_ID,
                "token", team.inviteCode(),
                "inviteCode", team.inviteCode(),
                "url", "/invite/" + team.inviteCode()
        );
    }

    private List<RiskActionOption> riskActions(RiskView risk, WorkspaceState workspace) {
        var overdueOrDueSoon = firstOpenTaskByDueDate(workspace.tasks());
        var reassignmentTarget = firstOpenTaskByOwnerLoad(workspace.tasks());
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

    private void requireDemoProject(long projectId) {
        if (projectId != DEMO_PROJECT_ID) {
            throw new IllegalArgumentException("Only demo project 1 is available in the MVP backend.");
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null ? "" : value.trim();
    }
}
