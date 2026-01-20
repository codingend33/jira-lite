package com.jiralite.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jiralite.backend.dto.AttachmentResponse;
import com.jiralite.backend.dto.PresignDownloadResponse;
import com.jiralite.backend.dto.PresignUploadRequest;
import com.jiralite.backend.dto.PresignUploadResponse;
import com.jiralite.backend.service.TicketAttachmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Ticket attachment endpoints scoped to the current org.
 */
@RestController
@RequestMapping("/tickets/{ticketId}/attachments")
@Tag(name = "Ticket Attachments", description = "Attachments for tickets")
@Validated
public class TicketAttachmentsController {

    private final TicketAttachmentService attachmentService;

    public TicketAttachmentsController(TicketAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "List attachments for a ticket")
    public ResponseEntity<List<AttachmentResponse>> listAttachments(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(attachmentService.listAttachments(ticketId));
    }

    @PostMapping("/presign-upload")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "Presign S3 upload for ticket attachment")
    public ResponseEntity<PresignUploadResponse> presignUpload(
            @PathVariable UUID ticketId,
            @Valid @RequestBody PresignUploadRequest request) {
        PresignUploadResponse response = attachmentService.presignUpload(ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{attachmentId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "Confirm attachment upload completed")
    public ResponseEntity<AttachmentResponse> confirmUpload(
            @PathVariable UUID ticketId,
            @PathVariable UUID attachmentId) {
        AttachmentResponse response = attachmentService.confirmUpload(ticketId, attachmentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{attachmentId}/presign-download")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "Presign S3 download for attachment")
    public ResponseEntity<PresignDownloadResponse> presignDownload(
            @PathVariable UUID ticketId,
            @PathVariable UUID attachmentId) {
        return ResponseEntity.ok(attachmentService.presignDownload(ticketId, attachmentId));
    }
}
