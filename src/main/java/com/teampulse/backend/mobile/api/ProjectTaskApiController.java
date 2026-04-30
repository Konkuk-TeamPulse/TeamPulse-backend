package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.ApiResponse;
import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.mobile.application.MobileTaskUseCase;
import com.teampulse.backend.mobile.application.WorkspaceQueryUseCase;
import com.teampulse.backend.mobile.dto.CreateTaskRequest;
import com.teampulse.backend.mobile.dto.MemberView;
import com.teampulse.backend.mobile.dto.TaskCreateSpecRequest;
import com.teampulse.backend.mobile.dto.TaskCreateSpecResponse;
import com.teampulse.backend.mobile.dto.TaskDependencyRequest;
import com.teampulse.backend.mobile.dto.TaskSummarySpecResponse;
import com.teampulse.backend.mobile.dto.TaskView;
import com.teampulse.backend.mobile.dto.UpdateTaskRequest;
import com.teampulse.backend.mobile.dto.UpdateTaskStatusRequest;
import com.teampulse.backend.mobile.dto.WorkspaceState;
import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
public class ProjectTaskApiController {

    private static final long DEMO_PROJECT_ID = 1L;
    private static final String SUCCESS_MESSAGE = "\uC694\uCCAD\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4.";
    private static final String TASK_CREATED_MESSAGE = "\uD0DC\uC2A4\uD06C\uAC00 \uC0DD\uC131\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";

    private final WorkspaceQueryUseCase workspaceQueryUseCase;
    private final MobileTaskUseCase mobileTaskUseCase;

    public ProjectTaskApiController(WorkspaceQueryUseCase workspaceQueryUseCase, MobileTaskUseCase mobileTaskUseCase) {
        this.workspaceQueryUseCase = workspaceQueryUseCase;
        this.mobileTaskUseCase = mobileTaskUseCase;
    }

    @GetMapping
    public SpecResponse<List<TaskSummarySpecResponse>> listTasks(@PathVariable long projectId) {
        requireDemoProject(projectId);
        var tasks = workspaceQueryUseCase.getWorkspace().tasks().stream()
                .map(task -> new TaskSummarySpecResponse(
                        task.id(),
                        task.title(),
                        task.status(),
                        task.owner(),
                        task.dueDate()))
                .toList();
        return SpecResponse.ok(SUCCESS_MESSAGE, tasks);
    }

    @PostMapping
    public SpecResponse<TaskCreateSpecResponse> createTask(
            @PathVariable long projectId,
            @Valid @RequestBody TaskCreateSpecRequest request
    ) {
        requireDemoProject(projectId);
        var workspace = workspaceQueryUseCase.getWorkspace();
        var assignee = requireMemberById(workspace, request.assigneeId());
        var updated = mobileTaskUseCase.createTask(new CreateTaskRequest(
                request.title(),
                assignee.name(),
                request.dueDate(),
                List.of(),
                normalizeNullable(request.description())));
        var task = latestTask(updated);
        return SpecResponse.ok(TASK_CREATED_MESSAGE, new TaskCreateSpecResponse(
                task.id(),
                task.title(),
                task.status(),
                assignee.id(),
                task.dueDate()));
    }

    @PatchMapping("/{taskId}")
    public ApiResponse<WorkspaceState> updateTask(
            @PathVariable long projectId,
            @PathVariable long taskId,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        requireDemoProject(projectId);
        return ApiResponse.ok(mobileTaskUseCase.updateTask(taskId, request));
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<WorkspaceState> deleteTask(@PathVariable long projectId, @PathVariable long taskId) {
        requireDemoProject(projectId);
        return ApiResponse.ok(mobileTaskUseCase.deleteTask(taskId));
    }

    @PatchMapping("/{taskId}/status")
    public ApiResponse<WorkspaceState> updateTaskStatus(
            @PathVariable long projectId,
            @PathVariable long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        requireDemoProject(projectId);
        return ApiResponse.ok(mobileTaskUseCase.updateTaskStatus(taskId, request));
    }

    @PostMapping("/{taskId}/dependencies")
    public ApiResponse<WorkspaceState> addDependency(
            @PathVariable long projectId,
            @PathVariable long taskId,
            @Valid @RequestBody TaskDependencyRequest request
    ) {
        requireDemoProject(projectId);
        return ApiResponse.ok(mobileTaskUseCase.addTaskDependency(taskId, request));
    }

    @DeleteMapping("/{taskId}/dependencies/{dependencyTitle}")
    public ApiResponse<WorkspaceState> deleteDependency(
            @PathVariable long projectId,
            @PathVariable long taskId,
            @PathVariable String dependencyTitle
    ) {
        requireDemoProject(projectId);
        return ApiResponse.ok(mobileTaskUseCase.deleteTaskDependency(taskId, dependencyTitle));
    }

    private MemberView requireMemberById(WorkspaceState workspace, long memberId) {
        return workspace.members().stream()
                .filter(member -> member.id() == memberId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Assignee not found."));
    }

    private TaskView latestTask(WorkspaceState workspace) {
        return workspace.tasks().stream()
                .max(Comparator.comparingLong(TaskView::id))
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
    }

    private String normalizeNullable(String value) {
        return value == null ? "" : value.trim();
    }

    private void requireDemoProject(long projectId) {
        if (projectId != DEMO_PROJECT_ID) {
            throw new IllegalArgumentException("Only demo project 1 is available in the MVP backend.");
        }
    }
}
