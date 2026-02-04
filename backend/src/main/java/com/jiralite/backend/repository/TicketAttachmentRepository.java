package com.jiralite.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jiralite.backend.entity.TicketAttachmentEntity;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachmentEntity, UUID> {

    List<TicketAttachmentEntity> findAllByOrgIdAndTicketIdOrderByCreatedAtAsc(UUID orgId, UUID ticketId);

    Optional<TicketAttachmentEntity> findByIdAndOrgId(UUID id, UUID orgId);

    // --- Active attachments (not deleted) ---
    @Query("SELECT a FROM TicketAttachmentEntity a WHERE a.orgId = :orgId AND a.ticketId = :ticketId AND a.deletedAt IS NULL ORDER BY a.createdAt ASC")
    List<TicketAttachmentEntity> findActiveByOrgIdAndTicketId(@Param("orgId") UUID orgId,
            @Param("ticketId") UUID ticketId);

    @Query("SELECT COUNT(a) FROM TicketAttachmentEntity a WHERE a.ticketId = :ticketId AND a.deletedAt IS NULL")
    long countActiveByTicketId(@Param("ticketId") UUID ticketId);

    // --- Cascade soft delete by ticket ---
    @Modifying
    @Query("UPDATE TicketAttachmentEntity a SET a.deletedAt = :deletedAt, a.deletedBy = :deletedBy WHERE a.ticketId = :ticketId AND a.deletedAt IS NULL")
    int softDeleteByTicketId(@Param("ticketId") UUID ticketId, @Param("deletedAt") OffsetDateTime deletedAt,
            @Param("deletedBy") UUID deletedBy);

    // --- Cascade restore by ticket ---
    @Modifying
    @Query("UPDATE TicketAttachmentEntity a SET a.deletedAt = NULL, a.deletedBy = NULL WHERE a.ticketId = :ticketId AND a.deletedAt IS NOT NULL")
    int restoreByTicketId(@Param("ticketId") UUID ticketId);

    // --- Get S3 keys for cleanup ---
    @Query("SELECT a.s3Key FROM TicketAttachmentEntity a WHERE a.ticketId = :ticketId AND a.deletedAt IS NOT NULL")
    List<String> findS3KeysByDeletedTicketId(@Param("ticketId") UUID ticketId);

    // --- Hard delete by ticket (cleanup) ---
    @Modifying
    @Query("DELETE FROM TicketAttachmentEntity a WHERE a.ticketId = :ticketId")
    void deleteByTicketId(@Param("ticketId") UUID ticketId);
}
