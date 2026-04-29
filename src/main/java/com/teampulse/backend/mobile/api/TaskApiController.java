package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.mobile.application.MobileTaskUseCase;
import com.teampulse.backend.mobile.application.WorkspaceQueryUseCase;
import com.teampulse.backend.mobile.dto.MemberView;
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

    private final WorkspaceQueryUseCase workspaceQueryUseCase;
    private final MobileTaskUseCase mobileTaskUseCase;

    public TaskApiController(WorkspaceQueryUseCase workspaceQueryUseCase, MobileTaskUseCase mobileTaskUseCase) {
        this.workspaceQueryUseCase = workspaceQueryUseCase;
        this.mobileTaskUseCase = mobileTaskUseCase;
    }

    @PatchMapping("/{taskId}")
    public SpecResponse<TaskUpdateSpecResponse> updateTask(
            @PathVariable long taskId,
            @Valid @RequestBody TaskUpdateSpecRequest request
    ) {
        var owner = request.assigneeId() == null ? null : requireMemberById(workspaceQueryUseCase.getWorkspace(), request.assigneeId()).name();
        var updated = mobileTaskUseCase.updateTask(taskId, new UpdateTaskRequest(
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
        mobileTaskUseCase.deleteTask(taskId);
        return SpecResponse.ok(TASK_DELETED_MESSAGE, null);
    }

    @PatchMapping("/{taskId}/status")
    public SpecResponse<TaskStatusSpecResponse> updateTaskStatus(
            @PathVariable long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        var updated = mobileTaskUseCase.updateTaskStatus(taskId, request);
        var task = requireTaskById(updated, taskId);
        return SpecResponse.ok(TASK_STATUS_UPDATED_MESSAGE, new TaskStatusSpecResponse(task.id(), task.status()));
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
}
