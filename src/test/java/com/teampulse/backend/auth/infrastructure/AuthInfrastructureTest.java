package com.teampulse.backend.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teampulse.backend.auth.application.AccessTokenVerifier;
import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.auth.persistence.AuthSessionEntity;
import com.teampulse.backend.auth.persistence.AuthUserEntity;
import com.teampulse.backend.auth.persistence.JpaAuthSessionRepository;
import com.teampulse.backend.auth.persistence.JpaAuthUserEntityRepository;
import jakarta.servlet.FilterChain;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthInfrastructureTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void inMemoryRepositoryAssignsIdsAndKeepsFirstUserForDuplicateEmail() {
        var repository = new InMemoryAuthUserRepository();
        var first = new AuthUser(repository.nextId(), "user@example.com", "hash", "User", "", "");
        var duplicate = new AuthUser(repository.nextId(), "user@example.com", "other", "Other", "", "");

        assertThat(repository.existsByEmail("user@example.com")).isFalse();
        assertThat(repository.save(first)).isSameAs(first);
        assertThat(repository.existsByEmail("user@example.com")).isTrue();
        assertThat(repository.save(duplicate)).isSameAs(first);
        assertThat(repository.findByEmail("user@example.com")).contains(first);
        assertThat(repository.findByEmail("missing@example.com")).isEmpty();
    }

    @Test
    void demoTokenIssuerTracksAccessAndRefreshTokenLifecycle() {
        var issuer = new DemoTokenIssuer();
        var user = new AuthUser(1L, "user@example.com", "hash", "User", "", "");
        var jwt = issuer.issue(user);

        assertThat(issuer.isActive(jwt.refreshToken())).isTrue();
        assertThat(issuer.isActiveAccessToken(jwt.accessToken())).isTrue();
        assertThat(issuer.findUserByAccessToken(jwt.accessToken())).contains(user);
        assertThat(issuer.findUserByAccessToken("missing")).isEmpty();

        issuer.revoke("unknown");
        assertThat(issuer.isActive(jwt.refreshToken())).isTrue();

        issuer.revoke(jwt.refreshToken());
        assertThat(issuer.isActive(jwt.refreshToken())).isFalse();
        assertThat(issuer.isActiveAccessToken(jwt.accessToken())).isFalse();
        assertThat(issuer.findUserByAccessToken(jwt.accessToken())).isEmpty();
    }

    @Test
    void accessTokenFilterSetsAuthenticationOnlyForValidToken() throws Exception {
        var user = new AuthUser(1L, "user@example.com", "hash", "User", "", "");
        var verifier = mock(AccessTokenVerifier.class);
        when(verifier.findUserByAccessToken("Bearer token")).thenReturn(Optional.of(user));

        var filter = new DemoAccessTokenAuthenticationFilter(verifier);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", " Bearer token ");
        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(user);

        SecurityContextHolder.clearContext();
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void jpaAuthUserRepositoryPersistsAndMapsDomainUsersForMysqlProfile() {
        var entityRepository = mock(JpaAuthUserEntityRepository.class);
        var repository = new JpaAuthUserRepository(entityRepository);
        var existing = userEntity(10L, "user@example.com", "hash", "User", "Konkuk", "010-0000-0000");

        when(entityRepository.existsByEmail("user@example.com")).thenReturn(true);
        when(entityRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));
        when(entityRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(entityRepository.saveAndFlush(any(AuthUserEntity.class))).thenAnswer(invocation -> {
            AuthUserEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 11L);
            return saved;
        });

        assertThat(repository.existsByEmail("user@example.com")).isTrue();
        assertThat(repository.findByEmail("user@example.com"))
                .contains(new AuthUser(10L, "user@example.com", "hash", "User", "Konkuk", "010-0000-0000"));

        var saved = repository.save(new AuthUser(0L, "new@example.com", "new-hash", "New User", "Konkuk", "010-1111-2222"));

        assertThat(saved).isEqualTo(new AuthUser(11L, "new@example.com", "new-hash", "New User", "Konkuk", "010-1111-2222"));
        assertThat(repository.nextId()).isZero();
        verify(entityRepository).saveAndFlush(any(AuthUserEntity.class));
    }

    @Test
    void jpaTokenIssuerStoresSessionsAndHonorsRevokedOrExpiredTokensForMysqlProfile() {
        var userRepository = mock(JpaAuthUserEntityRepository.class);
        var sessionRepository = mock(JpaAuthSessionRepository.class);
        var issuer = new JpaTokenIssuer(userRepository, sessionRepository);
        var entity = userEntity(20L, "db@example.com", "hash", "Db User", "Konkuk", "010-3333-4444");
        var user = new AuthUser(20L, "db@example.com", "hash", "Db User", "Konkuk", "010-3333-4444");

        when(userRepository.findById(20L)).thenReturn(Optional.of(entity));
        when(sessionRepository.save(any(AuthSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var jwtInfo = issuer.issue(user);

        assertThat(jwtInfo.accessToken()).startsWith("Bearer db-access-");
        assertThat(jwtInfo.refreshToken()).startsWith("Bearer db-refresh-");
        var captor = ArgumentCaptor.forClass(AuthSessionEntity.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(entity);
        assertThat(captor.getValue().getExpiresAt()).isAfter(LocalDateTime.now());

        var active = session(entity, "Bearer active-access", "Bearer active-refresh", false, LocalDateTime.now().plusDays(1));
        var revoked = session(entity, "Bearer revoked-access", "Bearer revoked-refresh", true, LocalDateTime.now().plusDays(1));
        var expired = session(entity, "Bearer expired-access", "Bearer expired-refresh", false, LocalDateTime.now().minusDays(1));
        when(sessionRepository.findByRefreshToken("Bearer active-refresh")).thenReturn(Optional.of(active));
        when(sessionRepository.findByRefreshToken("Bearer revoked-refresh")).thenReturn(Optional.of(revoked));
        when(sessionRepository.findByRefreshToken("Bearer expired-refresh")).thenReturn(Optional.of(expired));
        when(sessionRepository.findByAccessToken("Bearer active-access")).thenReturn(Optional.of(active));

        assertThat(issuer.isActive("Bearer active-refresh")).isTrue();
        assertThat(issuer.isActive("Bearer revoked-refresh")).isFalse();
        assertThat(issuer.isActive("Bearer expired-refresh")).isFalse();
        assertThat(issuer.isActiveAccessToken("Bearer active-access")).isTrue();
        assertThat(issuer.findUserByAccessToken("Bearer active-access")).contains(user);

        issuer.revoke("Bearer active-refresh");

        assertThat(active.isRevoked()).isTrue();
        verify(sessionRepository).save(active);
    }

    private AuthUserEntity userEntity(long id, String email, String passwordHash, String name, String university, String phone) {
        var entity = new AuthUserEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        entity.setEmail(email);
        entity.setPasswordHash(passwordHash);
        entity.setName(name);
        entity.setUniversity(university);
        entity.setPhone(phone);
        return entity;
    }

    private AuthSessionEntity session(
            AuthUserEntity user,
            String accessToken,
            String refreshToken,
            boolean revoked,
            LocalDateTime expiresAt
    ) {
        var session = new AuthSessionEntity();
        session.setUser(user);
        session.setAccessToken(accessToken);
        session.setRefreshToken(refreshToken);
        session.setRevoked(revoked);
        session.setExpiresAt(expiresAt);
        return session;
    }
}
