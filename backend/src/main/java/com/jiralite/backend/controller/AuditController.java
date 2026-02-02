package com.jiralite.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jiralite.backend.entity.AuditLogEntity;
import com.jiralite.backend.service.AuditService;

@RestController
@RequestMapping("/audit/logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLogEntity>> listLogs() {
        return ResponseEntity.ok(auditService.listRecent());
    }
}
