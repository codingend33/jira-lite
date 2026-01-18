package com.jiralite.backend.dto;

import java.util.Set;

/**
 * Debug response for current authenticated user context.
 */
public record DebugWhoAmIResponse(
        String orgId,
        String userId,
        Set<String> roles,
        String traceId) {
}
