package com.teampulse.backend.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.teampulse.backend.auth.application.AccessTokenVerifier;
import com.teampulse.backend.auth.domain.AuthUser;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

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
}
