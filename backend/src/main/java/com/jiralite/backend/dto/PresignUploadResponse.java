package com.jiralite.backend.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record PresignUploadResponse(
        UUID attachmentId,
        String uploadUrl,
        Map<String, String> headers,
        OffsetDateTime expiresAt
) {
}
