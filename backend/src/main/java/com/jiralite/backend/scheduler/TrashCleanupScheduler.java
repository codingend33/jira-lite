package com.jiralite.backend.scheduler;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jiralite.backend.entity.ProjectEntity;
import com.jiralite.backend.entity.TicketEntity;
import com.jiralite.backend.repository.ProjectRepository;
import com.jiralite.backend.repository.TicketAttachmentRepository;
import com.jiralite.backend.repository.TicketCommentRepository;
import com.jiralite.backend.repository.TicketRepository;
import com.jiralite.backend.repository.AuditLogRepository;
import com.jiralite.backend.entity.AuditLogEntity;
import com.jiralite.backend.service.S3PresignService;

/**
 * Scheduled task to permanently delete soft-deleted items after retention
 * period.
 * Runs daily at 3:00 AM to minimize impact on system performance.
 */
@Component
public class TrashCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrashCleanupScheduler.class);
    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;

    private final ProjectRepository projectRepository;
    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final TicketAttachmentRepository attachmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final S3PresignService s3Service;

    public TrashCleanupScheduler(
            ProjectRepository projectRepository,
            TicketRepository ticketRepository,
            TicketCommentRepository commentRepository,
            TicketAttachmentRepository attachmentRepository,
            AuditLogRepository auditLogRepository,
            S3PresignService s3Service) {
        this.projectRepository = projectRepository;
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.attachmentRepository = attachmentRepository;
        this.auditLogRepository = auditLogRepository;
        this.s3Service = s3Service;
    }

    /**
     * Run cleanup at 3:00 AM every day.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredItems() {
        log.info("Starting trash cleanup job...");
        OffsetDateTime now = OffsetDateTime.now();

        int projectsDeleted = cleanupProjectsBatched(now);
        int ticketsDeleted = cleanupTicketsBatched(now);

        log.info("Trash cleanup completed. Projects deleted: {}, Tickets deleted: {}",
                projectsDeleted, ticketsDeleted);
    }

    /**
     * Clean up expired projects with batch pagination.
     */
    public int cleanupProjectsBatched(OffsetDateTime now) {
        int totalDeleted = 0;
        int page = 0;
        List<ProjectEntity> batch;

        do {
            Pageable pageable = PageRequest.of(page, BATCH_SIZE);
            batch = projectRepository.findPurgeCandidatesPaged(now, pageable);

            for (ProjectEntity project : batch) {
                if (cleanupSingleProject(project)) {
                    totalDeleted++;
                }
            }
            page++;
        } while (batch.size() == BATCH_SIZE);

        return totalDeleted;
    }

    /**
     * Clean up a single project in its own transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean cleanupSingleProject(ProjectEntity project) {
        try {
            // Get all tickets for this project to clean up S3
            List<TicketEntity> tickets = ticketRepository.findAllByProjectId(project.getId());

            for (TicketEntity ticket : tickets) {
                cleanupTicketData(ticket.getId());
            }

            // Hard delete comments and attachments for all tickets
            for (TicketEntity ticket : tickets) {
                commentRepository.deleteByTicketId(ticket.getId());
                attachmentRepository.deleteByTicketId(ticket.getId());
            }

            // Hard delete the project (cascades to tickets)
            projectRepository.delete(project);

            writeAudit(project.getOrgId(), "PROJECT_PURGE", project.getProjectKey(),
                    "Project " + project.getProjectKey() + " permanently deleted (" + tickets.size() + " tickets)");
            return true;

        } catch (Exception e) {
            log.error("Failed to cleanup project {}: {}", project.getId(), e.getMessage());
            writeAudit(project.getOrgId(), "CLEANUP_FAILED", project.getProjectKey(),
                    "Failed to cleanup project: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clean up expired standalone tickets with batch pagination.
     */
    public int cleanupTicketsBatched(OffsetDateTime now) {
        int totalDeleted = 0;
        int page = 0;
        List<TicketEntity> batch;

        do {
            Pageable pageable = PageRequest.of(page, BATCH_SIZE);
            batch = ticketRepository.findPurgeCandidatesPaged(now, pageable);

            for (TicketEntity ticket : batch) {
                if (cleanupSingleTicket(ticket)) {
                    totalDeleted++;
                }
            }
            page++;
        } while (batch.size() == BATCH_SIZE);

        return totalDeleted;
    }

    /**
     * Clean up a single ticket in its own transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean cleanupSingleTicket(TicketEntity ticket) {
        try {
            cleanupTicketData(ticket.getId());

            // Hard delete comments and attachments
            commentRepository.deleteByTicketId(ticket.getId());
            attachmentRepository.deleteByTicketId(ticket.getId());

            ticketRepository.delete(ticket);

            writeAudit(ticket.getOrgId(), "TICKET_PURGE", ticket.getTicketKey(),
                    "Ticket " + ticket.getTicketKey() + " permanently deleted");
            return true;

        } catch (Exception e) {
            log.error("Failed to cleanup ticket {}: {}", ticket.getId(), e.getMessage());
            writeAudit(ticket.getOrgId(), "CLEANUP_FAILED", ticket.getTicketKey(),
                    "Failed to cleanup ticket: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clean up ticket S3 attachments.
     */
    private void cleanupTicketData(UUID ticketId) {
        List<String> s3Keys = attachmentRepository.findS3KeysByDeletedTicketId(ticketId);

        for (String key : s3Keys) {
            if (key != null && !key.isBlank()) {
                deleteS3WithRetry(key);
            }
        }
    }

    /**
     * Delete S3 object with retry mechanism.
     */
    private void deleteS3WithRetry(String key) {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                s3Service.deleteObject(key);
                log.debug("Deleted S3 object: {}", key);
                return;
            } catch (Exception e) {
                attempts++;
                log.warn("S3 delete attempt {} failed for key {}: {}", attempts, key, e.getMessage());
                if (attempts >= MAX_RETRIES) {
                    log.error("Failed to delete S3 object after {} retries: {}", MAX_RETRIES, key);
                }
            }
        }
    }

    /**
     * Write audit log entry.
     */
    private void writeAudit(UUID orgId, String action, String entityId, String details) {
        try {
            AuditLogEntity auditLog = new AuditLogEntity();
            auditLog.setId(UUID.randomUUID());
            auditLog.setTenantId(orgId);
            auditLog.setAction(action);
            auditLog.setEntityType("CLEANUP");
            auditLog.setEntityId(entityId);
            auditLog.setDetails(details);
            auditLog.setCreatedAt(OffsetDateTime.now());
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }
}
