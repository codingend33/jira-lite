package com.jiralite.backend.config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

import com.jiralite.backend.security.RestAccessDeniedHandler;
import com.jiralite.backend.security.RestAuthenticationEntryPoint;
import com.jiralite.backend.security.tenant.TenantContextFilter;

/**
 * Security configuration for JWT resource server and RBAC.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TenantContextFilter tenantContextFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/health",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                .permitAll()
                .anyRequest()
                .authenticated());

        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        http.addFilterAfter(tenantContextFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public TenantContextFilter tenantContextFilter(
            @Value("${app.security.org-claim:custom:org_id}") String orgClaim) {
        return new TenantContextFilter(orgClaim, "traceId");
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(cognitoGroupsConverter());
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>> cognitoGroupsConverter() {
        return jwt -> {
            Object groupsClaim = jwt.getClaim("cognito:groups");
            if (groupsClaim == null) {
                return List.of();
            }
            List<String> groups;
            if (groupsClaim instanceof Collection<?> collection) {
                groups = collection.stream()
                        .map(Object::toString)
                        .toList();
            } else {
                groups = List.of(groupsClaim.toString());
            }
            return groups.stream()
                    .filter(group -> "ADMIN".equals(group) || "MEMBER".equals(group))
                    .map(group -> new SimpleGrantedAuthority("ROLE_" + group))
                    .collect(Collectors.toUnmodifiableSet());
        };
    }
}
