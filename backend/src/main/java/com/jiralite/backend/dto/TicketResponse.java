package com.jiralite.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        UUID projectId,
        String key,
        String title,
        String description,
        String status,
        String priority,
        UUID assigneeId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
