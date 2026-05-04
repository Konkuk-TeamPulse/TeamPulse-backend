package com.teampulse.backend.auth.infrastructure;

import com.teampulse.backend.auth.application.AuthUserRepository;
import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.auth.persistence.AuthUserEntity;
import com.teampulse.backend.auth.persistence.JpaAuthUserEntityRepository;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile({"mysql", "prod"})
@Transactional
public class JpaAuthUserRepository implements AuthUserRepository {

    private final JpaAuthUserEntityRepository userRepository;

    public JpaAuthUserRepository(JpaAuthUserEntityRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public AuthUser save(AuthUser user) {
        var entity = user.id() > 0
                ? userRepository.findById(user.id()).orElseGet(AuthUserEntity::new)
                : userRepository.findByEmail(user.email()).orElseGet(AuthUserEntity::new);
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setName(user.name());
        entity.setUniversity(user.university());
        entity.setPhone(user.phone());
        return toDomain(userRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUser> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public long nextId() {
        return 0;
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
