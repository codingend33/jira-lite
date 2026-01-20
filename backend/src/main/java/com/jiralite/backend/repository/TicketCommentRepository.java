package com.jiralite.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jiralite.backend.entity.TicketCommentEntity;

public interface TicketCommentRepository extends JpaRepository<TicketCommentEntity, UUID> {
    List<TicketCommentEntity> findAllByOrgIdAndTicketIdOrderByCreatedAtAsc(UUID orgId, UUID ticketId);
}
