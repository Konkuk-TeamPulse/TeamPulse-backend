package com.teampulse.backend.workspace.api;

import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.workspace.application.InvitationAttemptGuard;
import com.teampulse.backend.workspace.application.WorkspaceInvitationUseCase;
import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.workspace.dto.InvitationAcceptRequest;
import com.teampulse.backend.workspace.dto.InvitationAcceptResponse;
import com.teampulse.backend.workspace.dto.InvitationInfoResponse;
import com.teampulse.backend.workspace.dto.MemberView;
import com.teampulse.backend.workspace.dto.WorkspaceState;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import org.springframework.security.access.AccessDeniedException;
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

    private static final String SUCCESS_MESSAGE = "\uC694\uCCAD\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4.";

    private final WorkspaceInvitationUseCase workspaceInvitationUseCase;
    private final InvitationAttemptGuard invitationAttemptGuard;

    public InvitationApiController(
            WorkspaceInvitationUseCase workspaceInvitationUseCase,
            InvitationAttemptGuard invitationAttemptGuard
    ) {
        this.workspaceInvitationUseCase = workspaceInvitationUseCase;
        this.invitationAttemptGuard = invitationAttemptGuard;
    }

    @GetMapping("/{token}")
    public SpecResponse<InvitationInfoResponse> getInvitation(@PathVariable String token) {
        invitationAttemptGuard.assertAllowed(token);
        try {
            var workspace = workspaceInvitationUseCase.getWorkspaceByInviteCode(token);
            invitationAttemptGuard.recordSuccess(token);
            return SpecResponse.ok(SUCCESS_MESSAGE, new InvitationInfoResponse(
                    token,
                    workspace.projectId(),
                    workspace.team().name(),
                    workspace.team().courseName(),
                    workspace.user().name(),
                    workspace.team().inviteExpiresAt(),
                    false,
                    false));
        } catch (IllegalArgumentException exception) {
            invitationAttemptGuard.recordFailure(token);
            throw exception;
        }
    }

    @PostMapping("/{token}/accept")
    public SpecResponse<InvitationAcceptResponse> acceptInvitation(
            @PathVariable String token,
            Authentication authentication,
            @Valid @RequestBody(required = false) InvitationAcceptRequest request
    ) {
        var authUser = requireAuthUser(authentication);
        var memberName = defaultText(request == null ? null : request.name(), authUser.name());
        invitationAttemptGuard.assertAllowed(token);
        var updatedWorkspace = acceptInvitation(token, memberName, authUser);
        var member = updatedWorkspace.members().stream()
                .filter(candidate -> candidate.email().equalsIgnoreCase(authUser.email()))
                .findFirst()
                .or(() -> updatedWorkspace.members().stream()
                        .filter(candidate -> candidate.name().equalsIgnoreCase(memberName))
                        .max(Comparator.comparingLong(MemberView::id)))
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        return SpecResponse.ok(SUCCESS_MESSAGE, new InvitationAcceptResponse(
                member.id(),
                updatedWorkspace.projectId(),
                updatedWorkspace.team().name(),
                authUser.id(),
                member.role(),
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()));
    }

    private WorkspaceState acceptInvitation(String token, String memberName, AuthUser authUser) {
        try {
            var updatedWorkspace = workspaceInvitationUseCase.acceptInvitation(token, memberName, authUser.email(), TeamRole.MEMBER);
            invitationAttemptGuard.recordSuccess(token);
            return updatedWorkspace;
        } catch (IllegalArgumentException exception) {
            invitationAttemptGuard.recordFailure(token);
            throw exception;
        }
    }

    private AuthUser requireAuthUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser) {
            return authUser;
        }
        throw new AccessDeniedException("Authentication user is required.");
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

}
