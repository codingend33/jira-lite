package com.jiralite.backend.security.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextHolderTest {

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
    }

    @Test
    void setAndGetWorks() {
        TenantContext ctx = new TenantContext("org", "user", Set.of("ADMIN"), "trace");
        TenantContextHolder.set(ctx);
        assertThat(TenantContextHolder.get()).contains(ctx);
    }

    @Test
    void getRequiredThrowsWhenMissing() {
        assertThatThrownBy(TenantContextHolder::getRequired)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void clearRemovesContext() {
        TenantContextHolder.set(new TenantContext("org", "user", Set.of(), "t"));
        TenantContextHolder.clear();
        assertThat(TenantContextHolder.get()).isEmpty();
    }
}
