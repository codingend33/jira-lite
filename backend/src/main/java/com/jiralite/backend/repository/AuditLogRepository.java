package com.jiralite.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jiralite.backend.entity.AuditLogEntity;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    List<AuditLogEntity> findTop50ByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
