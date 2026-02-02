package com.jiralite.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jiralite.backend.entity.NotificationEntity;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findTop50ByTenantIdAndUserIdOrderByCreatedAtDesc(String tenantId, UUID userId);
}
