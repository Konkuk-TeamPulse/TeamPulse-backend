package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    @PostMapping("/signup")
    public ApiResponse<Map<String, Object>> signup(@Valid @RequestBody AuthRequest request) {
        return ApiResponse.ok(session("SIGNED_UP", request.email(), displayName(request)));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody AuthRequest request) {
        return ApiResponse.ok(session("LOGGED_IN", request.email(), displayName(request)));
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Object>> logout() {
        return ApiResponse.ok(Map.of("status", "LOGGED_OUT"));
    }

    private Map<String, Object> session(String status, String email, String name) {
        return Map.of(
                "status", status,
                "tokenType", "DEMO",
                "accessToken", "demo-token",
                "user", Map.of("name", name, "email", email)
        );
    }

    private String displayName(AuthRequest request) {
        if (request.name() != null && !request.name().isBlank()) {
            return request.name().trim();
        }
        return request.email().split("@")[0];
    }

    public record AuthRequest(
            String name,
            @NotBlank(message = "Email is required.")
            @Email(message = "Email must be valid.")
            String email,
            @NotBlank(message = "Password is required.")
            String password
    ) {
    }
}
