package com.teampulse.backend.mobile.api;


import com.teampulse.backend.mobile.application.*;
import com.teampulse.backend.mobile.dto.*;
import com.teampulse.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/tasks")
public class MobileTaskController {

    private final MobileTaskUseCase mobileTaskUseCase;

    public MobileTaskController(MobileTaskUseCase mobileTaskUseCase) {
        this.mobileTaskUseCase = mobileTaskUseCase;
    }

    @PostMapping
    public ApiResponse<WorkspaceState> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.ok(mobileTaskUseCase.createTask(request));
    }

    @PatchMapping("/{taskId}/status")
    public ApiResponse<WorkspaceState> updateTaskStatus(
            @PathVariable long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return ApiResponse.ok(mobileTaskUseCase.updateTaskStatus(taskId, request));
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<WorkspaceState> deleteTask(@PathVariable long taskId) {
        return ApiResponse.ok(mobileTaskUseCase.deleteTask(taskId));
    }
}
