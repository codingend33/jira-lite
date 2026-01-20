package com.jiralite.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.jiralite.backend.entity.TicketEntity;

public interface TicketRepository extends JpaRepository<TicketEntity, UUID>, JpaSpecificationExecutor<TicketEntity> {
    Optional<TicketEntity> findByIdAndOrgId(UUID id, UUID orgId);

    long countByOrgIdAndProjectId(UUID orgId, UUID projectId);
}
