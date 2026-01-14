package com.jiralite.backend.dto;

/**
 * Unified error response DTO.
 * All errors returned to clients must conform to this structure.
 */
public record ErrorResponse(
    String code,
    String message,
    String traceId
) {
}

