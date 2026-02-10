# Ticket Comments

## Overview

Comments are scoped to the current org and ticket. The author is derived from JWT claims.

## Sequence

```mermaid
sequenceDiagram
title comment-create
participant U as Member
participant API as Backend
participant SEC as Spring Security
participant TC as TenantContext
participant SVC as TicketCommentService
participant DB as Postgres

U->>API: POST /tickets/{id}/comments + JWT
API->>SEC: Validate JWT
SEC-->>TC: Authenticated
TC->>SVC: orgId from TenantContext
SVC->>DB: Verify ticket belongs to org
DB-->>SVC: OK
SVC->>DB: Insert comment
DB-->>SVC: Comment
SVC-->>API: CommentResponse
```
