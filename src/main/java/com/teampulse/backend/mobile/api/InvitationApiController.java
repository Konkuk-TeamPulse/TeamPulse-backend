package com.teampulse.backend.mobile.api;

import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.mobile.application.MobileInvitationUseCase;
import com.teampulse.backend.domain.team.TeamRole;
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

    private static final String SUCCESS_MESSAGE = "\uC694\uCCAD\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4.";

    private final MobileInvitationUseCase mobileInvitationUseCase;

    public InvitationApiController(MobileInvitationUseCase mobileInvitationUseCase) {
        this.mobileInvitationUseCase = mobileInvitationUseCase;
    }

    @GetMapping("/{token}")
    public SpecResponse<InvitationInfoResponse> getInvitation(@PathVariable String token) {
        var workspace = mobileInvitationUseCase.getWorkspaceByInviteCode(token);
        return SpecResponse.ok(SUCCESS_MESSAGE, new InvitationInfoResponse(
                token,
                workspace.projectId(),
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
        var authUser = requireAuthUser(authentication);
        var memberName = defaultText(request == null ? null : request.name(), authUser.name());
        var role = request == null || request.role() == null ? TeamRole.MEMBER : request.role();
        var updatedWorkspace = mobileInvitationUseCase.acceptInvitation(token, memberName, authUser.email(), role);
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
