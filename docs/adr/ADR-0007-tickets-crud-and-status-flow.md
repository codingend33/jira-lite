# ADR-0007: Tickets CRUD and Status Flow

## Status

Accepted

## Context

The project requires a complete Tickets module with list/detail, create/edit,
and status transitions. All access must be tenant-scoped by orgId derived from
JWT claims.

## Decision

- Implement ticket list/detail/create/edit/transition endpoints under `/tickets`.
- Enforce orgId scoping in service/repository queries.
- Use a simple status flow: OPEN → IN_PROGRESS → DONE, and OPEN/IN_PROGRESS → CANCELLED.
- Return 404 for cross-org access to prevent data leakage.
- Provide paged list responses using a unified `content` + `page` structure.

## Consequences

- Ticket creation requires project ownership within the same org.
- Status transitions are validated and reject invalid changes with 400 errors.
- Future work: enforce assignee membership rules and add advanced filtering.
