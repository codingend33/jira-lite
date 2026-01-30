package com.jiralite.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jiralite.backend.entity.InvitationEntity;
import com.jiralite.backend.entity.OrgMembershipEntity;
import com.jiralite.backend.entity.OrgMembershipId;
import com.jiralite.backend.entity.UserEntity;
import com.jiralite.backend.repository.InvitationRepository;
import com.jiralite.backend.repository.OrgMembershipRepository;
import com.jiralite.backend.repository.UserRepository;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrgMembershipRepository membershipRepository;

    @Mock
    private CognitoIdentityProviderClient cognitoClient;

    @InjectMocks
    private InvitationService invitationService;

    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUpMocks() {
        when(invitationRepository.findByToken("token")).thenReturn(Optional.of(invitation()));
        when(membershipRepository.findAllByIdUserIdOrderByCreatedAtDesc(userId)).thenReturn(java.util.List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Mock Cognito adminGetUser to return email when not in token
        AdminGetUserResponse response = AdminGetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("email").value("invitee@example.com").build())
                .build();
        when(cognitoClient.adminGetUser(any(Consumer.class))).thenReturn(response);
        when(cognitoClient.adminUpdateUserAttributes(any(Consumer.class)))
                .thenReturn(AdminUpdateUserAttributesResponse.builder().build());
        when(cognitoClient.adminAddUserToGroup(any(Consumer.class)))
                .thenReturn(AdminAddUserToGroupResponse.builder().build());
    }

    @Test
    void fetchesEmailFromCognitoWhenMissingAndCreatesMembership() {
        invitationService.acceptInvitation("token", userId, null);

        // user saved with lower-case email
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();
        assertThat(savedUser.getId()).isEqualTo(userId);
        assertThat(savedUser.getEmail()).isEqualTo("invitee@example.com");

        // membership saved
        ArgumentCaptor<OrgMembershipEntity> membershipCaptor = ArgumentCaptor.forClass(OrgMembershipEntity.class);
        verify(membershipRepository).save(membershipCaptor.capture());
        OrgMembershipEntity savedMembership = membershipCaptor.getValue();
        assertThat(savedMembership.getId().getOrgId()).isEqualTo(orgId);
        assertThat(savedMembership.getId().getUserId()).isEqualTo(userId);
        assertThat(savedMembership.getRole()).isEqualTo("MEMBER");
    }

    private InvitationEntity invitation() {
        InvitationEntity inv = new InvitationEntity();
        inv.setId(UUID.randomUUID());
        inv.setOrgId(orgId);
        inv.setEmail("invitee@example.com");
        inv.setToken("token");
        inv.setRole("MEMBER");
        inv.setExpiresAt(Instant.now().plusSeconds(3600));
        inv.setCreatedAt(Instant.now());
        inv.setCreatedBy(UUID.randomUUID());
        return inv;
    }
}
