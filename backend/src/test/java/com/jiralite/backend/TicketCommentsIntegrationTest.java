package com.jiralite.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.jiralite.backend.entity.ProjectEntity;
import com.jiralite.backend.entity.TicketCommentEntity;
import com.jiralite.backend.entity.TicketEntity;
import com.jiralite.backend.repository.OrgRepository;
import com.jiralite.backend.repository.ProjectRepository;
import com.jiralite.backend.repository.TicketCommentRepository;
import com.jiralite.backend.repository.TicketRepository;
import com.jiralite.backend.security.TestJwtDecoderConfig;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class TicketCommentsIntegrationTest {

    private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROJECT_1 = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private static final UUID PROJECT_2 = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");
    private static final UUID TICKET_1 = UUID.fromString("cccccccc-3333-3333-3333-333333333333");
    private static final UUID TICKET_2 = UUID.fromString("dddddddd-4444-4444-4444-444444444444");
    private static final UUID COMMENT_1 = UUID.fromString("eeeeeeee-5555-5555-5555-555555555555");

    @Autowired
    private MockMvc mockMvc;

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
    void list_requires_authentication() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/comments", TICKET_1))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void member_lists_comments_for_ticket() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/comments", TICKET_1)
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].body").value("Seed comment"));
    }

    @Test
    void member_cannot_access_other_org_comments() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/comments", TICKET_2)
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void member_can_create_comment() throws Exception {
        String payload = "{\"body\":\"Hello\"}";
        mockMvc.perform(post("/tickets/{ticketId}/comments", TICKET_1)
                        .header("Authorization", "Bearer member-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Hello"))
                .andExpect(jsonPath("$.authorId").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void create_comment_requires_body() throws Exception {
        String payload = "{\"body\":\"\"}";
        mockMvc.perform(post("/tickets/{ticketId}/comments", TICKET_1)
                        .header("Authorization", "Bearer member-token")
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
