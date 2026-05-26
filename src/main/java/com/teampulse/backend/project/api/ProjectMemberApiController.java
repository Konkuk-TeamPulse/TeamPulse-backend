package com.teampulse.backend.project.api;

import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.mobile.application.ProjectWorkspaceUseCase;
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
    public SpecResponse<Void> deleteMember(@PathVariable long projectId, @PathVariable long memberId) {
        projectWorkspaceUseCase.deleteProjectMember(projectId, memberId);
        return SpecResponse.ok(MEMBER_REMOVED_MESSAGE, null);
    }
}
