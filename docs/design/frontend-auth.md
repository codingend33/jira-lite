# Frontend Auth (Cognito)

## Overview

Frontend uses Cognito Hosted UI with Authorization Code + PKCE. The app stores the access token and id token in localStorage for MVP and sends the access token as `Authorization: Bearer <token>`.

## Env vars

- `VITE_COGNITO_DOMAIN`
- `VITE_COGNITO_CLIENT_ID`
- `VITE_COGNITO_REDIRECT_URI`
- `VITE_COGNITO_LOGOUT_URI`

## C4 Context

```mermaid
C4Context
title day9-frontend-auth-context
Person(user, "User")
System(frontend, "React Frontend")
System(backend, "Spring Boot API")
System(cognito, "AWS Cognito")

Rel(user, frontend, "Uses")
Rel(frontend, cognito, "Login/Authorize")
Rel(frontend, backend, "API calls with JWT")
```

## Sequence (Login)

```mermaid
sequenceDiagram
autonumber
actor User
participant FE as Frontend
participant Cog as Cognito
participant API as Backend

User->>FE: Click Login
FE->>Cog: Redirect to Hosted UI (PKCE)
Cog-->>FE: Redirect back with code
FE->>Cog: Exchange code for tokens
Cog-->>FE: Tokens
FE->>API: GET /projects (Bearer token)
API-->>FE: 200 OK
```
