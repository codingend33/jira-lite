package com.jiralite.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jiralite.backend.dto.CreateCommentRequest;
import com.jiralite.backend.entity.TicketCommentEntity;
import com.jiralite.backend.entity.TicketEntity;
import com.jiralite.backend.repository.TicketCommentRepository;
import com.jiralite.backend.repository.TicketRepository;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;

@ExtendWith(MockitoExtension.class)
class TicketCommentServiceTest {

    @Mock
    TicketRepository ticketRepository;

    @Mock
    TicketCommentRepository commentRepository;

    @Mock
    NotificationService notificationService;

    @InjectMocks
    TicketCommentService service;

    private final UUID orgId = UUID.randomUUID();
    private final UUID assigneeId = UUID.randomUUID();
    private final UUID ticketId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContextHolder.set(new TenantContext(orgId.toString(), userId.toString(), Set.of("ADMIN"), "trace"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createComment_notifiesAssigneeWhenDifferentFromAuthor() {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(ticketId);
        ticket.setOrgId(orgId);
        ticket.setAssigneeId(assigneeId);
        ticket.setTicketKey("PROJ-1");
        when(ticketRepository.findByIdAndOrgId(ticketId, orgId)).thenReturn(Optional.of(ticket));

        TicketCommentEntity saved = new TicketCommentEntity();
        saved.setId(UUID.randomUUID());
        saved.setOrgId(orgId);
        when(commentRepository.save(any())).thenReturn(saved);

        CreateCommentRequest req = new CreateCommentRequest();
        req.setBody("hello");

        service.createComment(ticketId, req);

        verify(notificationService).createNotification(assigneeId, "COMMENT_CREATED", "New comment on PROJ-1");
        verify(commentRepository).save(any(TicketCommentEntity.class));
    }
}
