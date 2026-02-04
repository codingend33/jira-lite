package com.jiralite.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jiralite.backend.entity.OrgEntity;
import com.jiralite.backend.entity.OrgMembershipEntity;
import com.jiralite.backend.entity.OrgMembershipId;
import com.jiralite.backend.entity.UserEntity;
import com.jiralite.backend.repository.OrgMembershipRepository;
import com.jiralite.backend.repository.OrgRepository;
import com.jiralite.backend.repository.UserRepository;
import com.jiralite.backend.security.TestJwtDecoderConfig;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class OrgMembersIntegrationTest {

    private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID USER_C = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrgMembershipRepository membershipRepository;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        orgRepository.deleteAll();

        orgRepository.save(org("Org One", ORG_1));
        orgRepository.save(org("Org Two", ORG_2));

        userRepository.save(user(USER_A, "user-a@example.com", "User A"));
        userRepository.save(user(USER_B, "user-b@example.com", "User B"));
        userRepository.save(user(USER_C, "user-c@example.com", "User C"));

        membershipRepository.save(membership(ORG_1, USER_A, "ADMIN", "ACTIVE"));
        membershipRepository.save(membership(ORG_2, USER_B, "ADMIN", "ACTIVE"));
    }

    @Test
    void list_requires_authentication() throws Exception {
        mockMvc.perform(get("/org/members"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void list_requires_admin_role() throws Exception {
        mockMvc.perform(get("/org/members")
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void admin_lists_only_current_org_members() throws Exception {
        mockMvc.perform(get("/org/members")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(USER_A.toString()));
    }

    @Test
    void admin_org2_lists_only_org2_members() throws Exception {
        mockMvc.perform(get("/org/members")
                        .header("Authorization", "Bearer admin-org2-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(USER_B.toString()));
    }

    @Test
    void admin_cannot_update_other_org_member() throws Exception {
        String payload = "{\"role\":\"MEMBER\"}";
        mockMvc.perform(patch("/org/members/{userId}", USER_B)
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void admin_cannot_delete_other_org_member() throws Exception {
        mockMvc.perform(delete("/org/members/{userId}", USER_B)
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void admin_can_create_member_in_current_org() throws Exception {
        String payload = "{\"userId\":\"" + USER_C + "\"}";
        mockMvc.perform(post("/org/members")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(USER_C.toString()))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void admin_can_update_member_role_and_status() throws Exception {
        // add a second admin so downgrading USER_A doesn't violate "only admin" rule
        UUID extraAdmin = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        userRepository.save(user(extraAdmin, "user-d@example.com", "User D"));
        membershipRepository.save(membership(ORG_1, extraAdmin, "ADMIN", "ACTIVE"));

        String payload = "{\"role\":\"MEMBER\",\"status\":\"DISABLED\"}";
        mockMvc.perform(patch("/org/members/{userId}", USER_A)
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_A.toString()))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void update_requires_role_or_status() throws Exception {
        String payload = "{}";
        mockMvc.perform(patch("/org/members/{userId}", USER_A)
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void update_rejects_invalid_role() throws Exception {
        String payload = "{\"role\":\"OWNER\"}";
        mockMvc.perform(patch("/org/members/{userId}", USER_A)
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    private OrgEntity org(String name, UUID id) {
        OrgEntity org = new OrgEntity();
        org.setId(id);
        org.setName(name);
        org.setCreatedAt(OffsetDateTime.now());
        org.setUpdatedAt(OffsetDateTime.now());
        return org;
    }

    private UserEntity user(UUID id, String email, String displayName) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return user;
    }

    private OrgMembershipEntity membership(UUID orgId, UUID userId, String role, String status) {
        OrgMembershipEntity membership = new OrgMembershipEntity();
        membership.setId(new OrgMembershipId(orgId, userId));
        membership.setRole(role);
        membership.setStatus(status);
        membership.setCreatedAt(OffsetDateTime.now());
        membership.setUpdatedAt(OffsetDateTime.now());
        return membership;
    }
}
