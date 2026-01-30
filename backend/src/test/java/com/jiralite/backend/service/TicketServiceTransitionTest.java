package com.jiralite.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jiralite.backend.dto.TransitionTicketRequest;
import com.jiralite.backend.entity.TicketEntity;
import com.jiralite.backend.repository.OrgMembershipRepository;
import com.jiralite.backend.repository.ProjectRepository;
import com.jiralite.backend.repository.TicketRepository;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;

@ExtendWith(MockitoExtension.class)
class TicketServiceTransitionTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private OrgMembershipRepository membershipRepository;

    @InjectMocks
    private TicketService ticketService;

    private final UUID orgId = UUID.randomUUID();
    private final UUID ticketId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void allowsReopenFromDoneToOpen() {
        TenantContextHolder.set(new TenantContext(orgId.toString(), "user-1", java.util.Set.of("ADMIN"), null));

        TicketEntity ticket = new TicketEntity();
        ticket.setId(ticketId);
        ticket.setOrgId(orgId);
        ticket.setStatus("DONE");
        ticket.setUpdatedAt(OffsetDateTime.now());

        when(ticketRepository.findByIdAndOrgId(ticketId, orgId)).thenReturn(Optional.of(ticket));

        TransitionTicketRequest request = new TransitionTicketRequest();
        request.setStatus("open");

        var response = ticketService.transition(ticketId, request);

        assertThat(response.status()).isEqualTo("OPEN");
        assertThat(ticket.getUpdatedAt()).isNotNull();
    }

    @Test
    void allowsReopenFromCancelledToInProgress() {
        TenantContextHolder.set(new TenantContext(orgId.toString(), "user-1", java.util.Set.of("ADMIN"), null));

        TicketEntity ticket = new TicketEntity();
        ticket.setId(ticketId);
        ticket.setOrgId(orgId);
        ticket.setStatus("CANCELLED");
        ticket.setUpdatedAt(OffsetDateTime.now());

        when(ticketRepository.findByIdAndOrgId(ticketId, orgId)).thenReturn(Optional.of(ticket));

        TransitionTicketRequest request = new TransitionTicketRequest();
        request.setStatus("IN_PROGRESS");

        var response = ticketService.transition(ticketId, request);

        assertThat(response.status()).isEqualTo("IN_PROGRESS");
    }
}
