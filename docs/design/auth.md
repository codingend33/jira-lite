# Auth (Cognito JWT + RBAC)

## Overview

The backend is a Spring Security OAuth2 Resource Server that validates AWS Cognito JWTs.
RBAC is based on Cognito groups and mapped into Spring roles.

## JWT Claims

- **sub**: user id (Cognito subject)
- **custom:org_id**: organization id (configurable via `app.security.org-claim`)
- **cognito:groups**: list of groups used for RBAC (`ADMIN`, `MEMBER`)

## RBAC Mapping

| Cognito Group | Spring Authority |
| ------------- | ---------------- |
| ADMIN         | ROLE_ADMIN       |
| MEMBER        | ROLE_MEMBER      |

## Configuration (Local)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${COGNITO_ISSUER_URI:https://cognito-idp.<region>.amazonaws.com/<userPoolId>}

app:
  security:
    org-claim: ${ORG_CLAIM:custom:org_id}
```

## Mermaid

### C4 Context

```mermaid
C4Context
title jira-lite auth context
Person(user, "User")
System(fe, "Frontend")
System(be, "Backend (Resource Server)")
System_Ext(cognito, "AWS Cognito")
Rel(user, fe, "Login")
Rel(fe, cognito, "Authenticate")
Rel(fe, be, "Call APIs with JWT")
Rel(be, cognito, "Validate JWT via issuer/JWKS")
```

### Sequence

```mermaid
sequenceDiagram
autonumber
participant U as User
participant FE as Frontend
participant C as Cognito User Pool
participant BE as Backend (Spring Boot)
participant SS as Spring Security
participant TC as TenantContextFilter
participant API as Controller/Service

U->>FE: Login (email/password)
FE->>C: Authenticate
C-->>FE: JWTs (access/id/refresh)

FE->>BE: API request + Authorization: Bearer access_token
BE->>SS: Filter chain starts
SS->>SS: Decode & verify JWT (issuer, signature, exp)
SS-->>SS: Build Authentication (principal=Jwt, authorities)
SS-->>TC: Continue filters
TC->>TC: Extract sub/orgId/groups + traceId
TC->>TC: TenantContextHolder.set(...)
TC-->>API: Proceed to controller/service
API-->>TC: Return response
TC->>TC: finally TenantContextHolder.clear()
TC-->>FE: HTTP response
```
