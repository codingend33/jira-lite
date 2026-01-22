package com.jiralite.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jiralite.backend.entity.OrgEntity;
import com.jiralite.backend.entity.ProjectEntity;
import com.jiralite.backend.entity.TicketAttachmentEntity;
import com.jiralite.backend.entity.TicketEntity;
import com.jiralite.backend.repository.OrgRepository;
import com.jiralite.backend.repository.ProjectRepository;
import com.jiralite.backend.repository.TicketAttachmentRepository;
import com.jiralite.backend.repository.TicketRepository;
import com.jiralite.backend.security.TestJwtDecoderConfig;
import com.jiralite.backend.service.S3PresignService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class TicketAttachmentsIntegrationTest {

    private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROJECT_1 = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private static final UUID PROJECT_2 = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");
    private static final UUID TICKET_1 = UUID.fromString("cccccccc-3333-3333-3333-333333333333");
    private static final UUID TICKET_2 = UUID.fromString("dddddddd-4444-4444-4444-444444444444");
    private static final UUID ATTACHMENT_1 = UUID.fromString("eeeeeeee-5555-5555-5555-555555555555");
    private static final UUID ATTACHMENT_2 = UUID.fromString("ffffffff-6666-6666-6666-666666666666");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketAttachmentRepository attachmentRepository;

    @MockBean
    private S3PresignService s3PresignService;

    @BeforeEach
    void setUp() throws Exception {
        attachmentRepository.deleteAll();
        ticketRepository.deleteAll();
        projectRepository.deleteAll();
        orgRepository.deleteAll();

        orgRepository.save(org("Org One", ORG_1));
        orgRepository.save(org("Org Two", ORG_2));

        projectRepository.save(project(PROJECT_1, ORG_1, "JIRA", "Jira Lite"));
        projectRepository.save(project(PROJECT_2, ORG_2, "OPS", "Ops Portal"));

        ticketRepository.save(ticket(TICKET_1, ORG_1, PROJECT_1, "JIRA-1", "First ticket"));
        ticketRepository.save(ticket(TICKET_2, ORG_2, PROJECT_2, "OPS-1", "Other org"));

        attachmentRepository.save(attachment(ATTACHMENT_1, ORG_1, TICKET_1, "log.txt", "text/plain", 12L, "UPLOADED"));
        TicketAttachmentEntity missingKey = attachment(ATTACHMENT_2, ORG_1, TICKET_1, "empty.txt", "text/plain", 3L, "PENDING");
        missingKey.setS3Key("");
        attachmentRepository.save(missingKey);

        when(s3PresignService.presignUpload(any(), any()))
                .thenReturn(new S3PresignService.PresignResult(
                        new URL("https://example.com/upload"),
                        Map.of("Content-Type", "text/plain"),
                        OffsetDateTime.now().plusMinutes(5)));

        when(s3PresignService.presignDownload(any(), any(), any()))
                .thenReturn(new S3PresignService.PresignResult(
                        new URL("https://example.com/download"),
                        Map.of(),
                        OffsetDateTime.now().plusMinutes(5)));
    }

    @Test
    void list_requires_authentication() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/attachments", TICKET_1))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void member_lists_attachments() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/attachments", TICKET_1)
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fileName").value("log.txt"));
    }

    @Test
    void member_can_presign_upload() throws Exception {
        String payload = "{\"fileName\":\"log.txt\",\"contentType\":\"text/plain\",\"fileSize\":12}";
        mockMvc.perform(post("/tickets/{ticketId}/attachments/presign-upload", TICKET_1)
                        .header("Authorization", "Bearer member-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attachmentId").value(notNullValue()))
                .andExpect(jsonPath("$.uploadUrl").value("https://example.com/upload"));
    }

    @Test
    void member_can_presign_download() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/attachments/{attachmentId}/presign-download", TICKET_1, ATTACHMENT_1)
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").value("https://example.com/download"));
    }

    @Test
    void member_cannot_access_other_org_attachment() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/attachments", TICKET_2)
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void confirm_upload_updates_status() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/attachments/{attachmentId}/confirm", TICKET_1, ATTACHMENT_1)
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"));
    }

    @Test
    void confirm_upload_rejects_wrong_ticket() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/attachments/{attachmentId}/confirm", TICKET_2, ATTACHMENT_1)
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void presign_download_requires_s3_key() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/attachments/{attachmentId}/presign-download", TICKET_1, ATTACHMENT_2)
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
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

    private TicketAttachmentEntity attachment(UUID id, UUID orgId, UUID ticketId, String fileName,
            String contentType, long fileSize, String status) {
        TicketAttachmentEntity attachment = new TicketAttachmentEntity();
        attachment.setId(id);
        attachment.setOrgId(orgId);
        attachment.setTicketId(ticketId);
        attachment.setFileName(fileName);
        attachment.setContentType(contentType);
        attachment.setFileSize(fileSize);
        attachment.setUploadStatus(status);
        attachment.setS3Key("org/" + orgId + "/tickets/" + ticketId + "/" + id);
        attachment.setCreatedAt(OffsetDateTime.now());
        attachment.setUpdatedAt(OffsetDateTime.now());
        return attachment;
    }
}
