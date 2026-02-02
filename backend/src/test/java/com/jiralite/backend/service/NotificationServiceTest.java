package com.jiralite.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jiralite.backend.entity.NotificationEntity;
import com.jiralite.backend.repository.NotificationRepository;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    private UUID orgId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();
        TenantContextHolder.set(new TenantContext(orgId.toString(), userId.toString(), Set.of("ADMIN"), "trace"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createNotification_persistsWithTenantAndUser() {
        notificationService.createNotification(userId, "TYPE", "hello");

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        NotificationEntity saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(orgId.toString());
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getType()).isEqualTo("TYPE");
        assertThat(saved.getContent()).isEqualTo("hello");
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void listForCurrentUser_queriesRepositoryWithContext() {
        notificationService.listForCurrentUser();

        verify(notificationRepository)
                .findTop50ByTenantIdAndUserIdOrderByCreatedAtDesc(orgId.toString(), userId);
    }

    @Test
    void markRead_updatesEntityWhenFound() {
        UUID notificationId = UUID.randomUUID();
        NotificationEntity entity = new NotificationEntity();
        entity.setIsRead(false);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(entity));

        notificationService.markRead(notificationId);

        assertThat(entity.isRead()).isTrue();
        verify(notificationRepository).save(entity);
    }
}
