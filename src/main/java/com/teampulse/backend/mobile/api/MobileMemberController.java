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

    @PostMapping
    public ApiResponse<WorkspaceState> addMember(@Valid @RequestBody CreateMemberRequest request) {
        return ApiResponse.ok(mobileMemberUseCase.addMember(request));
    }

    @DeleteMapping("/{memberId}")
    public ApiResponse<WorkspaceState> deleteMember(@PathVariable long memberId) {
        return ApiResponse.ok(mobileMemberUseCase.deleteMember(memberId));
    }
}
