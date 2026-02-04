package com.jiralite.backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public Page<AuditLogEntity> listPaged(int page, int size, String action, UUID actorUserId) {
        UUID orgId = UUID.fromString(TenantContextHolder.getRequired().orgId());
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)));
        boolean hasAction = action != null && !action.isBlank();
        if (actorUserId != null && hasAction) {
            return auditLogRepository.findByTenantIdAndActorUserIdAndActionContainingIgnoreCaseOrderByCreatedAtDesc(
                    orgId, actorUserId, action, pageable);
        } else if (actorUserId != null) {
            return auditLogRepository.findByTenantIdAndActorUserIdOrderByCreatedAtDesc(orgId, actorUserId, pageable);
        } else if (hasAction) {
            return auditLogRepository.findByTenantIdAndActionContainingIgnoreCaseOrderByCreatedAtDesc(
                    orgId, action, pageable);
        } else {
            return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(orgId, pageable);
        }
    }
}
