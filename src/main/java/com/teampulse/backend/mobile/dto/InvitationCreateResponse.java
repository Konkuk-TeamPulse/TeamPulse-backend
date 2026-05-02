package com.teampulse.backend.mobile.dto;

public record InvitationCreateResponse(
        long invitationId,
        long projectId,
        String inviteCode,
        String inviteUrl,
        String expiredAt
) {
}
