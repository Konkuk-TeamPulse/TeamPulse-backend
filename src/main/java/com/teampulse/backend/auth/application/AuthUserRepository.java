package com.teampulse.backend.auth.application;

import com.teampulse.backend.auth.domain.AuthUser;
import java.util.Optional;

public interface AuthUserRepository {

    boolean existsByEmail(String email);

    AuthUser save(AuthUser user);

    Optional<AuthUser> findByEmail(String email);

    long nextId();
}
