# ADR-0004: Org Members Admin and Tenant Isolation

## Status

ACCEPTED

## Context

We need admin-controlled member management within a multi-tenant system:

- CRUD operations for org members
- Strict tenant isolation via org_id from JWT claims
- RBAC enforcement (ADMIN only)
- Tests must not rely on external JWKS

## Decision

1) Implement `/org/members` endpoints (GET/POST/PATCH/DELETE) as ADMIN-only.  
2) Resolve orgId from `TenantContextHolder` (JWT claim), never from request.  
3) All repository queries and mutations must scope by `orgId`.  
4) Tests use mocked `JwtDecoder` and H2 with `ddl-auto: create-drop`, while Flyway remains disabled.

## Consequences

### Positive

- Consistent tenant isolation for member management
- RBAC aligned with Cognito groups
- Deterministic tests without external dependencies

### Negative

- Cognito groups and DB roles can drift (future sync needed)
- H2 schema differs slightly from Postgres (integration tests recommended later)

## Follow-ups

- Add org membership validation against DB for all requests
- Sync Cognito groups with DB roles (e.g., admin-only update)
- Add pagination/sorting for member listing
