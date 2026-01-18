package com.jiralite.backend.security.tenant;

import java.util.Set;

/**
 * Immutable tenant context derived from the authenticated JWT.
 */
public record TenantContext(
        String orgId,
        String userId,
        Set<String> roles,
        String traceId) {
}
