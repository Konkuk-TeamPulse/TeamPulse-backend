package com.teampulse.backend.mobile.api;


import com.teampulse.backend.mobile.application.*;
import com.teampulse.backend.mobile.dto.*;
import com.teampulse.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/members")
public class MobileMemberController {

    private final MobileMemberUseCase mobileMemberUseCase;

    public MobileMemberController(MobileMemberUseCase mobileMemberUseCase) {
        this.mobileMemberUseCase = mobileMemberUseCase;
    }

    // Deprecated: 프론트엔드는 프로젝트 멤버 API GET /api/projects/{projectId}/members 와 DELETE /api/projects/{projectId}/members/me 를 사용합니다.
    @Deprecated
    @PostMapping
    public ApiResponse<WorkspaceState> addMember(@Valid @RequestBody CreateMemberRequest request) {
        return ApiResponse.ok(mobileMemberUseCase.addMember(request));
    }

    // Deprecated: 프론트엔드는 현재 사용자 탈퇴 API DELETE /api/projects/{projectId}/members/me 를 사용합니다.
    @Deprecated
    @DeleteMapping("/{memberId}")
    public ApiResponse<WorkspaceState> deleteMember(@PathVariable long memberId) {
        return ApiResponse.ok(mobileMemberUseCase.deleteMember(memberId));
    }
}
