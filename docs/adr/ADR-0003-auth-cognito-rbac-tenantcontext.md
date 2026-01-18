# ADR-0002: Cognito JWT + RBAC + TenantContext

## Status

ACCEPTED

## Context

We need production-grade authentication and authorization for Jira Lite:

- Validate AWS Cognito-issued JWTs
- Enforce RBAC with ADMIN/MEMBER roles
- Derive tenant context per request (orgId/userId/roles/traceId)
- Keep a unified error format for 401/403

## Decision

### 1. Cognito JWT Resource Server

- Use Spring Security OAuth2 Resource Server with `issuer-uri`.
- Validate signature, expiration, and issuer with JWKS metadata.

### 2. RBAC via Cognito Groups

- Map `cognito:groups` claim to Spring authorities.
- Groups `ADMIN`/`MEMBER` map to `ROLE_ADMIN`/`ROLE_MEMBER`.

### 3. TenantContext Filter

- Build TenantContext from authenticated JWT:
  - orgId from claim `custom:org_id` (configurable)
  - userId from claim `sub`
  - roles from authorities
  - traceId from MDC
- Store in ThreadLocal for the request and clear in finally.

### 4. Unified 401/403 Responses

- Use `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403).
- Return JSON `{ code, message, traceId }`.

## Consequences

### Positive

- Standardized JWT validation flow
- Clear role-based access control
- Tenant context available to services/repositories
- Consistent error format across security and business errors

### Negative

- Requires Cognito issuer configuration in each environment
- JWT claims must be present and consistent (`custom:org_id`, `cognito:groups`)

## Follow-ups

- Add DB membership validation beyond JWT claims
- Consider Pre Token Generation Lambda to enrich claims
- Expand RBAC rules for project/ticket permissions
