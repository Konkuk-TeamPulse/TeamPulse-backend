package com.teampulse.backend.workspace.dto;

import com.teampulse.backend.domain.team.TeamRole;
import jakarta.validation.constraints.Size;

public record InvitationAcceptRequest(
        @Size(max = 50, message = "Member name must be 50 characters or fewer.")
        String name,
        TeamRole role
) {
}
