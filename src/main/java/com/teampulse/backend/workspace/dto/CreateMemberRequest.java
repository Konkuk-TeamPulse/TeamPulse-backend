package com.teampulse.backend.workspace.dto;

import com.teampulse.backend.domain.team.TeamRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMemberRequest(
        @NotBlank(message = "Member name is required.")
        @Size(max = 50, message = "Member name must be 50 characters or fewer.")
        String name,
        TeamRole role
) {
}
