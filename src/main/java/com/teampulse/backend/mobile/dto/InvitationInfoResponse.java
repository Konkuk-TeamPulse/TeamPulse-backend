package com.teampulse.backend.mobile.dto;

public record InvitationInfoResponse(
        String inviteCode,
        long projectId,
        String projectName,
        String subject,
        String teamLeaderName,
        String expiredAt,
        boolean isExpired,
        boolean isAlreadyJoined
) {
}
