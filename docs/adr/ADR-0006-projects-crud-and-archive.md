# ADR-0006: Projects CRUD and Archive

## Status

Accepted

## Context

We need a Projects module that supports CRUD and archive/unarchive actions.
All access must be tenant-scoped by orgId derived from JWT claims (TenantContext).

## Decision

- Implement Projects CRUD + archive/unarchive endpoints scoped by orgId from TenantContext.
- Use `status` to represent ACTIVE/ARCHIVED instead of hard deletes for archive.
- Enforce org-scoped queries in repository/service methods.
- Provide MockMvc integration tests for auth, RBAC, and tenant isolation.

## Consequences

- Project writes are ADMIN-only; reads are allowed to ADMIN/MEMBER.
- Archive/unarchive is an update to `status` rather than deleting data.
- Future work: add search/pagination and map created_by from Cognito user records.
