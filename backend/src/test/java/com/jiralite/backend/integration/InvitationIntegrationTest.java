package com.jiralite.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jiralite.backend.config.TestCognitoConfig;
import com.jiralite.backend.security.TestJwtDecoderConfig;
import com.jiralite.backend.entity.InvitationEntity;
import com.jiralite.backend.entity.OrgEntity;
import com.jiralite.backend.entity.OrgMembershipEntity;
import com.jiralite.backend.entity.OrgMembershipId;
import com.jiralite.backend.entity.UserEntity;
import com.jiralite.backend.repository.InvitationRepository;
import com.jiralite.backend.repository.OrgMembershipRepository;
import com.jiralite.backend.repository.OrgRepository;
import com.jiralite.backend.repository.UserRepository;
import com.jiralite.backend.service.InvitationService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({ TestCognitoConfig.class, TestJwtDecoderConfig.class })
@Testcontainers
@EnabledIfSystemProperty(named = "runTestcontainers", matches = "true")
class InvitationIntegrationTest {

    private static final UUID ORG_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CREATOR_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID INVITEE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final String INVITEE_EMAIL = "invitee@example.com";

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

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrgMembershipRepository membershipRepository;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        invitationRepository.deleteAll();
        userRepository.deleteAll();
        orgRepository.deleteAll();

        OrgEntity org = new OrgEntity();
        org.setId(ORG_ID);
        org.setName("Test Org");
        org.setCreatedAt(OffsetDateTime.now());
        org.setUpdatedAt(OffsetDateTime.now());
        orgRepository.save(org);

        UserEntity creator = new UserEntity();
        creator.setId(CREATOR_ID);
        creator.setEmail("creator@example.com");
        creator.setCognitoSub(CREATOR_ID.toString());
        creator.setCreatedAt(OffsetDateTime.now());
        creator.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(creator);

        OrgMembershipEntity adminMembership = new OrgMembershipEntity();
        adminMembership.setId(new OrgMembershipId(ORG_ID, CREATOR_ID));
        adminMembership.setRole("ADMIN");
        adminMembership.setStatus("ACTIVE");
        adminMembership.setCreatedAt(OffsetDateTime.now());
        adminMembership.setUpdatedAt(OffsetDateTime.now());
        membershipRepository.save(adminMembership);
    }

    @Test
    void invitationFlowCreatesUserAndMembership() {
        String token = invitationService.createInvitation(ORG_ID, INVITEE_EMAIL, "MEMBER", CREATOR_ID);

        InvitationEntity invitation = invitationRepository.findByToken(token).orElseThrow();
        assertThat(invitation.getOrgId()).isEqualTo(ORG_ID);
        assertThat(invitation.getEmail()).isEqualTo(INVITEE_EMAIL);

        invitationService.acceptInvitation(token, INVITEE_ID, INVITEE_EMAIL);

        UserEntity invitee = userRepository.findById(INVITEE_ID).orElseThrow();
        assertThat(invitee.getEmail()).isEqualTo(INVITEE_EMAIL);
        assertThat(invitee.getCognitoSub()).isEqualTo(INVITEE_ID.toString());

        OrgMembershipEntity membership = membershipRepository.findById(new OrgMembershipId(ORG_ID, INVITEE_ID))
                .orElseThrow();
        assertThat(membership.getRole()).isEqualTo("MEMBER");
        assertThat(membership.getStatus()).isEqualTo("ACTIVE");

        assertThat(invitationRepository.findByToken(token)).isEmpty();
    }
}
