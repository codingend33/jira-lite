package com.jiralite.backend.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiralite.backend.entity.AuditLogEntity;
import com.jiralite.backend.repository.AuditLogRepository;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;

class AuditLogAspectTest {

    private final AuditLogRepository repo = Mockito.mock(AuditLogRepository.class);
    private final AuditLogAspect aspect = new AuditLogAspect(repo, new ObjectMapper());

    @BeforeEach
    void setup() {
        TenantContextHolder.set(new TenantContext(UUID.randomUUID().toString(), UUID.randomUUID().toString(), null, null));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "cred"));
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void writesAuditLogAfterInvocation() {
        LogAudit annotation = DummyService.class.getMethods()[0].getAnnotation(LogAudit.class);
        aspect.after(annotation, null, UUID.randomUUID());

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("TEST");
        assertThat(captor.getValue().getEntityType()).isEqualTo("ENTITY");
    }

    static class DummyService {
        @LogAudit(action = "TEST", entityType = "ENTITY")
        public void method() {
        }
    }
}
