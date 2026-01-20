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

import com.jiralite.backend.dto.CommentResponse;
import com.jiralite.backend.dto.CreateCommentRequest;
import com.jiralite.backend.service.TicketCommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Ticket comment endpoints scoped to the current org.
 */
@RestController
@RequestMapping("/tickets/{ticketId}/comments")
@Tag(name = "Ticket Comments", description = "Comments for tickets")
@Validated
public class TicketCommentsController {

    private final TicketCommentService commentService;

    public TicketCommentsController(TicketCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "List comments for a ticket")
    public ResponseEntity<List<CommentResponse>> listComments(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(commentService.listComments(ticketId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "Create a comment for a ticket")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID ticketId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse response = commentService.createComment(ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
