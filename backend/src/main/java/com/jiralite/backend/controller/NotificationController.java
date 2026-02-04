package com.jiralite.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.jiralite.backend.entity.NotificationEntity;
import com.jiralite.backend.service.NotificationService;
import com.jiralite.backend.dto.PageMeta;
import com.jiralite.backend.dto.PagedResponse;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationEntity>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)));
        Page<NotificationEntity> result = notificationService.listForCurrentUser(pr);
        PageMeta meta = new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
        return ResponseEntity.ok(new PagedResponse<>(result.getContent(), meta));
    }

    @GetMapping(path = "/stream", produces = "text/event-stream")
    public SseEmitter stream() {
        return notificationService.subscribeCurrentUser();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        notificationService.markRead(id);
        return ResponseEntity.noContent().build();
    }
}
