package com.teampulse.backend.workspace.dto;

import com.teampulse.backend.domain.team.TeamRole;

public record MemberSpecResponse(
        long memberId,
        String name,
        String email,
        TeamRole role
) {
}
