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

    // Deprecated: 프론트엔드는 프로젝트 기반 조회 API GET /api/projects 와 GET /api/projects/{projectId} 를 사용합니다.
    @Deprecated
    @GetMapping
    public ApiResponse<WorkspaceState> getWorkspace() {
        return ApiResponse.ok(workspaceQueryUseCase.getWorkspace());
    }

    // Deprecated: 프론트엔드는 프로젝트 생성 API POST /api/projects 를 사용합니다.
    @Deprecated
    @PostMapping("/bootstrap")
    public ApiResponse<WorkspaceState> bootstrap(@Valid @RequestBody BootstrapWorkspaceRequest request) {
        return ApiResponse.ok(workspaceLifecycleUseCase.bootstrap(request));
    }

    // Deprecated: 프론트엔드는 별도 서버 리셋 API 없이 클라이언트 상태 초기화와 GET /api/projects 를 사용합니다.
    @Deprecated
    @PostMapping("/reset")
    public ApiResponse<WorkspaceState> reset() {
        return ApiResponse.ok(workspaceLifecycleUseCase.reset());
    }

    // Deprecated: 프론트엔드는 샘플 로드 API 대신 프로젝트 생성 API POST /api/projects 를 사용합니다.
    @Deprecated
    @PostMapping("/sample")
    public ApiResponse<WorkspaceState> sample() {
        return ApiResponse.ok(workspaceLifecycleUseCase.loadSample());
    }
}
