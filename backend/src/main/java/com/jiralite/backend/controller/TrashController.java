package com.jiralite.backend.controller;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jiralite.backend.dto.ProjectResponse;
import com.jiralite.backend.dto.TicketResponse;
import com.jiralite.backend.service.ProjectService;
import com.jiralite.backend.service.TicketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Unified trash management endpoint for viewing soft-deleted items.
 */
@RestController
@RequestMapping("/trash")
@Tag(name = "Trash", description = "Trash management (soft-deleted items)")
public class TrashController {

    private final ProjectService projectService;
    private final TicketService ticketService;

    public TrashController(ProjectService projectService, TicketService ticketService) {
        this.projectService = projectService;
        this.ticketService = ticketService;
    }

    /**
     * DTO for trash items combining projects and tickets.
     */
    public record TrashItemResponse(
            UUID id,
            String type, // "PROJECT" or "TICKET"
            String name, // project name or ticket title
            String key, // project_key or ticket_key
            String deletedAt,
            UUID deletedBy,
            String purgeAfter,
            long daysRemaining) {
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all items in trash (projects and tickets)")
    public ResponseEntity<List<TrashItemResponse>> listTrash(
            @RequestParam(required = false, defaultValue = "all") String type) {

        List<TrashItemResponse> items = new ArrayList<>();

        if ("all".equalsIgnoreCase(type) || "project".equalsIgnoreCase(type)) {
            List<ProjectResponse> projects = projectService.listTrashProjects();
            for (ProjectResponse p : projects) {
                OffsetDateTime purgeAfter = p.updatedAt().plusDays(30);
                long days = calculateDaysRemaining(purgeAfter);
                items.add(new TrashItemResponse(
                        p.id(),
                        "PROJECT",
                        p.name(),
                        p.key(),
                        p.updatedAt().toString(),
                        null, // deletedBy not in current response
                        purgeAfter.toString(),
                        days));
            }
        }

        if ("all".equalsIgnoreCase(type) || "ticket".equalsIgnoreCase(type)) {
            List<TicketResponse> tickets = ticketService.listTrashTickets();
            for (TicketResponse t : tickets) {
                OffsetDateTime purgeAfter = t.updatedAt().plusDays(30);
                long days = calculateDaysRemaining(purgeAfter);
                items.add(new TrashItemResponse(
                        t.id(),
                        "TICKET",
                        t.title(),
                        t.key(),
                        t.updatedAt().toString(),
                        null,
                        purgeAfter.toString(),
                        days));
            }
        }

        return ResponseEntity.ok(items);
    }

    private long calculateDaysRemaining(OffsetDateTime purgeAfter) {
        OffsetDateTime now = OffsetDateTime.now();
        return Duration.between(now, purgeAfter).toDays();
    }
}
