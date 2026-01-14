package com.jiralite.backend.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for demo echo endpoint.
 */
@Schema(description = "Echo request")
public record EchoRequest(
    @NotBlank(message = "title must not be blank")
    @Schema(description = "Title to echo", example = "Hello World")
    String title
) {
}

