package com.jiralite.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUserGlobalSignOutRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRemoveUserFromGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

@Service
public class CognitoService {

    private static final Logger logger = LoggerFactory.getLogger(CognitoService.class);

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    public CognitoService(CognitoIdentityProviderClient cognitoClient) {
        this.cognitoClient = cognitoClient;
    }

    public void removeUserFromOrg(String cognitoSub) {
        try {
            // Remove custom:org_id attribute by setting it to mutable empty/dummy or using
            // delete attribute logic
            // However, AdminUpdateUserAttributes can typically update standard and custom
            // attributes.
            // AWS Cognito doesn't explicitly support "deleting" an attribute value easily
            // via update,
            // usually you set it to empty string if allowed, or we might need
            // AdminDeleteUserAttributes if we want to remove it.
            // But typical pattern for clearing custom attr is often setting to empty or
            // specific "none" marker.
            // Let's assume setting it to empty string "" clears it for our app logic, or
            // remove strict check.
            // Ideally we use AdminDeleteUserAttributes, but let's stick to update to empty
            // first as it's safer.
            // Actually, let's look for delete capability if strictly required.
            // Update: SDK has AdminDeleteUserAttributes. Let's use that for "removing" the
            // connection.
            // But wait, removing the attribute might be better. Let's try AdminUpdate to
            // empty first as "soft delete"
            // or AdminDeleteUserAttributes if available.
            // Let's use AdminUpdateUserAttributes with a blank value.

            AttributeType attribute = AttributeType.builder()
                    .name("custom:org_id")
                    .value("")
                    .build();

            AdminUpdateUserAttributesRequest request = AdminUpdateUserAttributesRequest.builder()
                    .userPoolId(userPoolId)
                    .username(cognitoSub)
                    .userAttributes(attribute)
                    .build();

            cognitoClient.adminUpdateUserAttributes(request);
            logger.info("Cleared custom:org_id for user {}", cognitoSub);

        } catch (CognitoIdentityProviderException e) {
            logger.error("Failed to clear org_id for user {}: {}", cognitoSub, e.awsErrorDetails().errorMessage());
            // Soft failure: do not throw exception
        }
    }

    /**
     * Force sign out all sessions for a user so old tokens become invalid.
     */
    public void globalSignOut(String cognitoSub) {
        try {
            AdminUserGlobalSignOutRequest request = AdminUserGlobalSignOutRequest.builder()
                    .userPoolId(userPoolId)
                    .username(cognitoSub)
                    .build();
            cognitoClient.adminUserGlobalSignOut(request);
            logger.info("Global sign-out issued for user {}", cognitoSub);
        } catch (CognitoIdentityProviderException e) {
            logger.error("Failed global sign-out for user {}: {}", cognitoSub, e.awsErrorDetails().errorMessage());
        }
    }

    public void updateUserGroup(String cognitoSub, String oldRole, String newRole) {
        try {
            String oldGroup = mapRoleToGroup(oldRole);
            String newGroup = mapRoleToGroup(newRole);

            if (oldGroup.equals(newGroup)) {
                return;
            }

            // 1. Remove old group first (avoid duplicates)
            if (!oldGroup.isEmpty()) {
                AdminRemoveUserFromGroupRequest removeRequest = AdminRemoveUserFromGroupRequest.builder()
                        .userPoolId(userPoolId)
                        .username(cognitoSub)
                        .groupName(oldGroup)
                        .build();
                cognitoClient.adminRemoveUserFromGroup(removeRequest);
                logger.info("Removed user {} from group {}", cognitoSub, oldGroup);
            }

            // 2. Add to new group
            if (!newGroup.isEmpty()) {
                AdminAddUserToGroupRequest addRequest = AdminAddUserToGroupRequest.builder()
                        .userPoolId(userPoolId)
                        .username(cognitoSub)
                        .groupName(newGroup)
                        .build();
                cognitoClient.adminAddUserToGroup(addRequest);
                logger.info("Added user {} to group {}", cognitoSub, newGroup);
            }

        } catch (CognitoIdentityProviderException e) {
            logger.error("Failed to update user group for {}: {}", cognitoSub, e.awsErrorDetails().errorMessage());
            // Log but don't fail the transaction
        }
    }

    private String mapRoleToGroup(String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return "ADMIN"; // keep group names uppercase to match Cognito pool
        } else if ("MEMBER".equalsIgnoreCase(role)) {
            return "MEMBER";
        }
        return "";
    }
}
