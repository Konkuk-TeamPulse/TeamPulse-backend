package com.teampulse.backend.workspace.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, Long> {

    Optional<WorkspaceEntity> findFirstByOwnerEmailIgnoreCaseOrderByIdAsc(String ownerEmail);

    Optional<WorkspaceEntity> findFirstByOwnerEmailIgnoreCaseAndInitializedFalseOrderByIdAsc(String ownerEmail);

    Optional<WorkspaceEntity> findFirstByInviteCodeIgnoreCaseAndInitializedTrueOrderByIdAsc(String inviteCode);

    boolean existsByInviteCodeIgnoreCase(String inviteCode);

    @Query("""
            select distinct workspace
            from WorkspaceEntity workspace
            join workspace.members member
            where workspace.initialized = true
              and lower(member.email) = lower(:email)
            order by workspace.id asc
            """)
    List<WorkspaceEntity> findInitializedByMemberEmail(@Param("email") String email);

    @Query("""
            select distinct workspace
            from WorkspaceEntity workspace
            left join workspace.members member
            where workspace.initialized = true
              and (
                lower(workspace.ownerEmail) = lower(:email)
                or lower(member.email) = lower(:email)
              )
            order by workspace.id asc
            """)
    List<WorkspaceEntity> findAccessibleInitializedByEmail(@Param("email") String email);
}
