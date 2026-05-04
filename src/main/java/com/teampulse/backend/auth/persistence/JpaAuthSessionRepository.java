package com.teampulse.backend.auth.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAuthSessionRepository extends JpaRepository<AuthSessionEntity, Long> {

    Optional<AuthSessionEntity> findByRefreshToken(String refreshToken);

    Optional<AuthSessionEntity> findByAccessToken(String accessToken);

    void deleteByExpiresAtBefore(java.time.LocalDateTime expiresAt);
}
