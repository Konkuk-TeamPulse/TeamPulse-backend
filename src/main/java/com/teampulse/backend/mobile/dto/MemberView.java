package com.teampulse.backend.mobile.dto;

import com.teampulse.backend.domain.team.TeamRole;

public record MemberView(long id, String name, TeamRole role) {
}
