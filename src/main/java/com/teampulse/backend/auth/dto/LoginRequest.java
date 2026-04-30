package com.teampulse.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "\uC774\uBA54\uC77C\uC740 \uD544\uC218\uC785\uB2C8\uB2E4.")
        @Email(message = "\uC774\uBA54\uC77C \uD615\uC2DD\uC774 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.")
        @Size(max = 191, message = "\uC774\uBA54\uC77C\uC740 191\uC790 \uC774\uD558\uC5EC\uC57C \uD569\uB2C8\uB2E4.")
        String email,

        @NotBlank(message = "\uBE44\uBC00\uBC88\uD638\uB294 \uD544\uC218\uC785\uB2C8\uB2E4.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,20}$",
                message = "\uC601\uBB38, \uC22B\uC790, \uD2B9\uC218\uBB38\uC790\uB97C \uD3EC\uD568\uD55C 8~20\uC790\uC5EC\uC57C \uD569\uB2C8\uB2E4."
        )
        String password
) {
}
