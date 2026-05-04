package com.teampulse.backend.mobile.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileWorkspaceRepository extends JpaRepository<MobileWorkspaceEntity, Long> {

    Optional<MobileWorkspaceEntity> findFirstByOwnerEmailIgnoreCaseOrderByIdAsc(String ownerEmail);
}
