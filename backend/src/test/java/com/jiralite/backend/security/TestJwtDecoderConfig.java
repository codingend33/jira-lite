package com.jiralite.backend.security;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Test-only JwtDecoder to avoid external JWKS calls.
 */
@TestConfiguration
public class TestJwtDecoderConfig {

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return token -> {
            if ("admin-token".equals(token)) {
                return buildJwt(token, "user-1", "11111111-1111-1111-1111-111111111111", List.of("ADMIN"));
            }
            if ("member-token".equals(token)) {
                return buildJwt(token, "user-2", "11111111-1111-1111-1111-111111111111", List.of("MEMBER"));
            }
            if ("admin-org2-token".equals(token)) {
                return buildJwt(token, "user-3", "22222222-2222-2222-2222-222222222222", List.of("ADMIN"));
            }
            if ("no-org-token".equals(token)) {
                return buildJwtWithoutOrg(token, "user-4", List.of("ADMIN"));
            }
            throw new JwtException("Invalid token");
        };
    }

    private Jwt buildJwt(String token, String subject, String orgId, List<String> groups) {
        Instant now = Instant.now();
        Map<String, Object> headers = Map.of("alg", "none");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        claims.put("custom:org_id", orgId);
        claims.put("org_id", orgId);
        claims.put("cognito:groups", groups);
        return new Jwt(token, now, now.plusSeconds(3600), headers, claims);
    }

    private Jwt buildJwtWithoutOrg(String token, String subject, List<String> groups) {
        Instant now = Instant.now();
        Map<String, Object> headers = Map.of("alg", "none");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        claims.put("cognito:groups", groups);
        return new Jwt(token, now, now.plusSeconds(3600), headers, claims);
    }
}
