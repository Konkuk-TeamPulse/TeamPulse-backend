package com.teampulse.backend.auth.infrastructure;

import com.teampulse.backend.auth.application.AuthUserRepository;
import com.teampulse.backend.auth.domain.AuthUser;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryAuthUserRepository implements AuthUserRepository {

    private final AtomicLong ids = new AtomicLong(0);
    private final ConcurrentHashMap<String, AuthUser> usersByEmail = new ConcurrentHashMap<>();

    @Override
    public boolean existsByEmail(String email) {
        return usersByEmail.containsKey(email);
    }

    @Override
    public AuthUser save(AuthUser user) {
        var existing = usersByEmail.putIfAbsent(user.email(), user);
        if (existing != null) {
            return existing;
        }
        return user;
    }

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(email));
    }

    @Override
    public long nextId() {
        return ids.incrementAndGet();
    }
}
