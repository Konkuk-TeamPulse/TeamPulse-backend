package com.teampulse.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "Refresh Token\uC740 \uD544\uC218\uC785\uB2C8\uB2E4.")
        String refreshToken
) {
}
