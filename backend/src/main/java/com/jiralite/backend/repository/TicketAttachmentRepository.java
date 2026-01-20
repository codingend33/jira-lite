package com.jiralite.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jiralite.backend.entity.TicketAttachmentEntity;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachmentEntity, UUID> {
    List<TicketAttachmentEntity> findAllByOrgIdAndTicketIdOrderByCreatedAtAsc(UUID orgId, UUID ticketId);

    Optional<TicketAttachmentEntity> findByIdAndOrgId(UUID id, UUID orgId);
}
