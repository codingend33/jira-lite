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

    public TicketCommentService(
            TicketRepository ticketRepository,
            TicketCommentRepository commentRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
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
        return toResponse(saved);
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
