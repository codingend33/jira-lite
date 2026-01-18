package com.jiralite.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jiralite.backend.dto.DebugWhoAmIResponse;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Debug endpoints to validate authentication and TenantContext wiring.
 */
@RestController
@RequestMapping("/debug")
@Tag(name = "Debug", description = "Debug endpoints for auth and tenant context")
public class DebugController {

    @GetMapping("/whoami")
    @Operation(summary = "Return the current authenticated tenant context")
    public ResponseEntity<DebugWhoAmIResponse> whoAmI() {
        TenantContext context = TenantContextHolder.getRequired();
        return ResponseEntity.ok(new DebugWhoAmIResponse(
                context.orgId(),
                context.userId(),
                context.roles(),
                context.traceId()));
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin-only endpoint for RBAC verification")
    public ResponseEntity<Map<String, String>> adminOnly() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
