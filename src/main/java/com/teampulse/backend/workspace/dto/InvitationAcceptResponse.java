package com.teampulse.backend.workspace.dto;

import com.teampulse.backend.domain.team.TeamRole;

public record InvitationAcceptResponse(
        long memberId,
        long projectId,
        String projectName,
        long userId,
        TeamRole role,
        String joinedAt
) {
}
