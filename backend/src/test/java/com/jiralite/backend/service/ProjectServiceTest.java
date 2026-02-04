package com.jiralite.backend.service;

import com.jiralite.backend.dto.ErrorCode;
import com.jiralite.backend.entity.ProjectEntity;
import com.jiralite.backend.exception.ApiException;
import com.jiralite.backend.repository.AuditLogRepository;
import com.jiralite.backend.repository.ProjectRepository;
import com.jiralite.backend.repository.TicketRepository;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private NotificationService notificationService;

    private ProjectService service;

    private final UUID orgId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectRepository, auditLogRepository, ticketRepository, notificationService);
        TenantContextHolder.set(new TenantContext(orgId.toString(), userId.toString(), Set.of("ADMIN"), "trace"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void softDeleteProject_requiresArchived() {
        ProjectEntity project = baseProject();
        project.setArchivedAt(null); // not archived
        when(projectRepository.findByIdAndOrgId(project.getId(), orgId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.softDeleteProject(project.getId()))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);

        verify(ticketRepository, never()).softDeleteByProjectId(any(), any(), any(), any());
    }

    @Test
    void softDeleteProject_failsWhenActiveTickets() {
        ProjectEntity project = baseProject();
        project.setArchivedAt(OffsetDateTime.now());
        when(projectRepository.findByIdAndOrgId(project.getId(), orgId)).thenReturn(Optional.of(project));
        when(ticketRepository.countActiveByProjectIdAndStatusIn(eq(project.getId()), any())).thenReturn(2L);

        assertThatThrownBy(() -> service.softDeleteProject(project.getId()))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);

        verify(ticketRepository, never()).softDeleteByProjectId(any(), any(), any(), any());
    }

    @Test
    void softDeleteProject_success_setsDeletedFieldsAndNotifiesCreator() {
        ProjectEntity project = baseProject();
        project.setArchivedAt(OffsetDateTime.now());
        when(projectRepository.findByIdAndOrgId(project.getId(), orgId)).thenReturn(Optional.of(project));
        when(ticketRepository.countActiveByProjectIdAndStatusIn(eq(project.getId()), any())).thenReturn(0L);

        service.softDeleteProject(project.getId());

        assertThat(project.getDeletedAt()).isNotNull();
        assertThat(project.getDeletedBy()).isEqualTo(userId);
        assertThat(project.getPurgeAfter()).isNotNull();
        verify(ticketRepository).softDeleteByProjectId(eq(project.getId()), any(), eq(userId), any());
        verify(notificationService).createNotification(eq(project.getCreatedBy()), eq("PROJECT_DELETED"), any());
    }

    private ProjectEntity baseProject() {
        ProjectEntity p = new ProjectEntity();
        p.setId(UUID.fromString("aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa"));
        p.setOrgId(orgId);
        p.setProjectKey("OPS");
        p.setName("Ops");
        p.setStatus("ARCHIVED");
        p.setArchivedAt(OffsetDateTime.now());
        p.setCreatedBy(userId);
        p.setCreatedAt(OffsetDateTime.now());
        p.setUpdatedAt(OffsetDateTime.now());
        return p;
    }
}
