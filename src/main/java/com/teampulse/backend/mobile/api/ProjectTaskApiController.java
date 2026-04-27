package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.ApiResponse;
import com.teampulse.backend.mobile.application.MobileTaskUseCase;
import com.teampulse.backend.mobile.application.WorkspaceQueryUseCase;
import com.teampulse.backend.mobile.dto.CreateTaskRequest;
import com.teampulse.backend.mobile.dto.TaskDependencyRequest;
import com.teampulse.backend.mobile.dto.TaskView;
import com.teampulse.backend.mobile.dto.UpdateTaskRequest;
import com.teampulse.backend.mobile.dto.UpdateTaskStatusRequest;
import com.teampulse.backend.mobile.dto.WorkspaceState;
import jakarta.validation.Valid;
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

    private final WorkspaceQueryUseCase workspaceQueryUseCase;
    private final MobileTaskUseCase mobileTaskUseCase;

    public ProjectTaskApiController(WorkspaceQueryUseCase workspaceQueryUseCase, MobileTaskUseCase mobileTaskUseCase) {
        this.workspaceQueryUseCase = workspaceQueryUseCase;
        this.mobileTaskUseCase = mobileTaskUseCase;
    }

    @GetMapping
    public ApiResponse<List<TaskView>> listTasks(@PathVariable long projectId) {
        requireDemoProject(projectId);
        return ApiResponse.ok(workspaceQueryUseCase.getWorkspace().tasks());
    }

    @PostMapping
    public ApiResponse<WorkspaceState> createTask(
            @PathVariable long projectId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        requireDemoProject(projectId);
        return ApiResponse.ok(mobileTaskUseCase.createTask(request));
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

    private void requireDemoProject(long projectId) {
        if (projectId != DEMO_PROJECT_ID) {
            throw new IllegalArgumentException("Only demo project 1 is available in the MVP backend.");
        }
    }
}
