package com.teampulse.backend.workspace.dto;

public record InvitationCreateResponse(
        long invitationId,
        long projectId,
        String inviteCode,
        String inviteUrl,
        String expiredAt
) {
}
