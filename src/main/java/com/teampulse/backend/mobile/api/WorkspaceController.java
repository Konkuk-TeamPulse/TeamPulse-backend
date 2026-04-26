package com.teampulse.backend.mobile.api;


import com.teampulse.backend.mobile.application.*;
import com.teampulse.backend.mobile.dto.*;
import com.teampulse.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/workspace")
public class WorkspaceController {

    private final WorkspaceQueryUseCase workspaceQueryUseCase;
    private final WorkspaceLifecycleUseCase workspaceLifecycleUseCase;

    public WorkspaceController(
            WorkspaceQueryUseCase workspaceQueryUseCase,
            WorkspaceLifecycleUseCase workspaceLifecycleUseCase
    ) {
        this.workspaceQueryUseCase = workspaceQueryUseCase;
        this.workspaceLifecycleUseCase = workspaceLifecycleUseCase;
    }

    @GetMapping
    public ApiResponse<WorkspaceState> getWorkspace() {
        return ApiResponse.ok(workspaceQueryUseCase.getWorkspace());
    }

    @PostMapping("/bootstrap")
    public ApiResponse<WorkspaceState> bootstrap(@Valid @RequestBody BootstrapWorkspaceRequest request) {
        return ApiResponse.ok(workspaceLifecycleUseCase.bootstrap(request));
    }

    @PostMapping("/reset")
    public ApiResponse<WorkspaceState> reset() {
        return ApiResponse.ok(workspaceLifecycleUseCase.reset());
    }

    @PostMapping("/sample")
    public ApiResponse<WorkspaceState> sample() {
        return ApiResponse.ok(workspaceLifecycleUseCase.loadSample());
    }
}
