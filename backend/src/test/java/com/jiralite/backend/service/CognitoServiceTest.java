package com.jiralite.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRemoveUserFromGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUserGlobalSignOutRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CognitoServiceTest {

    @Mock
    private CognitoIdentityProviderClient cognitoClient;

    @Test
    void updateUserGroup_removesOldThenAddsNew_withUppercaseGroups() {
        CognitoService service = new CognitoService(cognitoClient);

        service.updateUserGroup("user-1", "ADMIN", "MEMBER");

        InOrder inOrder = inOrder(cognitoClient);
        inOrder.verify(cognitoClient).adminRemoveUserFromGroup(AdminRemoveUserFromGroupRequest.builder()
                .groupName("ADMIN")
                .username("user-1")
                .build());
        inOrder.verify(cognitoClient).adminAddUserToGroup(AdminAddUserToGroupRequest.builder()
                .groupName("MEMBER")
                .username("user-1")
                .build());
    }

    @Test
    void globalSignOut_callsCognito() {
        CognitoService service = new CognitoService(cognitoClient);

        service.globalSignOut("user-2");

        verify(cognitoClient).adminUserGlobalSignOut(AdminUserGlobalSignOutRequest.builder()
                .username("user-2")
                .build());
    }
}
