package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.ApiResponse;
import com.teampulse.backend.mobile.application.MobileMemberUseCase;
import com.teampulse.backend.mobile.application.WorkspaceQueryUseCase;
import com.teampulse.backend.mobile.dto.CreateMemberRequest;
import com.teampulse.backend.mobile.dto.WorkspaceState;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invitations")
public class InvitationApiController {

    private static final long DEMO_PROJECT_ID = 1L;

    private final WorkspaceQueryUseCase workspaceQueryUseCase;
    private final MobileMemberUseCase mobileMemberUseCase;

    public InvitationApiController(WorkspaceQueryUseCase workspaceQueryUseCase, MobileMemberUseCase mobileMemberUseCase) {
        this.workspaceQueryUseCase = workspaceQueryUseCase;
        this.mobileMemberUseCase = mobileMemberUseCase;
    }

    @GetMapping("/{token}")
    public ApiResponse<Map<String, Object>> getInvitation(@PathVariable String token) {
        var workspace = workspaceQueryUseCase.getWorkspace();
        requireValidToken(token, workspace.team().inviteCode());
        return ApiResponse.ok(Map.of(
                "projectId", DEMO_PROJECT_ID,
                "teamName", workspace.team().name(),
                "courseName", workspace.team().courseName(),
                "token", token
        ));
    }

    @PostMapping("/{token}/accept")
    public ApiResponse<WorkspaceState> acceptInvitation(
            @PathVariable String token,
            @Valid @RequestBody CreateMemberRequest request
    ) {
        var workspace = workspaceQueryUseCase.getWorkspace();
        requireValidToken(token, workspace.team().inviteCode());
        return ApiResponse.ok(mobileMemberUseCase.addMember(request));
    }

    private void requireValidToken(String token, String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank() || !inviteCode.equals(token)) {
            throw new IllegalArgumentException("Invitation is invalid or expired.");
        }
    }
}
