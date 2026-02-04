package com.jiralite.backend.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jiralite.backend.entity.TicketEntity;

public interface TicketRepository extends JpaRepository<TicketEntity, UUID>, JpaSpecificationExecutor<TicketEntity> {

        Optional<TicketEntity> findByIdAndOrgId(UUID id, UUID orgId);

        long countByOrgIdAndProjectId(UUID orgId, UUID projectId);

        long countByOrgIdAndAssigneeId(UUID orgId, UUID assigneeId);

        boolean existsByOrgIdAndProjectId(UUID orgId, UUID projectId);

        @Query("select t from TicketEntity t where t.orgId = :orgId and t.deletedAt IS NULL and " +
                        "(lower(t.title) like lower(concat('%', :keyword, '%')) or lower(t.description) like lower(concat('%', :keyword, '%')))")
        List<TicketEntity> searchTickets(@Param("orgId") UUID orgId, @Param("keyword") String keyword);

        // --- Active tickets (not deleted) ---
        @Query("SELECT t FROM TicketEntity t WHERE t.projectId = :projectId AND t.deletedAt IS NULL")
        List<TicketEntity> findActiveByProjectId(@Param("projectId") UUID projectId);

        @Query("SELECT COUNT(t) FROM TicketEntity t WHERE t.projectId = :projectId AND t.deletedAt IS NULL")
        long countActiveByProjectId(@Param("projectId") UUID projectId);

        // --- Count active tickets by status ---
        @Query("SELECT COUNT(t) FROM TicketEntity t WHERE t.projectId = :projectId AND t.deletedAt IS NULL AND t.status IN :statuses")
        long countActiveByProjectIdAndStatusIn(@Param("projectId") UUID projectId,
                        @Param("statuses") List<String> statuses);

        // --- Trash (soft deleted) ---
        @Query("SELECT t FROM TicketEntity t WHERE t.orgId = :orgId AND t.deletedAt IS NOT NULL ORDER BY t.deletedAt DESC")
        List<TicketEntity> findTrashByOrgId(@Param("orgId") UUID orgId);

        // --- For scheduled cleanup ---
        @Query("SELECT t FROM TicketEntity t WHERE t.deletedAt IS NOT NULL AND t.purgeAfter <= :now")
        List<TicketEntity> findPurgeCandidates(@Param("now") OffsetDateTime now);

        // --- Cascade soft delete by project ---
        @Modifying
        @Query("UPDATE TicketEntity t SET t.deletedAt = :deletedAt, t.deletedBy = :deletedBy, t.purgeAfter = :purgeAfter WHERE t.projectId = :projectId AND t.deletedAt IS NULL")
        int softDeleteByProjectId(@Param("projectId") UUID projectId, @Param("deletedAt") OffsetDateTime deletedAt,
                        @Param("deletedBy") UUID deletedBy, @Param("purgeAfter") OffsetDateTime purgeAfter);

        // --- Cascade restore by project ---
        @Modifying
        @Query("UPDATE TicketEntity t SET t.deletedAt = NULL, t.deletedBy = NULL, t.purgeAfter = NULL, t.restoredAt = :restoredAt, t.restoredBy = :restoredBy WHERE t.projectId = :projectId AND t.deletedAt IS NOT NULL")
        int restoreByProjectId(@Param("projectId") UUID projectId, @Param("restoredAt") OffsetDateTime restoredAt,
                        @Param("restoredBy") UUID restoredBy);

        // --- Count comments and attachments for threshold check ---
        @Query("SELECT COUNT(c) FROM TicketCommentEntity c WHERE c.ticketId = :ticketId AND c.deletedAt IS NULL")
        long countActiveCommentsByTicketId(@Param("ticketId") UUID ticketId);

        @Query("SELECT COUNT(a) FROM TicketAttachmentEntity a WHERE a.ticketId = :ticketId AND a.deletedAt IS NULL")
        long countActiveAttachmentsByTicketId(@Param("ticketId") UUID ticketId);

        // --- For cleanup scheduler: find all tickets by project (including deleted)
        // ---
        List<TicketEntity> findAllByProjectId(UUID projectId);

        // --- Paged purge candidates ---
        @Query("SELECT t FROM TicketEntity t WHERE t.deletedAt IS NOT NULL AND t.purgeAfter <= :now")
        List<TicketEntity> findPurgeCandidatesPaged(@Param("now") OffsetDateTime now,
                        org.springframework.data.domain.Pageable pageable);
}
