package com.teampulse.backend.auth.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAuthUserEntityRepository extends JpaRepository<AuthUserEntity, Long> {

    boolean existsByEmail(String email);

    Optional<AuthUserEntity> findByEmail(String email);
}
