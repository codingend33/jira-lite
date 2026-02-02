package com.jiralite.backend.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jiralite.backend.audit.LogAudit;
import com.jiralite.backend.dto.AttachmentResponse;
import com.jiralite.backend.dto.ErrorCode;
import com.jiralite.backend.dto.PresignDownloadResponse;
import com.jiralite.backend.dto.PresignUploadRequest;
import com.jiralite.backend.dto.PresignUploadResponse;
import com.jiralite.backend.entity.TicketAttachmentEntity;
import com.jiralite.backend.entity.TicketEntity;
import com.jiralite.backend.exception.ApiException;
import com.jiralite.backend.repository.TicketAttachmentRepository;
import com.jiralite.backend.repository.TicketRepository;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;

/**
 * Ticket attachment management scoped to the current tenant.
 */
@Service
public class TicketAttachmentService {

    private static final Logger log = LoggerFactory.getLogger(TicketAttachmentService.class);
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_UPLOADED = "UPLOADED";

    private final TicketRepository ticketRepository;
    private final TicketAttachmentRepository attachmentRepository;
    private final S3PresignService s3PresignService;
    private final NotificationService notificationService;

    public TicketAttachmentService(
            TicketRepository ticketRepository,
            TicketAttachmentRepository attachmentRepository,
            S3PresignService s3PresignService,
            NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.attachmentRepository = attachmentRepository;
        this.s3PresignService = s3PresignService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> listAttachments(UUID ticketId) {
        TicketEntity ticket = getTicket(ticketId);
        return attachmentRepository.findAllByOrgIdAndTicketIdOrderByCreatedAtAsc(ticket.getOrgId(), ticket.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @LogAudit(action = "ATTACHMENT_UPLOAD", entityType = "ATTACHMENT")
    public PresignUploadResponse presignUpload(UUID ticketId, PresignUploadRequest request) {
        TicketEntity ticket = getTicket(ticketId);
        OffsetDateTime now = OffsetDateTime.now();

        TicketAttachmentEntity attachment = new TicketAttachmentEntity();
        attachment.setId(UUID.randomUUID());
        attachment.setOrgId(ticket.getOrgId());
        attachment.setTicketId(ticket.getId());
        attachment.setUploadedBy(parseUuidOrNull(getUserId()));
        attachment.setFileName(request.getFileName());
        attachment.setContentType(request.getContentType());
        attachment.setFileSize(request.getFileSize());
        attachment.setUploadStatus(STATUS_PENDING);
        attachment.setCreatedAt(now);
        attachment.setUpdatedAt(now);

        String s3Key = buildS3Key(ticket.getOrgId(), ticket.getId(), attachment.getId(), request.getFileName());
        attachment.setS3Key(s3Key);

        TicketAttachmentEntity saved = attachmentRepository.save(attachment);
        S3PresignService.PresignResult presign = s3PresignService.presignUpload(s3Key, request.getContentType());

        PresignUploadResponse response = new PresignUploadResponse(
                saved.getId(),
                presign.url().toString(),
                presign.headersOrEmpty(),
                presign.expiresAt());
        notifyUploader(attachment.getUploadedBy(), "ATTACHMENT_CREATED",
                "Attachment upload started for ticket " + ticket.getTicketKey());
        return response;
    }

    @Transactional
    @LogAudit(action = "ATTACHMENT_CONFIRM", entityType = "ATTACHMENT")
    public AttachmentResponse confirmUpload(UUID ticketId, UUID attachmentId) {
        TicketAttachmentEntity attachment = getAttachment(attachmentId);
        if (!attachment.getTicketId().equals(ticketId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Attachment not found", HttpStatus.NOT_FOUND.value());
        }
        attachment.setUploadStatus(STATUS_UPLOADED);
        attachment.setUpdatedAt(OffsetDateTime.now());
        return toResponse(attachment);
    }

    @Transactional(readOnly = true)
    public PresignDownloadResponse presignDownload(UUID ticketId, UUID attachmentId) {
        TicketAttachmentEntity attachment = getAttachment(attachmentId);
        if (!attachment.getTicketId().equals(ticketId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Attachment not found", HttpStatus.NOT_FOUND.value());
        }
        if (attachment.getS3Key() == null || attachment.getS3Key().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Attachment not ready", HttpStatus.BAD_REQUEST.value());
        }
        S3PresignService.PresignResult presign = s3PresignService.presignDownload(
                attachment.getS3Key(),
                attachment.getFileName(),
                attachment.getContentType());
        return new PresignDownloadResponse(
                attachment.getId(),
                presign.url().toString(),
                presign.expiresAt());
    }

    @Transactional
    @LogAudit(action = "ATTACHMENT_DELETE", entityType = "ATTACHMENT")
    public void deleteAttachment(UUID ticketId, UUID attachmentId) {
        TicketAttachmentEntity attachment = getAttachment(attachmentId);
        if (!attachment.getTicketId().equals(ticketId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Attachment not found", HttpStatus.NOT_FOUND.value());
        }

        if (attachment.getS3Key() != null && !attachment.getS3Key().isBlank()) {
            try {
                s3PresignService.deleteObject(attachment.getS3Key());
            } catch (Exception e) {
                // Log and continue to avoid leaving DB state inconsistent with S3
                // (best-effort cleanup).
                // A more robust approach would enqueue a retry job.
                log.warn("Failed to delete S3 object for attachment {}: {}", attachmentId, e.getMessage());
            }
        }

        attachmentRepository.delete(attachment);
        notifyUploader(attachment.getUploadedBy(), "ATTACHMENT_DELETED",
                "Attachment deleted on ticket " + attachment.getTicketId());
    }

    private void notifyUploader(UUID userId, String type, String content) {
        if (userId != null) {
            notificationService.createNotification(userId, type, content);
        }
    }

    private TicketEntity getTicket(UUID ticketId) {
        UUID orgId = getOrgId();
        return ticketRepository.findByIdAndOrgId(ticketId, orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Ticket not found",
                        HttpStatus.NOT_FOUND.value()));
    }

    private TicketAttachmentEntity getAttachment(UUID attachmentId) {
        UUID orgId = getOrgId();
        return attachmentRepository.findByIdAndOrgId(attachmentId, orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Attachment not found",
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

    private String buildS3Key(UUID orgId, UUID ticketId, UUID attachmentId, String fileName) {
        String safeName = fileName == null ? "file" : fileName.replaceAll("\\s+", "_");
        return "org/" + orgId + "/tickets/" + ticketId + "/" + attachmentId + "-" + safeName;
    }

    private AttachmentResponse toResponse(TicketAttachmentEntity attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getUploadStatus(),
                attachment.getCreatedAt(),
                attachment.getUpdatedAt());
    }
}
