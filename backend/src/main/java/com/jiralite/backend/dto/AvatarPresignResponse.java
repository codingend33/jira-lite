package com.jiralite.backend.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record AvatarPresignResponse(
        String uploadUrl,
        Map<String, String> headers,
        OffsetDateTime expiresAt,
        String key
) {
}
