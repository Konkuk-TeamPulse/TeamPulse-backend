package com.teampulse.backend.mobile.api;

import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.mobile.application.MobileMemberUseCase;
import com.teampulse.backend.mobile.application.WorkspaceQueryUseCase;
import com.teampulse.backend.mobile.dto.CreateMemberRequest;
import com.teampulse.backend.mobile.dto.InvitationAcceptRequest;
import com.teampulse.backend.mobile.dto.InvitationAcceptResponse;
import com.teampulse.backend.mobile.dto.InvitationInfoResponse;
import com.teampulse.backend.mobile.dto.MemberView;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import org.springframework.security.core.Authentication;
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
    private static final String SUCCESS_MESSAGE = "\uC694\uCCAD\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4.";

    private final WorkspaceQueryUseCase workspaceQueryUseCase;
    private final MobileMemberUseCase mobileMemberUseCase;

    public InvitationApiController(WorkspaceQueryUseCase workspaceQueryUseCase, MobileMemberUseCase mobileMemberUseCase) {
        this.workspaceQueryUseCase = workspaceQueryUseCase;
        this.mobileMemberUseCase = mobileMemberUseCase;
    }

    @GetMapping("/{token}")
    public SpecResponse<InvitationInfoResponse> getInvitation(@PathVariable String token) {
        var workspace = workspaceQueryUseCase.getWorkspace();
        requireValidToken(token, workspace.team().inviteCode());
        return SpecResponse.ok(SUCCESS_MESSAGE, new InvitationInfoResponse(
                token,
                DEMO_PROJECT_ID,
                workspace.team().name(),
                workspace.team().courseName(),
                workspace.user().name(),
                expirationTimestamp(),
                false,
                false));
    }

    @PostMapping("/{token}/accept")
    public SpecResponse<InvitationAcceptResponse> acceptInvitation(
            @PathVariable String token,
            Authentication authentication,
            @Valid @RequestBody(required = false) InvitationAcceptRequest request
    ) {
        var workspace = workspaceQueryUseCase.getWorkspace();
        requireValidToken(token, workspace.team().inviteCode());
        var authUser = requireAuthUser(authentication);
        var memberName = defaultText(request == null ? null : request.name(), authUser.name());
        var role = request == null || request.role() == null ? TeamRole.MEMBER : request.role();
        var updatedWorkspace = mobileMemberUseCase.addMember(new CreateMemberRequest(memberName, role));
        var member = updatedWorkspace.members().stream()
                .filter(candidate -> candidate.name().equalsIgnoreCase(memberName))
                .max(Comparator.comparingLong(MemberView::id))
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        return SpecResponse.ok(SUCCESS_MESSAGE, new InvitationAcceptResponse(
                member.id(),
                DEMO_PROJECT_ID,
                updatedWorkspace.team().name(),
                authUser.id(),
                member.role(),
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()));
    }

    private void requireValidToken(String token, String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank() || !inviteCode.equals(token)) {
            throw new IllegalArgumentException("Invitation is invalid or expired.");
        }
    }

    private AuthUser requireAuthUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser) {
            return authUser;
        }
        throw new IllegalArgumentException("Authentication user is required.");
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String expirationTimestamp() {
        return LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
