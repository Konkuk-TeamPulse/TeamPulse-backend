package com.teampulse.backend.auth.api;

import com.teampulse.backend.auth.application.AuthService;
import com.teampulse.backend.auth.dto.LoginRequest;
import com.teampulse.backend.auth.dto.LoginResponse;
import com.teampulse.backend.auth.dto.LogoutRequest;
import com.teampulse.backend.auth.dto.JwtInfo;
import com.teampulse.backend.auth.dto.RefreshTokenRequest;
import com.teampulse.backend.auth.dto.SignupRequest;
import com.teampulse.backend.auth.dto.SignupResponse;
import com.teampulse.backend.common.api.SpecResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("authApiController")
@RequestMapping("/api/auth")
public class AuthApiController {

    private static final String SUCCESS_MESSAGE = "\uC694\uCCAD\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4.";

    private final AuthService authService;

    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public SpecResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return SpecResponse.ok(SUCCESS_MESSAGE, authService.signup(request));
    }

    @PostMapping("/login")
    public SpecResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return SpecResponse.ok(SUCCESS_MESSAGE, authService.login(request));
    }

    @PostMapping("/logout")
    public SpecResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return SpecResponse.ok(SUCCESS_MESSAGE, null);
    }

    @PostMapping("/refresh")
    public SpecResponse<JwtInfo> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return SpecResponse.ok(SUCCESS_MESSAGE, authService.refresh(request));
    }
}
