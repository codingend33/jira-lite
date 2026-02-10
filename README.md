# Jira Lite

Multi-tenant ticketing system (portfolio project) targeting AU junior backend/full-stack roles.

## Tech Stack

- Backend: Java 17, Spring Boot 3, Spring Security, JPA/Hibernate, Flyway
- Backend Tests: JUnit 5, Mockito, Testcontainers (PostgreSQL), ArchUnit, JaCoCo
- Frontend: React + TypeScript, React Router, React Query, MUI, Vitest + RTL
- AWS: Cognito, Lambda (Pre Token Generation), RDS PostgreSQL, ECR, EC2 (Docker), CloudWatch, S3, CloudFront
- IaC: Terraform
- CI/CD: GitHub Actions (PR checks + main-branch deploy)

## Current Scope

- Auth and RBAC with Cognito JWT (`ADMIN` / `MEMBER`)
- Multi-tenant isolation by organization context
- Projects, tickets, comments, and attachments
- Notification center with SSE stream + unread/read APIs
- Soft delete and trash workflow with role-aware UI behavior
- Audit logging with traceable actions

## Quick Start (Local)

Prerequisites: Docker Desktop, Java 17, Node.js 20+

1. Start database

```bash
docker compose up -d
```

2. Start backend (Windows PowerShell)

```bash
.\mvnw.cmd --% spring-boot:run -Dspring-boot.run.profiles=local
```

3. Start frontend

```bash
cd frontend
copy .env.example .env.local
npm install
npm run dev
```

4. Verify backend

```bash
curl.exe -i http://localhost:8080/api/health
```

Full local runbook: `docs/runbooks/local-dev.md`

## CI/CD Summary

- PR to `main`
  - Backend: `Test`
  - Frontend: `Build and Deploy` job runs lint/test/build only (no deploy)
- Push/Merge to `main`
  - Backend: `Test` then `Build and Deploy`
  - Frontend: `Build and Deploy` (includes S3 sync + CloudFront invalidation)

Detailed runbook: `docs/runbooks/ci-cd.md`

## AWS Deployment (Production)

Architecture (current deployment target):

- Compute: EC2 t4g.micro running Docker container
- Database: RDS PostgreSQL (private subnet)
- Storage/CDN: S3 + CloudFront
- Auth: Cognito User Pool + Pre Token Generation Lambda
- CI/CD auth: GitHub OIDC role

Step-by-step setup: `docs/runbooks/getting-started-aws.md`

## Documentation

- Local dev: `docs/runbooks/local-dev.md`
- CI/CD: `docs/runbooks/ci-cd.md`
- AWS setup: `docs/runbooks/getting-started-aws.md`
- Terraform operations: `docs/runbooks/terraform.md`
- OpenAPI runtime endpoints: `docs/runbooks/openapi.md`
- Architecture: `docs/design/architecture.md`
- Auth/RBAC: `docs/design/auth.md`
- Multi-tenancy: `docs/design/multi-tenancy.md`
- Projects: `docs/design/projects.md`
- Tickets: `docs/design/tickets.md`
- Attachments: `docs/design/attachments.md`
- Comments: `docs/design/comments.md`
- UI pages: `docs/design/ui-pages.md`
- Error handling: `docs/design/error-handling.md`
- Trace ID and observability: `docs/design/observability-traceid.md`
- ERD: `docs/design/full_ERD.md`
- ADRs: `docs/adr/`

## Milestones

- Day 0: Repo bootstrap (docs, templates, base structure)
- Day 1: Local dev loop + DB + Flyway baseline
- Day 2: Error format + traceId + OpenAPI + layering tests
- Day 3: ERD + Flyway core schema enhancements
- Day 4: Cognito JWT auth + RBAC + TenantContext
- Day 5: Org members admin + tenant isolation tests
- Day 6: Projects CRUD + archive + OpenAPI updates
- Day 7: Tickets CRUD + pagination/filter/sort + status flow
- Day 8: Comments + attachments (S3 presigned) + tests/docs
- Day 9: Frontend pages + Cognito login + React Query data layer
- Day 10: AWS infrastructure (Terraform) and production deployment
- Day 11: Invitation flow and onboarding integration
- Day 12: Frontend polish and error/UX improvements
- Day 13: Test/coverage and CI/CD hardening
- Day 14: Notifications SSE, audit feed, "My Tickets", avatars/creator UX
- Day 15: Deletion strategy, trash/read-only behavior, token refresh flow, Lambda DB-connection fix

Work logs: `docs/work_logs/`

## Testing

Backend (Windows):

```bash
.\mvnw.cmd test
.\mvnw.cmd verify -DrunTestcontainers=true
```

Frontend:

```bash
cd frontend
npm run lint
npm run test
npm run test -- --coverage
npm run build
```
