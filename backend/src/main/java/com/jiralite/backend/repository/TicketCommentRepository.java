package com.jiralite.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jiralite.backend.entity.TicketCommentEntity;

public interface TicketCommentRepository extends JpaRepository<TicketCommentEntity, UUID> {

    List<TicketCommentEntity> findAllByOrgIdAndTicketIdOrderByCreatedAtAsc(UUID orgId, UUID ticketId);

    // --- Active comments (not deleted) ---
    @Query("SELECT c FROM TicketCommentEntity c WHERE c.orgId = :orgId AND c.ticketId = :ticketId AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    List<TicketCommentEntity> findActiveByOrgIdAndTicketId(@Param("orgId") UUID orgId,
            @Param("ticketId") UUID ticketId);

    @Query("SELECT COUNT(c) FROM TicketCommentEntity c WHERE c.ticketId = :ticketId AND c.deletedAt IS NULL")
    long countActiveByTicketId(@Param("ticketId") UUID ticketId);

    // --- Cascade soft delete by ticket ---
    @Modifying
    @Query("UPDATE TicketCommentEntity c SET c.deletedAt = :deletedAt, c.deletedBy = :deletedBy WHERE c.ticketId = :ticketId AND c.deletedAt IS NULL")
    int softDeleteByTicketId(@Param("ticketId") UUID ticketId, @Param("deletedAt") OffsetDateTime deletedAt,
            @Param("deletedBy") UUID deletedBy);

    // --- Cascade restore by ticket ---
    @Modifying
    @Query("UPDATE TicketCommentEntity c SET c.deletedAt = NULL, c.deletedBy = NULL WHERE c.ticketId = :ticketId AND c.deletedAt IS NOT NULL")
    int restoreByTicketId(@Param("ticketId") UUID ticketId);

    // --- Hard delete by ticket (cleanup) ---
    @Modifying
    @Query("DELETE FROM TicketCommentEntity c WHERE c.ticketId = :ticketId")
    void deleteByTicketId(@Param("ticketId") UUID ticketId);
}
