package com.jiralite.backend.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jiralite.backend.dto.CreateOrganizationRequest;
import com.jiralite.backend.dto.CreateOrganizationResponse;
import com.jiralite.backend.dto.ErrorCode;
import com.jiralite.backend.entity.OrgEntity;
import com.jiralite.backend.entity.OrgMembershipEntity;
import com.jiralite.backend.entity.OrgMembershipId;
import com.jiralite.backend.entity.UserEntity;
import com.jiralite.backend.exception.ApiException;
import com.jiralite.backend.repository.OrgMembershipRepository;
import com.jiralite.backend.repository.OrgRepository;
import com.jiralite.backend.repository.UserRepository;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

/**
 * Service for user onboarding and organization creation.
 * Handles self-service org creation with Cognito integration.
 */
@Service
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final OrgRepository orgRepository;
    private final UserRepository userRepository;
    private final OrgMembershipRepository membershipRepository;
    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    public OnboardingService(
            OrgRepository orgRepository,
            UserRepository userRepository,
            OrgMembershipRepository membershipRepository,
            CognitoIdentityProviderClient cognitoClient) {
        this.orgRepository = orgRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.cognitoClient = cognitoClient;
    }

    /**
     * Create new organization and assign creator as ADMIN.
     * Updates Cognito custom:org_id attribute.
     */
    @Transactional
    public CreateOrganizationResponse createOrganization(
            UUID userId,
            String email,
            CreateOrganizationRequest request) {

        log.info("Creating organization for user {} with email {}", userId, email);

        // Check if user already has an organization
        if (!membershipRepository.findAllByIdUserIdOrderByCreatedAtDesc(userId).isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "User already belongs to an organization", 400);
        }

        OffsetDateTime now = OffsetDateTime.now();
        UUID orgId = UUID.randomUUID();

        // Create organization
        OrgEntity org = new OrgEntity();
        org.setId(orgId);
        org.setName(request.getName());
        org.setCreatedAt(now);
        org.setUpdatedAt(now);
        orgRepository.save(org);

        log.info("Created organization {} with id {}", request.getName(), orgId);

        // Create or update user
        UserEntity user = userRepository.findById(userId).orElse(new UserEntity());
        if (user.getId() == null) {
            user.setId(userId);
            user.setEmail(email);
            user.setCreatedAt(now);
        }
        user.setUpdatedAt(now);
        userRepository.save(user);

        // Create membership (ADMIN role)
        OrgMembershipEntity membership = new OrgMembershipEntity();
        OrgMembershipId membershipId = new OrgMembershipId();
        membershipId.setOrgId(orgId);
        membershipId.setUserId(userId);
        membership.setId(membershipId);
        membership.setRole(ROLE_ADMIN);
        membership.setStatus(STATUS_ACTIVE);
        membership.setCreatedAt(now);
        membership.setUpdatedAt(now);
        membershipRepository.save(membership);

        log.info("Created ADMIN membership for user {} in org {}", userId, orgId);

        // Update Cognito custom:org_id attribute
        try {
            updateCognitoOrgId(userId.toString(), orgId.toString());
            log.info("Updated Cognito custom:org_id for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to update Cognito attribute for user {}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR,
                    "Organization created but failed to update Cognito attributes. Please contact support.", 500);
        }

        return new CreateOrganizationResponse(
                orgId,
                request.getName(),
                "Organization created successfully. Please refresh your session to continue.");
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
}
