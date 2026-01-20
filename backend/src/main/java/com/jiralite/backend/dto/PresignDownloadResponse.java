package com.jiralite.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PresignDownloadResponse(
        UUID attachmentId,
        String downloadUrl,
        OffsetDateTime expiresAt
) {
}
