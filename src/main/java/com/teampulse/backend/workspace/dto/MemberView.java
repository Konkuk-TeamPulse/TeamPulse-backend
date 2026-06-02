package com.teampulse.backend.workspace.dto;

import com.teampulse.backend.domain.team.TeamRole;

public record MemberView(long id, String name, String email, TeamRole role) {
    public MemberView(long id, String name, TeamRole role) {
        this(id, name, "", role);
    }
}
