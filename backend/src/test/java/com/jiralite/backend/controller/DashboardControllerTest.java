package com.jiralite.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.jiralite.backend.dto.DashboardMetricsResponse;
import com.jiralite.backend.service.DashboardService;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    DashboardService dashboardService;

    @InjectMocks
    DashboardController controller;

    private final UUID orgId = UUID.randomUUID();
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
    void metrics_returnsCountsFromRepositories() {
        when(dashboardService.metrics()).thenReturn(new DashboardMetricsResponse(3, 5, 7));

        ResponseEntity<DashboardMetricsResponse> resp = controller.metrics();

        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getActiveProjects()).isEqualTo(3L);
        assertThat(resp.getBody().getMyTickets()).isEqualTo(5L);
        assertThat(resp.getBody().getMembers()).isEqualTo(7L);
    }
}
