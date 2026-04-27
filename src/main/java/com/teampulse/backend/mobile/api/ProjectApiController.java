package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.ApiResponse;
import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.mobile.application.MobileMemberUseCase;
import com.teampulse.backend.mobile.application.MobileReportUseCase;
import com.teampulse.backend.mobile.application.MobileTeamUseCase;
import com.teampulse.backend.mobile.application.WorkspaceLifecycleUseCase;
import com.teampulse.backend.mobile.application.WorkspaceQueryUseCase;
import com.teampulse.backend.mobile.dto.ActivityView;
import com.teampulse.backend.mobile.dto.BootstrapWorkspaceRequest;
import com.teampulse.backend.mobile.dto.CreateMemberRequest;
import com.teampulse.backend.mobile.dto.MemberView;
import com.teampulse.backend.mobile.dto.ReportView;
import com.teampulse.backend.mobile.dto.RiskView;
import com.teampulse.backend.mobile.dto.TeamProfile;
import com.teampulse.backend.mobile.dto.UpdateTeamRequest;
import com.teampulse.backend.mobile.dto.UserProfile;
import com.teampulse.backend.mobile.dto.WorkspaceState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
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

    private final WorkspaceQueryUseCase workspaceQueryUseCase;
    private final WorkspaceLifecycleUseCase workspaceLifecycleUseCase;
    private final MobileTeamUseCase mobileTeamUseCase;
    private final MobileMemberUseCase mobileMemberUseCase;
    private final MobileReportUseCase mobileReportUseCase;

    public ProjectApiController(
            WorkspaceQueryUseCase workspaceQueryUseCase,
            WorkspaceLifecycleUseCase workspaceLifecycleUseCase,
            MobileTeamUseCase mobileTeamUseCase,
            MobileMemberUseCase mobileMemberUseCase,
            MobileReportUseCase mobileReportUseCase
    ) {
        this.workspaceQueryUseCase = workspaceQueryUseCase;
        this.workspaceLifecycleUseCase = workspaceLifecycleUseCase;
        this.mobileTeamUseCase = mobileTeamUseCase;
        this.mobileMemberUseCase = mobileMemberUseCase;
        this.mobileReportUseCase = mobileReportUseCase;
    }

    @GetMapping("/account")
    public ApiResponse<UserProfile> getAccount() {
        return ApiResponse.ok(workspaceQueryUseCase.getWorkspace().user());
    }

    @PostMapping("/projects")
    public ApiResponse<WorkspaceState> createProject(@Valid @RequestBody ProjectRequest request) {
        return ApiResponse.ok(workspaceLifecycleUseCase.bootstrap(new BootstrapWorkspaceRequest(
                request.ownerName(),
                request.ownerEmail(),
                request.name(),
                request.courseName(),
                request.semester(),
                request.dueDate()
        )));
    }

    @GetMapping("/projects")
    public ApiResponse<List<ProjectSummary>> listProjects() {
        var workspace = workspaceQueryUseCase.getWorkspace();
        if (!workspace.initialized()) {
            return ApiResponse.ok(List.of());
        }
        return ApiResponse.ok(List.of(summary(workspace)));
    }

    @GetMapping("/projects/{projectId}")
    public ApiResponse<WorkspaceState> getProject(@PathVariable long projectId) {
        requireDemoProject(projectId);
        return ApiResponse.ok(workspaceQueryUseCase.getWorkspace());
    }

    @PatchMapping("/projects/{projectId}")
    public ApiResponse<WorkspaceState> updateProject(
            @PathVariable long projectId,
            @Valid @RequestBody UpdateTeamRequest request
    ) {
        requireDemoProject(projectId);
        return ApiResponse.ok(mobileTeamUseCase.updateTeam(request));
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

    private ProjectSummary summary(WorkspaceState workspace) {
        return new ProjectSummary(
                DEMO_PROJECT_ID,
                workspace.team().name(),
                workspace.team().courseName(),
                workspace.team().semester(),
                workspace.team().dueDate(),
                workspace.members().size(),
                workspace.tasks().size()
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

    private void requireDemoProject(long projectId) {
        if (projectId != DEMO_PROJECT_ID) {
            throw new IllegalArgumentException("Only demo project 1 is available in the MVP backend.");
        }
    }

    public record ProjectRequest(
            @NotBlank(message = "Project name is required.")
            @Size(max = 80, message = "Project name must be 80 characters or fewer.")
            String name,
            @NotBlank(message = "Course name is required.")
            @Size(max = 80, message = "Course name must be 80 characters or fewer.")
            String courseName,
            String semester,
            @NotBlank(message = "Due date is required.")
            @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Due date must use yyyy-MM-dd.")
            String dueDate,
            @NotBlank(message = "Owner name is required.")
            String ownerName,
            @NotBlank(message = "Owner email is required.")
            @Email(message = "Owner email must be valid.")
            String ownerEmail
    ) {
    }

    public record ProjectSummary(
            long id,
            String name,
            String courseName,
            String semester,
            String dueDate,
            int memberCount,
            int taskCount
    ) {
    }
}
