package com.jiralite.backend.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jiralite.backend.dto.CommentResponse;
import com.jiralite.backend.dto.CreateCommentRequest;
import com.jiralite.backend.dto.ErrorCode;
import com.jiralite.backend.entity.TicketCommentEntity;
import com.jiralite.backend.entity.TicketEntity;
import com.jiralite.backend.audit.LogAudit;
import com.jiralite.backend.exception.ApiException;
import com.jiralite.backend.repository.TicketCommentRepository;
import com.jiralite.backend.repository.TicketRepository;
import com.jiralite.backend.repository.AuditLogRepository;
import com.jiralite.backend.entity.AuditLogEntity;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;

/**
 * Ticket comment management scoped to the current tenant.
 */
@Service
public class TicketCommentService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final NotificationService notificationService;
    private final AuditLogRepository auditLogRepository;

    public TicketCommentService(
            TicketRepository ticketRepository,
            TicketCommentRepository commentRepository,
            NotificationService notificationService,
            AuditLogRepository auditLogRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(UUID ticketId) {
        TicketEntity ticket = getTicket(ticketId);
        return commentRepository.findAllByOrgIdAndTicketIdOrderByCreatedAtAsc(ticket.getOrgId(), ticket.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @LogAudit(action = "COMMENT_CREATE", entityType = "COMMENT")
    public CommentResponse createComment(UUID ticketId, CreateCommentRequest request) {
        TicketEntity ticket = getTicket(ticketId);
        OffsetDateTime now = OffsetDateTime.now();

        TicketCommentEntity comment = new TicketCommentEntity();
        comment.setId(UUID.randomUUID());
        comment.setOrgId(ticket.getOrgId());
        comment.setTicketId(ticket.getId());
        comment.setAuthorId(parseUuidOrNull(getUserId()));
        comment.setBody(request.getBody());
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);

        TicketCommentEntity saved = commentRepository.save(comment);
        notifyAssignee(ticket, "COMMENT_CREATED", "New comment on " + ticket.getTicketKey());
        writeAudit("COMMENT_CREATE", ticket, saved.getId().toString(),
                "Comment added to ticket " + ticket.getTicketKey());
        return toResponse(saved);
    }

    private void notifyAssignee(TicketEntity ticket, String type, String content) {
        UUID assignee = ticket.getAssigneeId();
        UUID reporter = ticket.getCreatedBy();
        UUID author = parseUuidOrNull(getUserId());
        if (assignee != null) {
            notificationService.createNotification(assignee, type, content);
        }
        if (reporter != null && (assignee == null || !reporter.equals(assignee))) {
            notificationService.createNotification(reporter, type, content);
        }
    }

    private TicketEntity getTicket(UUID ticketId) {
        UUID orgId = getOrgId();
        return ticketRepository.findByIdAndOrgId(ticketId, orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Ticket not found",
                        HttpStatus.NOT_FOUND.value()));
    }

    private UUID getOrgId() {
        TenantContext context = TenantContextHolder.getRequired();
        if (context.orgId() == null || context.orgId().isBlank()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Missing org context", HttpStatus.UNAUTHORIZED.value());
        }
        return UUID.fromString(context.orgId());
    }

    private String getUserId() {
        TenantContext context = TenantContextHolder.getRequired();
        return context.userId();
    }

    private UUID parseUuidOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void writeAudit(String action, TicketEntity ticket, String entityId, String details) {
        try {
            TenantContext ctx = TenantContextHolder.getRequired();
            AuditLogEntity log = new AuditLogEntity();
            log.setId(UUID.randomUUID());
            log.setTenantId(UUID.fromString(ctx.orgId()));
            log.setActorUserId(parseUuidOrNull(ctx.userId()));
            log.setAction(action);
            log.setEntityType("COMMENT");
            log.setEntityId(entityId);
            log.setDetails("Ticket " + ticket.getTicketKey() + ": " + details);
            log.setCreatedAt(OffsetDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception ignored) {
        }
    }

    private CommentResponse toResponse(TicketCommentEntity comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthorId(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
