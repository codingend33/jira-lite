package com.jiralite.backend;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jiralite.backend.security.TestJwtDecoderConfig;

/**
 * Security integration tests for JWT auth, RBAC, and TenantContext.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class) // import test jwt decoder config，it is used to decode the jwt token in the test
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // test health endpoint is public
    @Test
    void health_is_public() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    // test whoami endpoint requires authentication
    @Test
    void whoami_requires_authentication() throws Exception {
        mockMvc.perform(get("/debug/whoami"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    // test admin-only endpoint requires admin role
    @Test
    void admin_only_rejects_member() throws Exception {
        mockMvc.perform(get("/debug/admin-only")
                .header("Authorization", "Bearer member-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    // test admin-only endpoint allows admin role
    @Test
    void admin_only_allows_admin() throws Exception {
        mockMvc.perform(get("/debug/admin-only")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    // test whoami endpoint returns tenant context
    @Test
    void whoami_returns_tenant_context() throws Exception {
        mockMvc.perform(get("/debug/whoami")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orgId").value("org-1"))
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.roles", hasItem("ADMIN")))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }
}
