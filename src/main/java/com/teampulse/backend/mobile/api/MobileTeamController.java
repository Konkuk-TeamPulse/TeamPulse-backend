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

    @PatchMapping
    public ApiResponse<WorkspaceState> updateTeam(@Valid @RequestBody UpdateTeamRequest request) {
        return ApiResponse.ok(mobileTeamUseCase.updateTeam(request));
    }

    @PostMapping("/regenerate-invite")
    public ApiResponse<WorkspaceState> regenerateInvite() {
        return ApiResponse.ok(mobileTeamUseCase.regenerateInviteCode());
    }
}
