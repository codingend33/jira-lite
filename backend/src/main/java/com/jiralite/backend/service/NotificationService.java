package com.jiralite.backend.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jiralite.backend.entity.NotificationEntity;
import com.jiralite.backend.repository.NotificationRepository;
import com.jiralite.backend.security.tenant.TenantContextHolder;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void createNotification(UUID userId, String type, String content) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setTenantId(TenantContextHolder.getRequired().orgId());
        entity.setType(type);
        entity.setContent(content);
        entity.setIsRead(false);
        entity.setCreatedAt(OffsetDateTime.now());
        notificationRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> listForCurrentUser() {
        var ctx = TenantContextHolder.getRequired();
        UUID userId = UUID.fromString(ctx.userId());
        return notificationRepository.findTop50ByTenantIdAndUserIdOrderByCreatedAtDesc(ctx.orgId(), userId);
    }

    @Transactional
    public void markRead(UUID id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }
}
