package com.jiralite.backend.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.jiralite.backend.entity.NotificationEntity;
import com.jiralite.backend.repository.NotificationRepository;
import com.jiralite.backend.security.tenant.TenantContextHolder;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final Map<UUID, Map<Integer, SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void createNotification(UUID userId, String type, String content) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setTenantId(UUID.fromString(TenantContextHolder.getRequired().orgId()));
        entity.setType(type);
        entity.setContent(content);
        entity.setIsRead(false);
        entity.setCreatedAt(OffsetDateTime.now());
        notificationRepository.save(entity);
        // push to SSE listeners
        emitToUser(userId, entity);
    }

    @Transactional(readOnly = true)
    public Page<NotificationEntity> listForCurrentUser(Pageable pageable) {
        var ctx = TenantContextHolder.getRequired();
        UUID userId = UUID.fromString(ctx.userId());
        return notificationRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(
                UUID.fromString(ctx.orgId()), userId, pageable);
    }

    @Transactional
    public void markRead(UUID id) {
        var ctx = TenantContextHolder.getRequired();
        UUID orgId = UUID.fromString(ctx.orgId());
        UUID userId = UUID.fromString(ctx.userId());
        notificationRepository.findByIdAndTenantIdAndUserId(id, orgId, userId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional(readOnly = true)
    public SseEmitter subscribeCurrentUser() {
        var ctx = TenantContextHolder.getRequired();
        UUID userId = UUID.fromString(ctx.userId());
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        int key = System.identityHashCode(emitter);
        emittersByUser.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(key, emitter);
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok").id(UUID.randomUUID().toString())
                    .reconnectTime(5000L));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    private void emitToUser(UUID userId, NotificationEntity entity) {
        var map = emittersByUser.get(userId);
        if (map == null) {
            return;
        }
        map.values().forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(entity, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });
    }

    private void removeEmitter(UUID userId, SseEmitter emitter) {
        var map = emittersByUser.get(userId);
        if (map != null) {
            map.remove(System.identityHashCode(emitter));
            if (map.isEmpty()) {
                emittersByUser.remove(userId);
            }
        }
    }
}
