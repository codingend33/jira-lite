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

## Org Member Management

Admin member management endpoints are scoped to the current org derived from JWT claims.
All membership reads and writes must include `orgId` from `TenantContextHolder`.

## Projects

Project CRUD is tenant-scoped by orgId from the JWT claim. Services must query
projects using `orgId` from `TenantContextHolder` and never accept orgId from clients.

## Tickets

Ticket list/detail/create/update/transition must always scope by `orgId` from
`TenantContextHolder`. Any cross-org access should return 404 to avoid leakage.

## Comments & Attachments

Ticket comments and attachments must verify the ticket belongs to the current org.
All reads/writes must include `orgId` from `TenantContextHolder`.

### C4 Context

```mermaid
C4Context
title org-members-admin
Person(admin, "Admin User")
System(be, "Backend")
System_Ext(cognito, "AWS Cognito")
Rel(admin, cognito, "Login")
Rel(admin, be, "Manage org members")
Rel(be, cognito, "Validate JWT + org_id")
```

### Sequence

```mermaid
sequenceDiagram
title admin-member-update
participant A as Admin
participant BE as Backend
participant SS as Spring Security
participant TC as TenantContext
participant SVC as OrgMemberService
participant DB as Postgres

A->>BE: PATCH /org/members/{userId} + JWT
BE->>SS: Validate JWT
SS-->>TC: Authenticated
TC->>SVC: orgId from TenantContext
SVC->>DB: Find membership by orgId + userId
DB-->>SVC: Membership
SVC->>DB: Update role/status
DB-->>SVC: OK
SVC-->>BE: Response
```

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
