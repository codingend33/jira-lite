package com.jiralite.backend.service;

import com.jiralite.backend.dto.ErrorCode;
import com.jiralite.backend.entity.ProjectEntity;
import com.jiralite.backend.entity.TicketEntity;
import com.jiralite.backend.exception.ApiException;
import com.jiralite.backend.repository.*;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceSoftDeleteTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private OrgMembershipRepository membershipRepository;
    @Mock
    private TicketCommentRepository commentRepository;
    @Mock
    private TicketAttachmentRepository attachmentRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogRepository auditLogRepository;

    private TicketService service;
    private final UUID orgId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID otherUser = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        service = new TicketService(ticketRepository, projectRepository, membershipRepository,
                commentRepository, attachmentRepository, notificationService, auditLogRepository);
        TenantContextHolder.set(new TenantContext(orgId.toString(), userId.toString(), Set.of("MEMBER"), "trace"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void softDelete_forbidden_whenNotAdminOrCreator() {
        TicketEntity ticket = baseTicket(otherUser);
        when(ticketRepository.findByIdAndOrgId(ticket.getId(), orgId)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(ticket.getProjectId())).thenReturn(Optional.of(activeProject()));

        assertThatThrownBy(() -> service.softDeleteTicket(ticket.getId(), null))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void softDelete_success_forCreator() {
        TicketEntity ticket = baseTicket(userId); // creator = current user
        when(ticketRepository.findByIdAndOrgId(ticket.getId(), orgId)).thenReturn(Optional.of(ticket));
        when(projectRepository.findById(ticket.getProjectId())).thenReturn(Optional.of(activeProject()));
        when(commentRepository.countActiveByTicketId(ticket.getId())).thenReturn(0L);
        when(attachmentRepository.countActiveByTicketId(ticket.getId())).thenReturn(0L);

        service.softDeleteTicket(ticket.getId(), "cleanup");

        assertThat(ticket.getDeletedAt()).isNotNull();
        assertThat(ticket.getDeletedBy()).isEqualTo(userId);
        verify(commentRepository).softDeleteByTicketId(eq(ticket.getId()), any(), eq(userId));
        verify(attachmentRepository).softDeleteByTicketId(eq(ticket.getId()), any(), eq(userId));
    }

    private TicketEntity baseTicket(UUID creator) {
        TicketEntity t = new TicketEntity();
        t.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        t.setOrgId(orgId);
        t.setProjectId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        t.setTicketKey("OPS-1");
        t.setTitle("Test");
        t.setStatus("OPEN");
        t.setPriority("LOW");
        t.setCreatedBy(creator);
        t.setCreatedAt(OffsetDateTime.now());
        t.setUpdatedAt(OffsetDateTime.now());
        return t;
    }

    private ProjectEntity activeProject() {
        ProjectEntity p = new ProjectEntity();
        p.setId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        p.setOrgId(orgId);
        p.setStatus("ACTIVE");
        p.setCreatedAt(OffsetDateTime.now());
        p.setUpdatedAt(OffsetDateTime.now());
        return p;
    }
}
