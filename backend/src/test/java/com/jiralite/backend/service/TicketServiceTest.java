package com.jiralite.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jiralite.backend.dto.CreateTicketRequest;
import com.jiralite.backend.dto.TransitionTicketRequest;
import com.jiralite.backend.entity.ProjectEntity;
import com.jiralite.backend.entity.TicketEntity;
import com.jiralite.backend.exception.ApiException;
import com.jiralite.backend.repository.OrgMembershipRepository;
import com.jiralite.backend.repository.ProjectRepository;
import com.jiralite.backend.repository.TicketRepository;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

        @Mock
        private TicketRepository ticketRepository;
        @Mock
        private ProjectRepository projectRepository;
        @Mock
        private OrgMembershipRepository membershipRepository;
        @Mock
        private NotificationService notificationService;

        private TicketService ticketService;

        private final UUID ORG_ID = UUID.randomUUID();
        private final UUID USER_ID = UUID.randomUUID();

        @BeforeEach
        void setUp() {
                ticketService = new TicketService(ticketRepository, projectRepository, membershipRepository,
                                notificationService);
                TenantContextHolder.set(new TenantContext(ORG_ID.toString(), USER_ID.toString(),
                                java.util.Set.of("user"), "access_token"));
        }

        @Test
        void createTicket_InvalidPriority_ThrowsException() {
                CreateTicketRequest request = new CreateTicketRequest();
                request.setProjectId(UUID.randomUUID());
                request.setTitle("Test");
                request.setPriority("SUPER_URGENT"); // Invalid

                when(projectRepository.findByIdAndOrgId(any(), eq(ORG_ID)))
                                .thenReturn(Optional.of(new ProjectEntity()));

                assertThatThrownBy(() -> ticketService.createTicket(request))
                                .isInstanceOf(ApiException.class)
                                .hasMessageContaining("Invalid priority");
        }

        @Test
        void transition_InvalidStatus_ThrowsException() {
                UUID ticketId = UUID.randomUUID();
                TicketEntity ticket = new TicketEntity();
                ticket.setStatus("OPEN");
                ticket.setOrgId(ORG_ID);

                when(ticketRepository.findByIdAndOrgId(eq(ticketId), eq(ORG_ID)))
                                .thenReturn(Optional.of(ticket));

                TransitionTicketRequest request = new TransitionTicketRequest();
                request.setStatus("INVALID_STATUS");

                assertThatThrownBy(() -> ticketService.transition(ticketId, request))
                                .isInstanceOf(ApiException.class)
                                .hasMessageContaining("Invalid status");
        }

        @Test
        void transition_InvalidFlow_ThrowsException() {
                // CANCELLED -> DONE is invalid
                UUID ticketId = UUID.randomUUID();
                TicketEntity ticket = new TicketEntity();
                ticket.setStatus("CANCELLED");
                ticket.setOrgId(ORG_ID);

                when(ticketRepository.findByIdAndOrgId(eq(ticketId), eq(ORG_ID)))
                                .thenReturn(Optional.of(ticket));

                TransitionTicketRequest request = new TransitionTicketRequest();
                request.setStatus("DONE");

                assertThatThrownBy(() -> ticketService.transition(ticketId, request))
                                .isInstanceOf(ApiException.class)
                                .hasMessageContaining("Invalid status transition");
        }
}
