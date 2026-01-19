package com.jiralite.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiralite.backend.entity.OrgEntity;
import com.jiralite.backend.entity.OrgMembershipEntity;
import com.jiralite.backend.entity.OrgMembershipId;
import com.jiralite.backend.entity.UserEntity;
import com.jiralite.backend.repository.OrgMembershipRepository;
import com.jiralite.backend.repository.OrgRepository;
import com.jiralite.backend.repository.UserRepository;
import com.jiralite.backend.security.TestJwtDecoderConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Testcontainers
class OrgMembersTcIntegrationTest {

    private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID USER_C = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("jira_lite")
            .withUsername("jira_lite")
            .withPassword("jira_lite_password");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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
        membershipRepository.save(membership(ORG_2, USER_C, "MEMBER", "ACTIVE"));
    }

    @Test
    void admin_lists_only_org1_members() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/org/members"),
                HttpMethod.GET,
                authEntity("admin-token", null),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).get("userId").asText()).isEqualTo(USER_A.toString());
    }

    @Test
    void admin_cannot_delete_other_org_member() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/org/members/" + USER_B),
                HttpMethod.DELETE,
                authEntity("admin-token", null),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("code").asText()).isEqualTo("NOT_FOUND");
    }

    @Test
    void member_cannot_create_member() throws Exception {
        String payload = "{\"userId\":\"" + USER_C + "\"}";
        ResponseEntity<String> response = restTemplate.exchange(
                url("/org/members"),
                HttpMethod.POST,
                authEntity("member-token", payload),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("code").asText()).isEqualTo("FORBIDDEN");
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpEntity<String> authEntity(String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return new HttpEntity<>(body, headers);
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
