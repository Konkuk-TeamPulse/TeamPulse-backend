package com.teampulse.backend.auth.infrastructure;

import com.teampulse.backend.auth.application.AccessTokenVerifier;
import com.teampulse.backend.auth.application.RefreshTokenRegistry;
import com.teampulse.backend.auth.application.TokenIssuer;
import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.auth.dto.JwtInfo;
import com.teampulse.backend.auth.persistence.AuthSessionEntity;
import com.teampulse.backend.auth.persistence.AuthUserEntity;
import com.teampulse.backend.auth.persistence.JpaAuthSessionRepository;
import com.teampulse.backend.auth.persistence.JpaAuthUserEntityRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("mysql")
@Transactional
public class JpaTokenIssuer implements TokenIssuer, RefreshTokenRegistry, AccessTokenVerifier {

    private static final int SESSION_DAYS = 14;

    private final JpaAuthUserEntityRepository userRepository;
    private final JpaAuthSessionRepository sessionRepository;

    public JpaTokenIssuer(
            JpaAuthUserEntityRepository userRepository,
            JpaAuthSessionRepository sessionRepository
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public JwtInfo issue(AuthUser user) {
        var entity = userRepository.findById(user.id())
                .or(() -> userRepository.findByEmail(user.email()))
                .orElseThrow(() -> new IllegalArgumentException("Auth user not found."));
        var accessToken = "Bearer db-access-" + UUID.randomUUID();
        var refreshToken = "Bearer db-refresh-" + UUID.randomUUID();

        var session = new AuthSessionEntity();
        session.setUser(entity);
        session.setAccessToken(accessToken);
        session.setRefreshToken(refreshToken);
        session.setExpiresAt(LocalDateTime.now().plusDays(SESSION_DAYS));
        sessionRepository.save(session);

        return new JwtInfo(accessToken, refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(String refreshToken) {
        return sessionRepository.findByRefreshToken(refreshToken)
                .filter(this::isUsable)
                .isPresent();
    }

    @Override
    public void revoke(String refreshToken) {
        sessionRepository.findByRefreshToken(refreshToken)
                .ifPresent(session -> {
                    session.setRevoked(true);
                    sessionRepository.save(session);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveAccessToken(String accessToken) {
        return sessionRepository.findByAccessToken(accessToken)
                .filter(this::isUsable)
                .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUser> findUserByAccessToken(String accessToken) {
        return sessionRepository.findByAccessToken(accessToken)
                .filter(this::isUsable)
                .map(AuthSessionEntity::getUser)
                .map(this::toDomain);
    }

    private boolean isUsable(AuthSessionEntity session) {
        return !session.isRevoked() && session.getExpiresAt().isAfter(LocalDateTime.now());
    }

    private AuthUser toDomain(AuthUserEntity entity) {
        return new AuthUser(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getName(),
                entity.getUniversity(),
                entity.getPhone());
    }
}
