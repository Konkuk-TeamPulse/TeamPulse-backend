package com.teampulse.backend.auth.application;

import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.auth.dto.LoginRequest;
import com.teampulse.backend.auth.dto.LoginResponse;
import com.teampulse.backend.auth.dto.LogoutRequest;
import com.teampulse.backend.auth.dto.SignupRequest;
import com.teampulse.backend.auth.dto.SignupResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenRegistry refreshTokenRegistry;
    private final LoginAttemptGuard loginAttemptGuard;

    public AuthService(
            AuthUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenIssuer tokenIssuer,
            RefreshTokenRegistry refreshTokenRegistry,
            LoginAttemptGuard loginAttemptGuard) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokenRegistry = refreshTokenRegistry;
        this.loginAttemptGuard = loginAttemptGuard;
    }

    public SignupResponse signup(SignupRequest request) {
        var email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        var user = new AuthUser(
                userRepository.nextId(),
                email,
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                request.university().trim(),
                normalizeNullable(request.phone()));
        var saved = userRepository.save(user);
        return new SignupResponse(
                saved.id(),
                saved.email(),
                saved.name(),
                saved.university(),
                saved.phone(),
                tokenIssuer.issue(saved));
    }

    public LoginResponse login(LoginRequest request) {
        var email = normalizeEmail(request.email());
        loginAttemptGuard.assertAllowed(email);
        try {
            var user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AuthUserNotFoundException(email));
            if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
                throw new InvalidPasswordException();
            }
            loginAttemptGuard.recordSuccess(email);
            return new LoginResponse(user.id(), user.email(), tokenIssuer.issue(user));
        } catch (AuthUserNotFoundException | InvalidPasswordException exception) {
            loginAttemptGuard.recordFailure(email);
            throw exception;
        }
    }

    public void logout(LogoutRequest request) {
        var refreshToken = request.refreshToken().trim();
        if (!refreshTokenRegistry.isActive(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }
        refreshTokenRegistry.revoke(refreshToken);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeNullable(String value) {
        return value == null ? "" : value.trim();
    }
}
