package com.jiralite.backend.security.tenant;

import java.util.Optional;

/**
 * Thread-local storage for TenantContext.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantContext context) {
        CONTEXT.set(context);
    }

    public static Optional<TenantContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static TenantContext getRequired() {
        TenantContext context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("TenantContext is not set");
        }
        return context;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
