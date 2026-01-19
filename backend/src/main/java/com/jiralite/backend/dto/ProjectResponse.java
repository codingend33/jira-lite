package com.jiralite.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String key,
        String name,
        String description,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
