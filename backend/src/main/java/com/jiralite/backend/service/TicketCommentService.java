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

    public TicketCommentService(
            TicketRepository ticketRepository,
            TicketCommentRepository commentRepository,
            NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
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
        return toResponse(saved);
    }

    private void notifyAssignee(TicketEntity ticket, String type, String content) {
        UUID assignee = ticket.getAssigneeId();
        UUID author = parseUuidOrNull(getUserId());
        if (assignee != null && !assignee.equals(author)) {
            notificationService.createNotification(assignee, type, content);
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

    private CommentResponse toResponse(TicketCommentEntity comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthorId(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
