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
import com.jiralite.backend.repository.OrgRepository;
import com.jiralite.backend.repository.ProjectRepository;
import com.jiralite.backend.security.TestJwtDecoderConfig;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class ProjectsIntegrationTest {

    private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROJECT_1 = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private static final UUID PROJECT_2 = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll();
        orgRepository.deleteAll();

        orgRepository.save(org("Org One", ORG_1));
        orgRepository.save(org("Org Two", ORG_2));

        projectRepository.save(project(PROJECT_1, ORG_1, "JIRA", "Jira Lite", "ACTIVE"));
        projectRepository.save(project(PROJECT_2, ORG_2, "OPS", "Ops Portal", "ACTIVE"));
    }

    @Test
    void list_requires_authentication() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void member_lists_only_current_org_projects() throws Exception {
        mockMvc.perform(get("/projects")
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(PROJECT_1.toString()));
    }

    @Test
    void member_cannot_create_project() throws Exception {
        String payload = "{\"key\":\"APP\",\"name\":\"App Platform\"}";
        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer member-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void admin_can_create_project() throws Exception {
        String payload = "{\"key\":\"APP\",\"name\":\"App Platform\",\"description\":\"Core apps\"}";
        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("APP"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void admin_cannot_access_other_org_project() throws Exception {
        mockMvc.perform(get("/projects/{projectId}", PROJECT_2)
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void admin_can_archive_project() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/archive", PROJECT_1)
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    private OrgEntity org(String name, UUID id) {
        OrgEntity org = new OrgEntity();
        org.setId(id);
        org.setName(name);
        org.setCreatedAt(OffsetDateTime.now());
        org.setUpdatedAt(OffsetDateTime.now());
        return org;
    }

    private ProjectEntity project(UUID id, UUID orgId, String key, String name, String status) {
        ProjectEntity project = new ProjectEntity();
        project.setId(id);
        project.setOrgId(orgId);
        project.setProjectKey(key);
        project.setName(name);
        project.setDescription("Seeded project");
        project.setStatus(status);
        project.setCreatedAt(OffsetDateTime.now());
        project.setUpdatedAt(OffsetDateTime.now());
        return project;
    }
}
