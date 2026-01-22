# ADR-0009: Frontend Cognito Login + React Query Data Layer

## Status
Accepted

## Context
Day 9 requires a full frontend that integrates Cognito login and consumes backend APIs with consistent caching. The project must avoid hardcoding secrets and keep tenant context derived from JWT claims only.

## Decision
- Use Cognito Hosted UI (Authorization Code + PKCE) for login.
- Store access/id tokens in localStorage for MVP.
- Use React Query to manage API requests, caching, and invalidation.
- Centralize API client for Authorization header and error handling.

## Consequences
- Frontend depends on environment variables for Cognito and API URLs.
- Token refresh is not handled in MVP; follow-up required for refresh flow.
- React Query invalidation keeps UI consistent after mutations.

## Follow-ups
- Add refresh token rotation and silent renew.
- Add role-based UI toggles once roles are exposed to frontend.
