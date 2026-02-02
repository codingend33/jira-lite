package com.jiralite.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jiralite.backend.entity.OrgMembershipEntity;
import com.jiralite.backend.entity.OrgMembershipId;

public interface OrgMembershipRepository extends JpaRepository<OrgMembershipEntity, OrgMembershipId> {
    List<OrgMembershipEntity> findAllByIdOrgId(UUID orgId);

    List<OrgMembershipEntity> findAllByIdUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<OrgMembershipEntity> findByIdOrgIdAndIdUserId(UUID orgId, UUID userId);

    long countByIdOrgId(UUID orgId);
}
