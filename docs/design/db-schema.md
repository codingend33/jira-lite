# Database Schema (MVP)

## Goals
- Multi-tenant isolation via `org_id` on tenant-owned tables
- Prevent cross-org references using composite FKs `(org_id, id)`
- Minimal RBAC via `org_memberships.role`: `ADMIN`, `MEMBER`

## ERD
```mermaid
erDiagram
  ORGS ||--o{ ORG_MEMBERSHIPS : has
  USERS ||--o{ ORG_MEMBERSHIPS : has
  ORGS ||--o{ PROJECTS : owns
  PROJECTS ||--o{ TICKETS : contains
  TICKETS ||--o{ TICKET_COMMENTS : has
  TICKETS ||--o{ TICKET_ATTACHMENTS : has
```


