package com.jiralite.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jiralite.backend.dto.CreateTicketRequest;
import com.jiralite.backend.dto.PagedResponse;
import com.jiralite.backend.dto.TicketResponse;
import com.jiralite.backend.dto.TransitionTicketRequest;
import com.jiralite.backend.dto.UpdateTicketRequest;
import com.jiralite.backend.service.TicketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;

/**
 * Ticket endpoints scoped to the current org.
 */
@RestController
@RequestMapping("/tickets")
@Tag(name = "Tickets", description = "Ticket management within current org")
@Validated
public class TicketsController {

    private final TicketService ticketService;

    public TicketsController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "List tickets in current org (paged)")
    public ResponseEntity<PagedResponse<TicketResponse>> listTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID projectId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ticketService.listTickets(status, priority, projectId, pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "Search tickets by keyword in current org")
    public ResponseEntity<List<TicketResponse>> searchTickets(@RequestParam String keyword) {
        return ResponseEntity.ok(ticketService.search(keyword));
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "Get ticket detail in current org")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(ticketService.getTicket(ticketId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "Create ticket in current org")
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{ticketId}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "Update ticket in current org")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateTicketRequest request) {
        return ResponseEntity.ok(ticketService.updateTicket(ticketId, request));
    }

    @PostMapping("/{ticketId}/transition")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "Transition ticket status in current org")
    public ResponseEntity<TicketResponse> transitionTicket(
            @PathVariable UUID ticketId,
            @Valid @RequestBody TransitionTicketRequest request) {
        return ResponseEntity.ok(ticketService.transition(ticketId, request));
    }

    @PostMapping("/{ticketId}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "Soft delete ticket (move to trash, ADMIN or creator)")
    public ResponseEntity<Void> deleteTicket(
            @PathVariable UUID ticketId,
            @RequestParam(required = false) String reason) {
        ticketService.softDeleteTicket(ticketId, reason);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ticketId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Restore ticket from trash (ADMIN only)")
    public ResponseEntity<TicketResponse> restoreTicket(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(ticketService.restoreTicket(ticketId));
    }

    @GetMapping("/trash")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List tickets in trash (ADMIN only)")
    public ResponseEntity<List<TicketResponse>> listTrashTickets() {
        return ResponseEntity.ok(ticketService.listTrashTickets());
    }
}
