package com.teampulse.backend.mobile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @Size(max = 50, message = "Name must be 50 characters or fewer.")
        String name,
        @Email(message = "Email must be valid.")
        @Size(max = 191, message = "Email must be 191 characters or fewer.")
        String email,
        @Size(max = 80, message = "University must be 80 characters or fewer.")
        String university,
        @Size(max = 30, message = "Phone must be 30 characters or fewer.")
        String phone
) {
}
