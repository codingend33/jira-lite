package com.jiralite.backend.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jiralite.backend.audit.LogAudit;
import com.jiralite.backend.dto.CreateTicketRequest;
import com.jiralite.backend.dto.ErrorCode;
import com.jiralite.backend.dto.PageMeta;
import com.jiralite.backend.dto.PagedResponse;
import com.jiralite.backend.dto.TicketResponse;
import com.jiralite.backend.dto.TransitionTicketRequest;
import com.jiralite.backend.dto.UpdateTicketRequest;
import com.jiralite.backend.entity.OrgMembershipEntity;
import com.jiralite.backend.entity.ProjectEntity;
import com.jiralite.backend.entity.TicketEntity;
import com.jiralite.backend.exception.ApiException;
import com.jiralite.backend.repository.OrgMembershipRepository;
import com.jiralite.backend.repository.ProjectRepository;
import com.jiralite.backend.repository.TicketRepository;
import com.jiralite.backend.service.NotificationService;
import com.jiralite.backend.repository.AuditLogRepository;
import com.jiralite.backend.entity.AuditLogEntity;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;

/**
 * Ticket management scoped to the current tenant.
 */
@Service
public class TicketService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("OPEN", "IN_PROGRESS", "DONE", "CANCELLED");
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");

    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final OrgMembershipRepository membershipRepository;
    private final NotificationService notificationService;
    private final AuditLogRepository auditLogRepository;

    public TicketService(
            TicketRepository ticketRepository,
            ProjectRepository projectRepository,
            OrgMembershipRepository membershipRepository,
            NotificationService notificationService,
            AuditLogRepository auditLogRepository) {
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.notificationService = notificationService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<TicketResponse> listTickets(
            String status,
            String priority,
            UUID projectId,
            Pageable pageable) {
        UUID orgId = getOrgId();
        Specification<TicketEntity> spec = (root, query, cb) -> cb.conjunction();
        spec = spec.and(orgEquals(orgId));
        Specification<TicketEntity> statusSpec = statusEquals(status);
        if (statusSpec != null) {
            spec = spec.and(statusSpec);
        }
        Specification<TicketEntity> prioritySpec = priorityEquals(priority);
        if (prioritySpec != null) {
            spec = spec.and(prioritySpec);
        }
        Specification<TicketEntity> projectSpec = projectEquals(projectId);
        if (projectSpec != null) {
            spec = spec.and(projectSpec);
        }

        Page<TicketEntity> page = ticketRepository.findAll(spec, pageable);
        List<TicketResponse> content = page.stream()
                .map(this::toResponse)
                .toList();
        return new PagedResponse<>(
                content,
                new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID ticketId) {
        return toResponse(findTicket(ticketId));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> search(String keyword) {
        UUID orgId = getOrgId();
        String term = keyword == null ? "" : keyword.trim();
        if (term.isEmpty()) {
            return List.of();
        }
        return ticketRepository.searchTickets(orgId, term)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @LogAudit(action = "TICKET_CREATE", entityType = "TICKET")
    public TicketResponse createTicket(CreateTicketRequest request) {
        UUID orgId = getOrgId();
        ProjectEntity project = projectRepository.findByIdAndOrgId(request.getProjectId(), orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Project not found",
                        HttpStatus.NOT_FOUND.value()));

        String priority = normalizePriority(request.getPriority());
        validateAssignee(orgId, request.getAssigneeId());

        long nextNumber = ticketRepository.countByOrgIdAndProjectId(orgId, project.getId()) + 1;
        String ticketKey = project.getProjectKey() + "-" + nextNumber;

        OffsetDateTime now = OffsetDateTime.now();
        TicketEntity ticket = new TicketEntity();
        ticket.setId(UUID.randomUUID());
        ticket.setOrgId(orgId);
        ticket.setProjectId(project.getId());
        ticket.setTicketKey(ticketKey);
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setStatus("OPEN");
        ticket.setPriority(priority);
        ticket.setCreatedBy(parseUuidOrNull(getUserId()));
        ticket.setAssigneeId(request.getAssigneeId());
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);

        TicketEntity saved = ticketRepository.save(ticket);
        notifyAssignee(saved.getAssigneeId(), "TICKET_ASSIGNED",
                "You were assigned ticket " + saved.getTicketKey());
        writeAudit("TICKET_CREATE", "TICKET", saved.getTicketKey(),
                "Ticket %s created with priority %s, assignee %s"
                        .formatted(saved.getTicketKey(), priority, formatAssignee(saved.getAssigneeId())));
        return toResponse(saved);
    }

    @Transactional
    @LogAudit(action = "TICKET_UPDATE", entityType = "TICKET")
    public TicketResponse updateTicket(UUID ticketId, UpdateTicketRequest request) {
        if ((request.getTitle() == null || request.getTitle().isBlank())
                && request.getDescription() == null
                && (request.getPriority() == null || request.getPriority().isBlank())
                && request.getAssigneeId() == null
                && (request.getClearAssignee() == null || !request.getClearAssignee())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "No fields to update", HttpStatus.BAD_REQUEST.value());
        }

        TicketEntity ticket = findTicket(ticketId);
        StringBuilder changeSummary = new StringBuilder();
        String oldTitle = ticket.getTitle();
        String oldDescription = ticket.getDescription();
        String oldPriority = ticket.getPriority();
        UUID oldAssignee = ticket.getAssigneeId();

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            if (!request.getTitle().equals(ticket.getTitle())) {
                changeSummary.append("title:'").append(trimForAudit(oldTitle)).append("' -> '")
                        .append(trimForAudit(request.getTitle())).append("'; ");
            }
            ticket.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            if (!request.getDescription().equals(ticket.getDescription())) {
                changeSummary.append("description changed; ");
            }
            ticket.setDescription(request.getDescription());
        }
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            String normalized = normalizePriority(request.getPriority());
            if (!normalized.equals(ticket.getPriority())) {
                changeSummary.append("priority:").append(ticket.getPriority()).append(" -> ").append(normalized)
                        .append("; ");
            }
            ticket.setPriority(normalized);
        }
        if (request.getAssigneeId() != null) {
            validateAssignee(ticket.getOrgId(), request.getAssigneeId());
            if (!request.getAssigneeId().equals(ticket.getAssigneeId())) {
                changeSummary.append("assignee:")
                        .append(formatAssignee(oldAssignee))
                        .append(" -> ")
                        .append(formatAssignee(request.getAssigneeId()))
                        .append("; ");
            }
            ticket.setAssigneeId(request.getAssigneeId());
        } else if (Boolean.TRUE.equals(request.getClearAssignee())) {
            if (ticket.getAssigneeId() != null) {
                changeSummary.append("assignee:")
                        .append(formatAssignee(ticket.getAssigneeId()))
                        .append(" -> Unassigned; ");
            }
            ticket.setAssigneeId(null);
        }
        ticket.setUpdatedAt(OffsetDateTime.now());

        String changeText = changeSummary.toString().isBlank() ? "updated" : changeSummary.toString().trim();
        String auditDetails = "Ticket %s %s".formatted(ticket.getTicketKey(), changeText);
        notifyAssignee(ticket.getAssigneeId(), "TICKET_UPDATED",
                "Ticket " + ticket.getTicketKey() + " updated: " + changeText);
        writeAudit("TICKET_UPDATE", "TICKET", ticket.getTicketKey(), auditDetails);
        return toResponse(ticket);
    }

    @Transactional
    @LogAudit(action = "TICKET_TRANSITION", entityType = "TICKET")
    public TicketResponse transition(UUID ticketId, TransitionTicketRequest request) {
        TicketEntity ticket = findTicket(ticketId);
        String nextStatus = normalizeStatus(request.getStatus());
        String currentStatus = ticket.getStatus();
        if (!isValidTransition(ticket.getStatus(), nextStatus)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Invalid status transition", HttpStatus.BAD_REQUEST.value());
        }
        ticket.setStatus(nextStatus);
        ticket.setUpdatedAt(OffsetDateTime.now());
        notifyAssignee(ticket.getAssigneeId(), "TICKET_STATUS",
                "Ticket " + ticket.getTicketKey() + " moved " + currentStatus + " -> " + nextStatus);
        writeAudit("TICKET_TRANSITION", "TICKET", ticket.getTicketKey(),
                "Ticket %s status: %s -> %s".formatted(ticket.getTicketKey(), currentStatus, nextStatus));
        return toResponse(ticket);
    }

    @Transactional
    @LogAudit(action = "TICKET_DELETE", entityType = "TICKET")
    public void deleteTicket(UUID ticketId) {
        TicketEntity ticket = findTicket(ticketId);
        ticketRepository.delete(ticket);
        writeAudit("TICKET_DELETE", "TICKET", ticket.getTicketKey(),
                "Ticket %s deleted".formatted(ticket.getTicketKey()));
    }

    private void notifyAssignee(UUID assigneeId, String type, String content) {
        if (assigneeId == null) {
            return;
        }
        notificationService.createNotification(assigneeId, type, content);
    }

    private TicketEntity findTicket(UUID ticketId) {
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

    private void validateAssignee(UUID orgId, UUID assigneeId) {
        if (assigneeId == null) {
            return;
        }
        OrgMembershipEntity membership = membershipRepository
                .findByIdOrgIdAndIdUserId(orgId, assigneeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Assignee not in org",
                        HttpStatus.NOT_FOUND.value()));
        if (membership.getStatus() != null && "DISABLED".equalsIgnoreCase(membership.getStatus())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Assignee is disabled", HttpStatus.BAD_REQUEST.value());
        }
    }

    private void writeAudit(String action, String entityType, String entityId, String details) {
        TenantContext ctx = TenantContextHolder.getRequired();
        try {
            AuditLogEntity log = new AuditLogEntity();
            log.setId(UUID.randomUUID());
            log.setTenantId(UUID.fromString(ctx.orgId()));
            log.setActorUserId(parseUuidOrNull(ctx.userId()));
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setDetails(details);
            log.setCreatedAt(OffsetDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception ignored) {
        }
    }

    private String formatAssignee(UUID id) {
        return id == null ? "Unassigned" : id.toString();
    }

    private String trimForAudit(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 50 ? text.substring(0, 47) + "..." : text;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Status is required", HttpStatus.BAD_REQUEST.value());
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Invalid status", HttpStatus.BAD_REQUEST.value());
        }
        return normalized;
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Priority is required", HttpStatus.BAD_REQUEST.value());
        }
        String normalized = priority.toUpperCase(Locale.ROOT);
        if (!ALLOWED_PRIORITIES.contains(normalized)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Invalid priority", HttpStatus.BAD_REQUEST.value());
        }
        return normalized;
    }

    private boolean isValidTransition(String current, String next) {
        if (current == null) {
            return false;
        }
        if (current.equals(next)) {
            return true;
        }
        return switch (current) {
            case "OPEN" -> next.equals("IN_PROGRESS") || next.equals("DONE") || next.equals("CANCELLED");
            case "IN_PROGRESS" -> next.equals("DONE") || next.equals("CANCELLED");
            case "DONE", "CANCELLED" -> next.equals("OPEN") || next.equals("IN_PROGRESS");
            default -> false;
        };
    }

    private Specification<TicketEntity> orgEquals(UUID orgId) {
        return (root, query, cb) -> cb.equal(root.get("orgId"), orgId);
    }

    private Specification<TicketEntity> statusEquals(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(root.get("status"), normalized);
    }

    private Specification<TicketEntity> priorityEquals(String priority) {
        if (priority == null || priority.isBlank()) {
            return null;
        }
        String normalized = priority.toUpperCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(root.get("priority"), normalized);
    }

    private Specification<TicketEntity> projectEquals(UUID projectId) {
        if (projectId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("projectId"), projectId);
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

    private TicketResponse toResponse(TicketEntity ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getProjectId(),
                ticket.getTicketKey(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssigneeId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
