package com.teampulse.backend.mobile.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MobileWorkspaceRepository extends JpaRepository<MobileWorkspaceEntity, Long> {

    Optional<MobileWorkspaceEntity> findFirstByOwnerEmailIgnoreCaseOrderByIdAsc(String ownerEmail);

    Optional<MobileWorkspaceEntity> findFirstByOwnerEmailIgnoreCaseAndInitializedFalseOrderByIdAsc(String ownerEmail);

    Optional<MobileWorkspaceEntity> findFirstByInviteCodeIgnoreCaseAndInitializedTrueOrderByIdAsc(String inviteCode);

    @Query("""
            select distinct workspace
            from MobileWorkspaceEntity workspace
            join workspace.members member
            where workspace.initialized = true
              and lower(member.email) = lower(:email)
            order by workspace.id asc
            """)
    List<MobileWorkspaceEntity> findInitializedByMemberEmail(@Param("email") String email);

    @Query("""
            select distinct workspace
            from MobileWorkspaceEntity workspace
            left join workspace.members member
            where workspace.initialized = true
              and (
                lower(workspace.ownerEmail) = lower(:email)
                or lower(member.email) = lower(:email)
              )
            order by workspace.id asc
            """)
    List<MobileWorkspaceEntity> findAccessibleInitializedByEmail(@Param("email") String email);
}
