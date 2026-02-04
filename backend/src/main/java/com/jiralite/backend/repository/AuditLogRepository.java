package com.jiralite.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jiralite.backend.entity.AuditLogEntity;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    List<AuditLogEntity> findTop50ByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    org.springframework.data.domain.Page<AuditLogEntity> findByTenantIdOrderByCreatedAtDesc(
            UUID tenantId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<AuditLogEntity> findByTenantIdAndActionContainingIgnoreCaseOrderByCreatedAtDesc(
            UUID tenantId, String action, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<AuditLogEntity> findByTenantIdAndActorUserIdOrderByCreatedAtDesc(
            UUID tenantId, UUID actorUserId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<AuditLogEntity> findByTenantIdAndActorUserIdAndActionContainingIgnoreCaseOrderByCreatedAtDesc(
            UUID tenantId, UUID actorUserId, String action, org.springframework.data.domain.Pageable pageable);
}
