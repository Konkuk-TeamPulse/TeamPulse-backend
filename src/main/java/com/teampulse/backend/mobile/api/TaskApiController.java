package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.mobile.application.ProjectWorkspaceUseCase;
import com.teampulse.backend.mobile.dto.MemberView;
import com.teampulse.backend.mobile.dto.TaskDependencyRequest;
import com.teampulse.backend.mobile.dto.TaskDependencySpecRequest;
import com.teampulse.backend.mobile.dto.TaskDependencySpecResponse;
import com.teampulse.backend.mobile.dto.TaskStatusSpecResponse;
import com.teampulse.backend.mobile.dto.TaskUpdateSpecRequest;
import com.teampulse.backend.mobile.dto.TaskUpdateSpecResponse;
import com.teampulse.backend.mobile.dto.TaskView;
import com.teampulse.backend.mobile.dto.UpdateTaskRequest;
import com.teampulse.backend.mobile.dto.UpdateTaskStatusRequest;
import com.teampulse.backend.mobile.dto.WorkspaceState;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskApiController {

    private static final String SUCCESS_MESSAGE = "\uC694\uCCAD\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4.";
    private static final String TASK_UPDATED_MESSAGE = "\uD0DC\uC2A4\uD06C\uAC00 \uC218\uC815\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
    private static final String TASK_DELETED_MESSAGE = "\uD0DC\uC2A4\uD06C\uAC00 \uC0AD\uC81C\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
    private static final String TASK_STATUS_UPDATED_MESSAGE = "\uD0DC\uC2A4\uD06C \uC0C1\uD0DC\uAC00 \uBCC0\uACBD\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
    private static final String TASK_DEPENDENCY_ADDED_MESSAGE = "\uD0DC\uC2A4\uD06C \uC758\uC874\uAD00\uACC4\uAC00 \uCD94\uAC00\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
    private static final String TASK_DEPENDENCY_DELETED_MESSAGE = "\uD0DC\uC2A4\uD06C \uC758\uC874\uAD00\uACC4\uAC00 \uC0AD\uC81C\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";

    private final ProjectWorkspaceUseCase projectWorkspaceUseCase;

    public TaskApiController(ProjectWorkspaceUseCase projectWorkspaceUseCase) {
        this.projectWorkspaceUseCase = projectWorkspaceUseCase;
    }

    @PatchMapping("/{taskId}")
    public SpecResponse<TaskUpdateSpecResponse> updateTask(
            @PathVariable long taskId,
            @Valid @RequestBody TaskUpdateSpecRequest request
    ) {
        var workspace = projectWorkspaceUseCase.getProjectWorkspaceByTaskId(taskId);
        var owner = request.assigneeId() == null ? null : requireMemberById(workspace, request.assigneeId()).name();
        var updated = projectWorkspaceUseCase.updateTaskById(taskId, new UpdateTaskRequest(
                request.title(),
                owner,
                null,
                request.dueDate(),
                null,
                null,
                null,
                request.description()));
        var task = requireTaskById(updated, taskId);
        return SpecResponse.ok(TASK_UPDATED_MESSAGE, new TaskUpdateSpecResponse(
                task.id(),
                task.title(),
                task.note(),
                task.dueDate()));
    }

    @DeleteMapping("/{taskId}")
    public SpecResponse<Void> deleteTask(@PathVariable long taskId) {
        projectWorkspaceUseCase.deleteTaskById(taskId);
        return SpecResponse.ok(TASK_DELETED_MESSAGE, null);
    }

    @PatchMapping("/{taskId}/status")
    public SpecResponse<TaskStatusSpecResponse> updateTaskStatus(
            @PathVariable long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        var updated = projectWorkspaceUseCase.updateTaskStatusById(taskId, request);
        var task = requireTaskById(updated, taskId);
        return SpecResponse.ok(TASK_STATUS_UPDATED_MESSAGE, new TaskStatusSpecResponse(task.id(), task.status()));
    }

    @PostMapping("/{taskId}/dependencies")
    public SpecResponse<TaskDependencySpecResponse> addDependency(
            @PathVariable long taskId,
            @Valid @RequestBody TaskDependencySpecRequest request
    ) {
        var workspace = projectWorkspaceUseCase.getProjectWorkspaceByTaskId(taskId);
        var task = requireTaskById(workspace, taskId);
        var precedingTask = requireTaskById(workspace, request.precedingTaskId());
        if (task.id() == precedingTask.id()) {
            throw new IllegalArgumentException("Task cannot depend on itself.");
        }
        projectWorkspaceUseCase.addTaskDependencyById(taskId, new TaskDependencyRequest(precedingTask.title()));
        return SpecResponse.ok(TASK_DEPENDENCY_ADDED_MESSAGE, new TaskDependencySpecResponse(taskId, precedingTask.id()));
    }

    @DeleteMapping("/{taskId}/dependencies/{dependencyId}")
    public SpecResponse<Void> deleteDependency(@PathVariable long taskId, @PathVariable long dependencyId) {
        var workspace = projectWorkspaceUseCase.getProjectWorkspaceByTaskId(taskId);
        var task = requireTaskById(workspace, taskId);
        var precedingTask = requireTaskById(workspace, dependencyId);
        if (!hasBlocker(task, precedingTask)) {
            throw new IllegalArgumentException("Task dependency not found.");
        }
        projectWorkspaceUseCase.deleteTaskDependencyById(taskId, precedingTask.title());
        return SpecResponse.ok(TASK_DEPENDENCY_DELETED_MESSAGE, null);
    }

    private MemberView requireMemberById(WorkspaceState workspace, long memberId) {
        return workspace.members().stream()
                .filter(member -> member.id() == memberId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Assignee not found."));
    }

    private TaskView requireTaskById(WorkspaceState workspace, long taskId) {
        return workspace.tasks().stream()
                .filter(task -> task.id() == taskId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
    }

    private boolean hasBlocker(TaskView task, TaskView precedingTask) {
        return task.blockers().stream()
                .anyMatch(blocker -> blocker.equalsIgnoreCase(precedingTask.title()));
    }
}
