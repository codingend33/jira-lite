package com.jiralite.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
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
import com.jiralite.backend.entity.ProjectEntity;
import com.jiralite.backend.entity.TicketCommentEntity;
import com.jiralite.backend.entity.TicketEntity;
import com.jiralite.backend.repository.OrgRepository;
import com.jiralite.backend.repository.ProjectRepository;
import com.jiralite.backend.repository.TicketCommentRepository;
import com.jiralite.backend.repository.TicketRepository;
import com.jiralite.backend.security.TestJwtDecoderConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Testcontainers
@EnabledIfSystemProperty(named = "runTestcontainers", matches = "true")
class TicketCommentsTcIntegrationTest {

    private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROJECT_1 = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private static final UUID PROJECT_2 = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");
    private static final UUID TICKET_1 = UUID.fromString("cccccccc-3333-3333-3333-333333333333");
    private static final UUID TICKET_2 = UUID.fromString("dddddddd-4444-4444-4444-444444444444");
    private static final UUID COMMENT_1 = UUID.fromString("eeeeeeee-5555-5555-5555-555555555555");

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
        registry.add("app.s3.bucket", () -> "test-bucket");
        registry.add("app.s3.region", () -> "ap-southeast-2");
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
    private ProjectRepository projectRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketCommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
        projectRepository.deleteAll();
        orgRepository.deleteAll();

        orgRepository.save(org("Org One", ORG_1));
        orgRepository.save(org("Org Two", ORG_2));

        projectRepository.save(project(PROJECT_1, ORG_1, "JIRA", "Jira Lite"));
        projectRepository.save(project(PROJECT_2, ORG_2, "OPS", "Ops Portal"));

        ticketRepository.save(ticket(TICKET_1, ORG_1, PROJECT_1, "JIRA-1", "First ticket"));
        ticketRepository.save(ticket(TICKET_2, ORG_2, PROJECT_2, "OPS-1", "Other org"));

        commentRepository.save(comment(COMMENT_1, ORG_1, TICKET_1, "Seed comment"));
    }

    @Test
    void member_lists_comments_for_ticket() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/tickets/" + TICKET_1 + "/comments"),
                HttpMethod.GET,
                authEntity("member-token", null),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).get("body").asText()).isEqualTo("Seed comment");
    }

    @Test
    void member_cannot_access_other_org_comments() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/tickets/" + TICKET_2 + "/comments"),
                HttpMethod.GET,
                authEntity("member-token", null),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("code").asText()).isEqualTo("NOT_FOUND");
    }

    @Test
    void member_can_create_comment() throws Exception {
        String payload = "{\"body\":\"Hello\"}";
        ResponseEntity<String> response = restTemplate.exchange(
                url("/tickets/" + TICKET_1 + "/comments"),
                HttpMethod.POST,
                authEntity("member-token", payload),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("body").asText()).isEqualTo("Hello");
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

    private ProjectEntity project(UUID id, UUID orgId, String key, String name) {
        ProjectEntity project = new ProjectEntity();
        project.setId(id);
        project.setOrgId(orgId);
        project.setProjectKey(key);
        project.setName(name);
        project.setDescription("Seeded project");
        project.setStatus("ACTIVE");
        project.setCreatedAt(OffsetDateTime.now());
        project.setUpdatedAt(OffsetDateTime.now());
        return project;
    }

    private TicketEntity ticket(UUID id, UUID orgId, UUID projectId, String key, String title) {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(id);
        ticket.setOrgId(orgId);
        ticket.setProjectId(projectId);
        ticket.setTicketKey(key);
        ticket.setTitle(title);
        ticket.setDescription("Seeded ticket");
        ticket.setStatus("OPEN");
        ticket.setPriority("MEDIUM");
        ticket.setCreatedAt(OffsetDateTime.now());
        ticket.setUpdatedAt(OffsetDateTime.now());
        return ticket;
    }

    private TicketCommentEntity comment(UUID id, UUID orgId, UUID ticketId, String body) {
        TicketCommentEntity comment = new TicketCommentEntity();
        comment.setId(id);
        comment.setOrgId(orgId);
        comment.setTicketId(ticketId);
        comment.setAuthorId(null);
        comment.setBody(body);
        comment.setCreatedAt(OffsetDateTime.now());
        comment.setUpdatedAt(OffsetDateTime.now());
        return comment;
    }
}
