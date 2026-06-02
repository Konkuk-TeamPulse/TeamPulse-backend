package com.teampulse.backend.project.api;

import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.workspace.application.ProjectWorkspaceUseCase;
import com.teampulse.backend.workspace.dto.MemberView;
import com.teampulse.backend.workspace.dto.WorkspaceState;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberApiController {

    private static final String MEMBER_REMOVED_MESSAGE = "\uD300\uC6D0\uC774 \uC81C\uC678\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";

    private final ProjectWorkspaceUseCase projectWorkspaceUseCase;

    public ProjectMemberApiController(ProjectWorkspaceUseCase projectWorkspaceUseCase) {
        this.projectWorkspaceUseCase = projectWorkspaceUseCase;
    }

    @DeleteMapping("/{memberId}")
    public SpecResponse<Void> deleteMember(
            @PathVariable long projectId,
            @PathVariable long memberId,
            Authentication authentication
    ) {
        var workspace = projectWorkspaceUseCase.getProjectWorkspace(projectId);
        var authUser = requireProjectLeader(workspace, authentication);
        var target = workspace.members().stream()
                .filter(member -> member.id() == memberId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        if (sameMember(target, authUser) || isProjectOwnerMember(workspace, target, authUser)) {
            throw new IllegalArgumentException("Project leaders cannot remove themselves.");
        }
        projectWorkspaceUseCase.deleteProjectMember(projectId, memberId);
        return SpecResponse.ok(MEMBER_REMOVED_MESSAGE, null);
    }

    private AuthUser requireProjectLeader(WorkspaceState workspace, Authentication authentication) {
        var authUser = requireAuthUser(authentication);
        if (isProjectOwner(workspace, authUser) || hasLeaderMembership(workspace, authUser)) {
            return authUser;
        }
        throw new AccessDeniedException("Only project leaders can remove team members.");
    }

    private AuthUser requireAuthUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser) {
            return authUser;
        }
        throw new AccessDeniedException("Authentication user is required.");
    }

    private boolean isProjectOwner(WorkspaceState workspace, AuthUser authUser) {
        return !authUser.email().isBlank()
                && workspace.user().email().equalsIgnoreCase(authUser.email());
    }

    private boolean hasLeaderMembership(WorkspaceState workspace, AuthUser authUser) {
        return workspace.members().stream()
                .filter(member -> member.role() == TeamRole.LEADER)
                .anyMatch(member -> sameMember(member, authUser));
    }

    private boolean sameMember(MemberView member, AuthUser authUser) {
        return !member.email().isBlank()
                && !authUser.email().isBlank()
                && member.email().equalsIgnoreCase(authUser.email());
    }

    private boolean isProjectOwnerMember(WorkspaceState workspace, MemberView member, AuthUser authUser) {
        return isProjectOwner(workspace, authUser)
                && member.role() == TeamRole.LEADER
                && member.name().equalsIgnoreCase(workspace.user().name());
    }
}
