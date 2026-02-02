package com.jiralite.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jiralite.backend.entity.AuditLogEntity;
import com.jiralite.backend.repository.AuditLogRepository;
import com.jiralite.backend.security.tenant.TenantContextHolder;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntity> listRecent() {
        UUID orgId = UUID.fromString(TenantContextHolder.getRequired().orgId());
        return auditLogRepository.findTop50ByTenantIdOrderByCreatedAtDesc(orgId);
    }
}
