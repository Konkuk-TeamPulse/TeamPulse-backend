package com.teampulse.backend.auth.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AuthPersistenceEntityTest {

    @Test
    void authUserEntityNormalizesNullableFieldsAndUpdatesTimestamps() {
        var user = new AuthUserEntity();
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
        user.setName("User");
        user.setUniversity(null);
        user.setPhone(null);

        user.prePersist();
        var createdAt = (LocalDateTime) ReflectionTestUtils.getField(user, "createdAt");
        var updatedAt = (LocalDateTime) ReflectionTestUtils.getField(user, "updatedAt");

        assertThat(user.getId()).isNull();
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getName()).isEqualTo("User");
        assertThat(user.getUniversity()).isEmpty();
        assertThat(user.getPhone()).isEmpty();
        assertThat(createdAt).isNotNull();
        assertThat(updatedAt).isNotNull();

        user.setUniversity("Konkuk");
        user.setPhone("010");
        user.preUpdate();
        var changedAt = (LocalDateTime) ReflectionTestUtils.getField(user, "updatedAt");

        assertThat(user.getUniversity()).isEqualTo("Konkuk");
        assertThat(user.getPhone()).isEqualTo("010");
        assertThat(changedAt).isAfterOrEqualTo(updatedAt);
    }

    @Test
    void authSessionEntityStoresSessionStateAndCreatedTimestamp() {
        var user = new AuthUserEntity();
        var session = new AuthSessionEntity();
        var expiresAt = LocalDateTime.now().plusDays(7);

        session.setUser(user);
        session.setAccessToken("access");
        session.setRefreshToken("refresh");
        session.setRevoked(true);
        session.setExpiresAt(expiresAt);
        session.prePersist();

        assertThat(session.getUser()).isSameAs(user);
        assertThat(session.getAccessToken()).isEqualTo("access");
        assertThat(session.getRefreshToken()).isEqualTo("refresh");
        assertThat(session.isRevoked()).isTrue();
        assertThat(session.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(ReflectionTestUtils.getField(session, "createdAt")).isNotNull();
    }
}
