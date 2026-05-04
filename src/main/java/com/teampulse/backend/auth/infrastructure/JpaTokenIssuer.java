package com.teampulse.backend.auth.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teampulse.backend.auth.application.AccessTokenVerifier;
import com.teampulse.backend.auth.application.InvalidRefreshTokenException;
import com.teampulse.backend.auth.application.RefreshTokenRegistry;
import com.teampulse.backend.auth.application.TokenIssuer;
import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.auth.dto.JwtInfo;
import com.teampulse.backend.auth.persistence.AuthSessionEntity;
import com.teampulse.backend.auth.persistence.AuthUserEntity;
import com.teampulse.backend.auth.persistence.JpaAuthSessionRepository;
import com.teampulse.backend.auth.persistence.JpaAuthUserEntityRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"mysql", "prod"})
@Transactional
public class JpaTokenIssuer implements TokenIssuer, RefreshTokenRegistry, AccessTokenVerifier {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {
    };

    private final JpaAuthUserEntityRepository userRepository;
    private final JpaAuthSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;
    private final byte[] jwtSecret;
    private final long accessTokenSeconds;
    private final int refreshTokenDays;

    public JpaTokenIssuer(
            JpaAuthUserEntityRepository userRepository,
            JpaAuthSessionRepository sessionRepository,
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret:local-development-secret-change-before-production-please}") String jwtSecret,
            @Value("${app.jwt.access-token-minutes:15}") long accessTokenMinutes,
            @Value("${app.jwt.refresh-token-days:14}") int refreshTokenDays
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.objectMapper = objectMapper;
        this.jwtSecret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSeconds = Math.max(1, accessTokenMinutes) * 60;
        this.refreshTokenDays = Math.max(1, refreshTokenDays);
    }

    @Override
    public JwtInfo issue(AuthUser user) {
        var entity = userRepository.findById(user.id())
                .or(() -> userRepository.findByEmail(user.email()))
                .orElseThrow(() -> new IllegalArgumentException("Auth user not found."));
        return issueForEntity(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(String refreshToken) {
        return sessionRepository.findByRefreshToken(tokenHash(refreshToken))
                .filter(this::isUsable)
                .isPresent();
    }

    @Override
    public void revoke(String refreshToken) {
        sessionRepository.findByRefreshToken(tokenHash(refreshToken))
                .ifPresent(session -> {
                    session.setRevoked(true);
                    sessionRepository.save(session);
                });
    }

    @Override
    public JwtInfo rotate(String refreshToken) {
        var session = sessionRepository.findByRefreshToken(tokenHash(refreshToken))
                .filter(this::isUsable)
                .orElseThrow(InvalidRefreshTokenException::new);
        session.setRevoked(true);
        sessionRepository.save(session);
        return issueForEntity(session.getUser());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveAccessToken(String accessToken) {
        return sessionRepository.findByAccessToken(tokenHash(accessToken))
                .filter(this::isUsable)
                .filter(session -> verifyClaims(accessToken).isPresent())
                .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUser> findUserByAccessToken(String accessToken) {
        var claims = verifyClaims(accessToken);
        if (claims.isEmpty()) {
            return Optional.empty();
        }
        return sessionRepository.findByAccessToken(tokenHash(accessToken))
                .filter(this::isUsable)
                .flatMap(session -> userId(claims.get()).flatMap(userRepository::findById))
                .map(this::toDomain);
    }

    private JwtInfo issueForEntity(AuthUserEntity entity) {
        sessionRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        var accessToken = bearer(jwt(entity));
        var refreshToken = bearer("refresh-" + randomToken());

        var session = new AuthSessionEntity();
        session.setUser(entity);
        session.setAccessToken(tokenHash(accessToken));
        session.setRefreshToken(tokenHash(refreshToken));
        session.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenDays));
        sessionRepository.save(session);

        return new JwtInfo(accessToken, refreshToken);
    }

    private String jwt(AuthUserEntity entity) {
        var now = Instant.now();
        var header = Map.of("alg", "HS256", "typ", "JWT");
        var claims = new LinkedHashMap<String, Object>();
        claims.put("sub", String.valueOf(entity.getId()));
        claims.put("email", entity.getEmail());
        claims.put("name", entity.getName());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plusSeconds(accessTokenSeconds).getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());

        var unsigned = base64Json(header) + "." + base64Json(claims);
        return unsigned + "." + hmacSha256(unsigned);
    }

    private Optional<Map<String, Object>> verifyClaims(String accessToken) {
        try {
            var jwt = stripBearer(accessToken);
            var parts = jwt.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }
            var unsigned = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(
                    hmacSha256(unsigned).getBytes(StandardCharsets.US_ASCII),
                    parts[2].getBytes(StandardCharsets.US_ASCII))) {
                return Optional.empty();
            }
            var claims = objectMapper.readValue(base64UrlDecode(parts[1]), CLAIMS_TYPE);
            var exp = claims.get("exp");
            if (!(exp instanceof Number number)
                    || number.longValue() <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private Optional<Long> userId(Map<String, Object> claims) {
        try {
            return Optional.of(Long.parseLong(String.valueOf(claims.get("sub"))));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
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

    private String base64Json(Object value) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT JSON serialization failed.", exception);
        }
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private String hmacSha256(String value) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT signing failed.", exception);
        }
    }

    private String randomToken() {
        var bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String tokenHash(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(normalizeToken(token).getBytes(StandardCharsets.UTF_8));
            var builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Token hashing failed.", exception);
        }
    }

    private String bearer(String token) {
        return BEARER_PREFIX + token;
    }

    private String stripBearer(String token) {
        var normalized = normalizeToken(token);
        return normalized.startsWith(BEARER_PREFIX) ? normalized.substring(BEARER_PREFIX.length()) : normalized;
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        return token.trim();
    }
}
