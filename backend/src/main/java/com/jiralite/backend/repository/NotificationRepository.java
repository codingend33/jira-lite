package com.jiralite.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jiralite.backend.entity.NotificationEntity;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findTop50ByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId);

    Optional<NotificationEntity> findByIdAndTenantIdAndUserId(UUID id, UUID tenantId, UUID userId);

    org.springframework.data.domain.Page<NotificationEntity> findByTenantIdAndUserIdOrderByCreatedAtDesc(
            UUID tenantId, UUID userId, org.springframework.data.domain.Pageable pageable);
}
