package com.jiralite.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for demo echo endpoint.
 */
@Schema(description = "Echo response")
public record EchoResponse(
    @Schema(description = "Echoed title")
    String title,

    @Schema(description = "Timestamp in ISO format")
    String timestamp
) {
}

