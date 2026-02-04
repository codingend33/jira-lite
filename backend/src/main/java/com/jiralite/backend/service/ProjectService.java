package com.jiralite.backend.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jiralite.backend.audit.LogAudit;
import com.jiralite.backend.dto.CreateProjectRequest;
import com.jiralite.backend.dto.ErrorCode;
import com.jiralite.backend.dto.ProjectResponse;
import com.jiralite.backend.dto.UpdateProjectRequest;
import com.jiralite.backend.entity.AuditLogEntity;
import com.jiralite.backend.entity.ProjectEntity;
import com.jiralite.backend.exception.ApiException;
import com.jiralite.backend.repository.AuditLogRepository;
import com.jiralite.backend.repository.TicketRepository;
import com.jiralite.backend.repository.ProjectRepository;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;

/**
 * Project management scoped to the current tenant.
 */
@Service
public class ProjectService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private final ProjectRepository projectRepository;
    private final AuditLogRepository auditLogRepository;
    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;

    public ProjectService(ProjectRepository projectRepository,
            AuditLogRepository auditLogRepository,
            TicketRepository ticketRepository,
            NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.auditLogRepository = auditLogRepository;
        this.ticketRepository = ticketRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects() {
        UUID orgId = getOrgId();
        // Exclude deleted projects (deletedAt IS NULL)
        return projectRepository.findAllByOrgId(orgId).stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId) {
        ProjectEntity project = findProject(projectId);
        return toResponse(project);
    }

    @Transactional
    @LogAudit(action = "PROJECT_CREATE", entityType = "PROJECT")
    public ProjectResponse createProject(CreateProjectRequest request) {
        UUID orgId = getOrgId();
        if (request.getKey() == null || request.getKey().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Project key is required", HttpStatus.BAD_REQUEST.value());
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Project name is required", HttpStatus.BAD_REQUEST.value());
        }
        if (projectRepository.existsByOrgIdAndProjectKey(orgId, request.getKey())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Project key already exists", HttpStatus.BAD_REQUEST.value());
        }

        OffsetDateTime now = OffsetDateTime.now();
        ProjectEntity project = new ProjectEntity();
        project.setId(UUID.randomUUID());
        project.setOrgId(orgId);
        project.setProjectKey(request.getKey());
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStatus(STATUS_ACTIVE);
        project.setCreatedBy(parseUuidOrNull(getUserId()));
        project.setCreatedAt(now);
        project.setUpdatedAt(now);

        ProjectEntity saved = projectRepository.save(project);
        writeAudit("PROJECT_CREATE", saved.getProjectKey(),
                "project %s created (name=%s)".formatted(saved.getProjectKey(), saved.getName()));
        return toResponse(saved);
    }

    @Transactional
    @LogAudit(action = "PROJECT_UPDATE", entityType = "PROJECT")
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request) {
        if ((request.getName() == null || request.getName().isBlank())
                && (request.getDescription() == null || request.getDescription().isBlank())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "name or description is required",
                    HttpStatus.BAD_REQUEST.value());
        }

        ProjectEntity project = findProject(projectId);
        if (request.getName() != null && !request.getName().isBlank()) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        project.setUpdatedAt(OffsetDateTime.now());

        writeAudit("PROJECT_UPDATE", project.getProjectKey(), "project " + project.getProjectKey() + " updated");
        return toResponse(project);
    }

    @Transactional
    @LogAudit(action = "PROJECT_ARCHIVE", entityType = "PROJECT")
    public ProjectResponse archiveProject(UUID projectId) {
        ProjectEntity project = findProject(projectId);

        if (project.isArchived()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Project is already archived",
                    HttpStatus.BAD_REQUEST.value());
        }
        if (project.isDeleted()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Cannot archive a deleted project",
                    HttpStatus.BAD_REQUEST.value());
        }

        UUID userId = parseUuidOrNull(getUserId());
        OffsetDateTime now = OffsetDateTime.now();

        project.setArchivedAt(now);
        project.setArchivedBy(userId);
        project.setStatus(STATUS_ARCHIVED);
        project.setUpdatedAt(now);

        writeAudit("PROJECT_ARCHIVE", project.getProjectKey(),
                "project " + project.getProjectKey() + " archived");
        return toResponse(project);
    }

    @Transactional
    @LogAudit(action = "PROJECT_UNARCHIVE", entityType = "PROJECT")
    public ProjectResponse unarchiveProject(UUID projectId) {
        ProjectEntity project = findProject(projectId);

        if (!project.isArchived()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Project is not archived",
                    HttpStatus.BAD_REQUEST.value());
        }
        if (project.isDeleted()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Cannot unarchive a deleted project",
                    HttpStatus.BAD_REQUEST.value());
        }

        project.setArchivedAt(null);
        project.setArchivedBy(null);
        project.setStatus(STATUS_ACTIVE);
        project.setUpdatedAt(OffsetDateTime.now());

        writeAudit("PROJECT_UNARCHIVE", project.getProjectKey(),
                "project " + project.getProjectKey() + " unarchived");
        return toResponse(project);
    }

    /**
     * Soft delete a project and cascade to all its tickets, comments, attachments.
     * Pre-conditions: must be archived and have no active tickets
     * (OPEN/IN_PROGRESS).
     */
    @Transactional
    @LogAudit(action = "PROJECT_SOFT_DELETE", entityType = "PROJECT")
    public void softDeleteProject(UUID projectId) {
        ProjectEntity project = findProject(projectId);
        UUID userId = parseUuidOrNull(getUserId());
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime purgeAfter = now.plusDays(30);

        // Pre-check: must be archived
        if (!project.isArchived()) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Project must be archived before deletion. Archive it first.",
                    HttpStatus.BAD_REQUEST.value());
        }

        // Pre-check: no active tickets (OPEN or IN_PROGRESS)
        long activeTickets = ticketRepository.countActiveByProjectIdAndStatusIn(
                project.getId(), List.of("OPEN", "IN_PROGRESS"));
        if (activeTickets > 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Project has " + activeTickets + " active tickets. Close them before deletion.",
                    HttpStatus.BAD_REQUEST.value());
        }

        // Cascade soft delete all tickets
        ticketRepository.softDeleteByProjectId(project.getId(), now, userId, purgeAfter);

        // Soft delete the project
        project.setDeletedAt(now);
        project.setDeletedBy(userId);
        project.setPurgeAfter(purgeAfter);
        project.setUpdatedAt(now);

        writeAudit("PROJECT_SOFT_DELETE", project.getProjectKey(),
                "project " + project.getProjectKey() + " moved to trash");

        // Notify creator
        String purgeDate = purgeAfter.toLocalDate().toString();
        if (project.getCreatedBy() != null) {
            notificationService.createNotification(project.getCreatedBy(), "PROJECT_DELETED",
                    "Project " + project.getName() + " (" + project.getProjectKey() + ") has been moved to trash. " +
                            "It will be permanently deleted on " + purgeDate
                            + ". [View in Trash](/trash?type=project)");
        }
    }

    /**
     * Restore a soft-deleted project and cascade restore all its tickets.
     * Pre-check: project key must not conflict with existing active projects.
     */
    @Transactional
    @LogAudit(action = "PROJECT_RESTORE", entityType = "PROJECT")
    public ProjectResponse restoreProject(UUID projectId) {
        UUID orgId = getOrgId();
        ProjectEntity project = projectRepository.findById(projectId)
                .filter(p -> p.getOrgId().equals(orgId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Project not found",
                        HttpStatus.NOT_FOUND.value()));

        if (!project.isDeleted()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Project is not in trash",
                    HttpStatus.BAD_REQUEST.value());
        }

        // Check for key conflict
        if (projectRepository.existsActiveByOrgIdAndProjectKey(orgId, project.getProjectKey())) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Cannot restore: project key '" + project.getProjectKey() + "' already exists. " +
                            "Delete the conflicting project or contact admin.",
                    HttpStatus.BAD_REQUEST.value());
        }

        UUID userId = parseUuidOrNull(getUserId());
        OffsetDateTime now = OffsetDateTime.now();

        // Cascade restore all tickets
        ticketRepository.restoreByProjectId(project.getId(), now, userId);

        // Restore the project (remains archived)
        project.setDeletedAt(null);
        project.setDeletedBy(null);
        project.setPurgeAfter(null);
        project.setRestoredAt(now);
        project.setRestoredBy(userId);
        project.setUpdatedAt(now);

        writeAudit("PROJECT_RESTORE", project.getProjectKey(),
                "project " + project.getProjectKey() + " restored from trash");

        // Notify creator
        if (project.getCreatedBy() != null) {
            notificationService.createNotification(project.getCreatedBy(), "PROJECT_RESTORED",
                    "Project " + project.getName() + " (" + project.getProjectKey() + ") has been restored from trash. "
                            +
                            "[View Project](/projects/" + project.getId() + ")");
        }

        return toResponse(project);
    }

    /**
     * Get all projects in trash for current org.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> listTrashProjects() {
        UUID orgId = getOrgId();
        return projectRepository.findTrashByOrgId(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get active projects (not archived, not deleted) for current org.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> listActiveProjects() {
        UUID orgId = getOrgId();
        return projectRepository.findActiveByOrgId(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get archived projects (not deleted) for current org.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> listArchivedProjects() {
        UUID orgId = getOrgId();
        return projectRepository.findArchivedByOrgId(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ProjectEntity findProject(UUID projectId) {
        UUID orgId = getOrgId();
        return projectRepository.findByIdAndOrgId(projectId, orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Project not found",
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

    private void writeAudit(String action, String entityId, String details) {
        try {
            TenantContext ctx = TenantContextHolder.getRequired();
            AuditLogEntity log = new AuditLogEntity();
            log.setId(UUID.randomUUID());
            log.setTenantId(UUID.fromString(ctx.orgId()));
            log.setActorUserId(ctx.userId() != null && !ctx.userId().isBlank() ? UUID.fromString(ctx.userId()) : null);
            log.setAction(action);
            log.setEntityType("PROJECT");
            log.setEntityId(entityId);
            log.setDetails(details);
            log.setCreatedAt(OffsetDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception ignored) {
        }
    }

    private ProjectResponse toResponse(ProjectEntity project) {
        return new ProjectResponse(
                project.getId(),
                project.getProjectKey(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedBy(),
                project.getCreatedAt(),
                project.getUpdatedAt());
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
}
