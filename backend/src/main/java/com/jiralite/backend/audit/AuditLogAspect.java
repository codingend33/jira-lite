package com.jiralite.backend.audit;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiralite.backend.entity.AuditLogEntity;
import com.jiralite.backend.repository.AuditLogRepository;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@Aspect
@Component
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogAspect(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @AfterReturning(pointcut = "@annotation(logAudit)", returning = "result")
    public void after(LogAudit logAudit, JoinPoint joinPoint, Object result) {
        TenantContext context = TenantContextHolder.get().orElse(null);
        if (context == null) {
            return;
        }
        if (context.orgId() == null) {
            return;
        }
        String details = joinPoint == null ? null : buildDetails(joinPoint.getArgs(), result);
        UUID actor = currentUserId(context);
        String entityId = resolveEntityId(result, joinPoint == null ? new Object[]{} : joinPoint.getArgs());

        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(UUID.fromString(context.orgId()));
        entity.setActorUserId(actor);
        entity.setAction(logAudit.action());
        entity.setEntityType(logAudit.entityType());
        entity.setEntityId(entityId);
        entity.setDetails(details);
        entity.setCreatedAt(OffsetDateTime.now());

        persistAsync(entity);
    }

    private UUID currentUserId(TenantContext context) {
        try {
            return UUID.fromString(context.userId());
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveEntityId(Object result, Object[] args) {
        if (result instanceof UUID id) {
            return id.toString();
        }
        if (result != null) {
            try {
                var getter = result.getClass().getMethod("getId");
                Object val = getter.invoke(result);
                return val != null ? val.toString() : null;
            } catch (Exception ignored) {
            }
        }
        // fallback first UUID arg
        for (Object arg : args) {
            if (arg instanceof UUID uuid) {
                return uuid.toString();
            }
        }
        return null;
    }

    private String buildDetails(Object[] args, Object result) {
        try {
            Object safeArgs = sanitize(args);
            Object safeResult = sanitize(result);
            return objectMapper.writeValueAsString(new AuditPayload(safeArgs, safeResult));
        } catch (Exception e) {
            return null;
        }
    }

    private Object sanitize(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof jakarta.servlet.ServletRequest || obj instanceof jakarta.servlet.ServletResponse) {
            return obj.getClass().getSimpleName();
        }
        if (obj instanceof Object[] arr) {
            return java.util.Arrays.stream(arr).map(this::sanitize).toArray();
        }
        if (obj instanceof java.io.Serializable) {
            return obj;
        }
        return obj.toString();
    }

    @Async
    protected void persistAsync(AuditLogEntity entity) {
        auditLogRepository.save(entity);
    }

    record AuditPayload(Object args, Object result) {
    }
}
