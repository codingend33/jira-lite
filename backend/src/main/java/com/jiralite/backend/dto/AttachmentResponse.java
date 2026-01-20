package com.jiralite.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        String fileName,
        String contentType,
        long fileSize,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
