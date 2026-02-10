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

---

## Frontend Integration (React + Cognito)

The frontend application uses **AWS Cognito Hosted UI** with **Authorization Code Flow + PKCE** for secure authentication.

### Environment Variables

- `VITE_COGNITO_DOMAIN`: The Cognito domain prefix.
- `VITE_COGNITO_CLIENT_ID`: The User Pool App Client ID.
- `VITE_COGNITO_REDIRECT_URI`: Callback URL after login (e.g., `http://localhost:5173`).
- `VITE_COGNITO_LOGOUT_URI`: URL to redirect to after logout.

### Login Flow (PKCE)

```mermaid
sequenceDiagram
autonumber
actor User
participant FE as Frontend
participant Cog as Cognito
participant API as Backend

User->>FE: Click Login
FE->>Cog: Redirect to Hosted UI (PKCE Challenge)
Cog-->>FE: Redirect back with Auth Code
FE->>Cog: Exchange Code for Tokens (Verifier)
Cog-->>FE: Returns { access_token, id_token }
FE->>FE: Store tokens in Memory/LocalStorage
FE->>API: API Request (Authorization: Bearer token)
API-->>FE: 200 OK
```
