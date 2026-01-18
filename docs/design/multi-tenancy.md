# Multi-Tenancy (TenantContext)

## Overview

Each request derives tenant context from the authenticated JWT.
The context is stored in a ThreadLocal and cleared after the request finishes.

## TenantContext Fields

- **orgId**: from JWT claim `custom:org_id` (configurable)
- **userId**: from JWT claim `sub`
- **roles**: derived from Spring authorities (ROLE_ADMIN/MEMBER)
- **traceId**: from MDC key `traceId`

## Enforcement Rules

- `orgId` must be sourced from JWT claims; never accept `org_id` from clients.
- Services should use `TenantContextHolder.getRequired()` to fetch orgId/userId.
- Repository methods should scope queries by orgId.

## Mermaid

### C4 Context

```mermaid
C4Context
title jira-lite tenancy context
System(be, "Backend")
System_Ext(cognito, "AWS Cognito")
Rel(be, cognito, "Read org_id from JWT claims")
```

### Sequence

```mermaid
sequenceDiagram
title tenant-context-flow
participant SEC as Security
participant TEN as TenantContextFilter
participant SVC as Service
participant REPO as Repository
SEC-->>TEN: Authenticated JWT
TEN->>TEN: Build TenantContext(orgId,userId,roles,traceId)
TEN-->>SVC: Request handling
SVC->>REPO: Query with orgId constraint
TEN-->>TEN: Clear TenantContext + MDC
```
