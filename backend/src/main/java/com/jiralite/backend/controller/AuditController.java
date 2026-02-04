package com.jiralite.backend.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jiralite.backend.entity.AuditLogEntity;
import com.jiralite.backend.service.AuditService;
import com.jiralite.backend.dto.PageMeta;
import com.jiralite.backend.dto.PagedResponse;

@RestController
@RequestMapping("/audit/logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<AuditLogEntity>> listLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID actorUserId) {
        Page<AuditLogEntity> result = auditService.listPaged(page, size, action, actorUserId);
        PageMeta meta = new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
        return ResponseEntity.ok(new PagedResponse<>(result.getContent(), meta));
    }
}
