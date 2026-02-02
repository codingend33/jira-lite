package com.jiralite.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jiralite.backend.dto.DashboardMetricsResponse;
import com.jiralite.backend.repository.OrgMembershipRepository;
import com.jiralite.backend.repository.ProjectRepository;
import com.jiralite.backend.repository.TicketRepository;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;

@Service
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final TicketRepository ticketRepository;
    private final OrgMembershipRepository membershipRepository;

    public DashboardService(ProjectRepository projectRepository,
            TicketRepository ticketRepository,
            OrgMembershipRepository membershipRepository) {
        this.projectRepository = projectRepository;
        this.ticketRepository = ticketRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public DashboardMetricsResponse metrics() {
        TenantContext ctx = TenantContextHolder.getRequired();
        UUID orgId = UUID.fromString(ctx.orgId());
        UUID userId = ctx.userId() != null ? UUID.fromString(ctx.userId()) : null;

        long projects = projectRepository.countByOrgId(orgId);
        long myTickets = userId == null ? 0 : ticketRepository.countByOrgIdAndAssigneeId(orgId, userId);
        long members = membershipRepository.countByIdOrgId(orgId);

        return new DashboardMetricsResponse(projects, myTickets, members);
    }
}
