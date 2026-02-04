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

    public ProjectService(ProjectRepository projectRepository,
            AuditLogRepository auditLogRepository,
            TicketRepository ticketRepository) {
        this.projectRepository = projectRepository;
        this.auditLogRepository = auditLogRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects() {
        UUID orgId = getOrgId();
        return projectRepository.findAllByOrgId(orgId).stream()
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
        project.setStatus(STATUS_ARCHIVED);
        project.setUpdatedAt(OffsetDateTime.now());
        writeAudit("PROJECT_ARCHIVE", project.getProjectKey(), "project " + project.getProjectKey() + " archived");
        return toResponse(project);
    }

    @Transactional
    @LogAudit(action = "PROJECT_UNARCHIVE", entityType = "PROJECT")
    public ProjectResponse unarchiveProject(UUID projectId) {
        ProjectEntity project = findProject(projectId);
        project.setStatus(STATUS_ACTIVE);
        project.setUpdatedAt(OffsetDateTime.now());
        writeAudit("PROJECT_UNARCHIVE", project.getProjectKey(), "project " + project.getProjectKey() + " unarchived");
        return toResponse(project);
    }

    @Transactional
    @LogAudit(action = "PROJECT_DELETE", entityType = "PROJECT")
    public void deleteProject(UUID projectId) {
        ProjectEntity project = findProject(projectId);
        if (ticketRepository.existsByOrgIdAndProjectId(project.getOrgId(), project.getId())) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Project has tickets. Delete/transfer tickets first or archive the project.",
                    HttpStatus.BAD_REQUEST.value());
        }
        try {
            projectRepository.delete(project);
            writeAudit("PROJECT_DELETE", project.getProjectKey(), "project " + project.getProjectKey() + " deleted");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Project has related tickets or data; delete tickets first or archive the project",
                    HttpStatus.BAD_REQUEST.value());
        }
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
