package com.jiralite.backend.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jiralite.backend.dto.ErrorCode;
import com.jiralite.backend.entity.InvitationEntity;
import com.jiralite.backend.entity.OrgMembershipEntity;
import com.jiralite.backend.entity.OrgMembershipId;
import com.jiralite.backend.entity.UserEntity;
import com.jiralite.backend.exception.ApiException;
import com.jiralite.backend.repository.InvitationRepository;
import com.jiralite.backend.repository.OrgMembershipRepository;
import com.jiralite.backend.repository.UserRepository;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

/**
 * Service for managing organization invitations.
 */
@Service
public class InvitationService {

    private static final Logger log = LoggerFactory.getLogger(InvitationService.class);
    private static final int INVITATION_EXPIRY_DAYS = 7;
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final OrgMembershipRepository membershipRepository;
    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    public InvitationService(
            InvitationRepository invitationRepository,
            UserRepository userRepository,
            OrgMembershipRepository membershipRepository,
            CognitoIdentityProviderClient cognitoClient) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.cognitoClient = cognitoClient;
    }

    /**
     * Create invitation for a new member.
     */
    @Transactional
    public String createInvitation(UUID orgId, String email, String role, UUID creatorId) {
        log.info("Creating invitation for email {} to org {} with role {}", email, orgId, role);

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(INVITATION_EXPIRY_DAYS, java.time.temporal.ChronoUnit.DAYS);

        InvitationEntity invitation = new InvitationEntity();
        invitation.setId(UUID.randomUUID());
        invitation.setOrgId(orgId);
        invitation.setEmail(email.toLowerCase());
        invitation.setToken(token);
        invitation.setRole(role);
        invitation.setExpiresAt(expiresAt);
        invitation.setCreatedAt(Instant.now());
        invitation.setCreatedBy(creatorId);

        invitationRepository.save(invitation);

        log.info("Created invitation with token {} for {}", token, email);
        return token;
    }

    /**
     * Accept invitation and add user to organization.
     */
    @Transactional
    public void acceptInvitation(String token, UUID userId, String email) {
        log.info("User {} accepting invitation with token {}", userId, token);

        // Validate email is present (required from JWT)
        if (email == null || email.isBlank()) {
            log.error("Email is missing from JWT for user {}", userId);
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Email is required to accept invitation. Please ensure your account has a verified email.", 400);
        }

        // Find and validate invitation
        InvitationEntity invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Invalid invitation token", 404));

        // Check expiry
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "This invitation has expired", 400);
        }

        // Verify email matches (case-insensitive)
        if (!invitation.getEmail().equalsIgnoreCase(email.trim())) {
            throw new ApiException(ErrorCode.FORBIDDEN,
                    "This invitation is for a different email address", 403);
        }

        // Check if user already belongs to this org
        List<OrgMembershipEntity> existingMemberships = membershipRepository
                .findAllByIdUserIdOrderByCreatedAtDesc(userId);
        boolean alreadyMember = existingMemberships.stream()
                .anyMatch(m -> m.getId().getOrgId().equals(invitation.getOrgId()));

        if (alreadyMember) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "You are already a member of this organization", 400);
        }

        OffsetDateTime now = OffsetDateTime.now();

        // Create or update user
        UserEntity user = userRepository.findById(userId).orElse(new UserEntity());
        if (user.getId() == null) {
            user.setId(userId);
            user.setEmail(email);
            user.setCognitoSub(userId.toString());
            user.setCreatedAt(now);
        }
        user.setUpdatedAt(now);
        userRepository.save(user);

        // Create membership
        OrgMembershipEntity membership = new OrgMembershipEntity();
        OrgMembershipId membershipId = new OrgMembershipId(invitation.getOrgId(), userId);
        membership.setId(membershipId);
        membership.setRole(invitation.getRole());
        membership.setStatus(STATUS_ACTIVE);
        membership.setCreatedAt(now);
        membership.setUpdatedAt(now);
        membershipRepository.save(membership);

        log.info("Created {} membership for user {} in org {}",
                invitation.getRole(), userId, invitation.getOrgId());

        // Update Cognito custom:org_id and add to group
        try {
            updateCognitoOrgId(userId.toString(), invitation.getOrgId().toString());
            log.info("Updated Cognito custom:org_id for user {}", userId);

            // Add user to Cognito group based on role
            addUserToGroup(userId.toString(), invitation.getRole());
            log.info("Added user {} to Cognito group {}", userId, invitation.getRole());
        } catch (Exception e) {
            log.error("Failed to update Cognito attribute for user {}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR,
                    "Invitation accepted but failed to update Cognito attributes. Please contact support.", 500);
        }

        // Delete invitation
        invitationRepository.delete(invitation);
        log.info("Deleted invitation token {} after successful acceptance", token);
    }

    /**
     * Get all pending invitations for an organization.
     */
    public List<InvitationEntity> getPendingInvitations(UUID orgId) {
        return invitationRepository.findByOrgId(orgId).stream()
                .filter(inv -> inv.getExpiresAt().isAfter(Instant.now()))
                .toList();
    }

    /**
     * Update Cognito user attribute custom:org_id.
     */
    private void updateCognitoOrgId(String userId, String orgId) {
        AdminUpdateUserAttributesRequest updateRequest = AdminUpdateUserAttributesRequest.builder()
                .userPoolId(userPoolId)
                .username(userId)
                .userAttributes(
                        AttributeType.builder()
                                .name("custom:org_id")
                                .value(orgId)
                                .build())
                .build();

        cognitoClient.adminUpdateUserAttributes(updateRequest);
    }

    /**
     * Add user to Cognito group for RBAC.
     */
    private void addUserToGroup(String userId, String groupName) {
        try {
            AdminAddUserToGroupRequest request = AdminAddUserToGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(userId)
                    .groupName(groupName)
                    .build();
            cognitoClient.adminAddUserToGroup(request);
        } catch (Exception e) {
            log.warn("Failed to add user {} to group {}: {}", userId, groupName, e.getMessage());
            // Don't throw - group membership is nice-to-have, org_id is critical
        }
    }
}
