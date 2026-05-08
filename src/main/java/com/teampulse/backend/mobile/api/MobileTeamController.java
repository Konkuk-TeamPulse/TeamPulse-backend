package com.teampulse.backend.mobile.api;


import com.teampulse.backend.mobile.application.*;
import com.teampulse.backend.mobile.dto.*;
import com.teampulse.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/team")
public class MobileTeamController {

    private final MobileTeamUseCase mobileTeamUseCase;

    public MobileTeamController(MobileTeamUseCase mobileTeamUseCase) {
        this.mobileTeamUseCase = mobileTeamUseCase;
    }

    // Deprecated: 프론트엔드는 프로젝트 수정 API PATCH /api/projects/{projectId} 를 사용합니다.
    @Deprecated
    @PatchMapping
    public ApiResponse<WorkspaceState> updateTeam(@Valid @RequestBody UpdateTeamRequest request) {
        return ApiResponse.ok(mobileTeamUseCase.updateTeam(request));
    }

    // Deprecated: 프론트엔드는 초대 생성 API POST /api/projects/{projectId}/invitations 를 사용합니다.
    @Deprecated
    @PostMapping("/regenerate-invite")
    public ApiResponse<WorkspaceState> regenerateInvite() {
        return ApiResponse.ok(mobileTeamUseCase.regenerateInviteCode());
    }
}
