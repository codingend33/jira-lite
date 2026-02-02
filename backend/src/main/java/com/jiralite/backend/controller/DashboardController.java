package com.jiralite.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jiralite.backend.dto.DashboardMetricsResponse;
import com.jiralite.backend.service.DashboardService;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<DashboardMetricsResponse> metrics() {
        return ResponseEntity.ok(dashboardService.metrics());
    }
}
