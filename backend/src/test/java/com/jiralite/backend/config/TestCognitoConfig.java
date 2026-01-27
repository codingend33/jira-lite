package com.jiralite.backend.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * Test configuration that provides a mock CognitoIdentityProviderClient.
 * This prevents tests from requiring actual AWS credentials.
 */
@TestConfiguration
public class TestCognitoConfig {

    @Bean
    @Primary
    public CognitoIdentityProviderClient cognitoClient() {
        return Mockito.mock(CognitoIdentityProviderClient.class);
    }
}
