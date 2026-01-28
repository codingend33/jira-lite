package com.jiralite.backend.security.tenant;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Builds TenantContext from authenticated JWT claims and authorities.
 */
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String MDC_ORG_KEY = "org_id";
    private static final String MDC_USER_KEY = "user_id";

    private final String orgClaim;
    private final String traceIdKey;

    public TenantContextFilter(String orgClaim, String traceIdKey) {
        this.orgClaim = orgClaim;
        this.traceIdKey = traceIdKey;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Jwt jwt) {
                String userId = jwt.getSubject();
                String orgId = jwt.getClaimAsString(orgClaim);
                if ((orgId == null || orgId.isBlank()) && !"custom:org_id".equals(orgClaim)) {
                    orgId = jwt.getClaimAsString("custom:org_id");
                }
                Set<String> roles = authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .map(authority -> authority.substring("ROLE_".length()))
                        .collect(Collectors.toUnmodifiableSet());
                String traceId = MDC.get(traceIdKey);

                TenantContextHolder.set(new TenantContext(orgId, userId, roles, traceId));

                if (orgId != null && !orgId.isBlank()) {
                    MDC.put(MDC_ORG_KEY, orgId);
                }
                if (userId != null && !userId.isBlank()) {
                    MDC.put(MDC_USER_KEY, userId);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
            MDC.remove(MDC_ORG_KEY);
            MDC.remove(MDC_USER_KEY);
        }
    }
}
