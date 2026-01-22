# Jira Lite

Multi-tenant ticket system (portfolio project) for AU junior backend/full-stack roles.

## Tech Stack

- Backend: Java 17, Spring Boot 3, Spring Security, JPA/Hibernate, Flyway, OpenAPI
- Frontend: React + TypeScript, React Router, React Query, MUI
- AWS: Cognito, S3 (pre-signed), RDS Postgres, ECS Fargate, ALB, CloudWatch, ECR, CloudFront + S3
- IaC: Terraform
- CI/CD: GitHub Actions

## Workflow

Issues → Branch → PR → Review → Merge

## Quick Start (Local)

Prereqs: Docker Desktop, Java 17

```bash
cp .env.example .env
docker compose up -d

# Windows
.\mvnw.cmd --% spring-boot:run -Dspring-boot.run.profiles=local
```

Verify:

```bash
curl.exe -i http://localhost:8080/health
```

Frontend (Vite):

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Full local runbook: `docs/runbooks/local-dev.md`

## Documentation

- Runbook: `docs/runbooks/local-dev.md`
- API: `docs/design/openapi.md`
- Error handling: `docs/design/error-handling.md`
- TraceId: `docs/design/observability-traceid.md`
- Auth/RBAC: `docs/design/auth.md`
- Frontend auth: `docs/design/frontend-auth.md`
- Frontend data: `docs/design/frontend-data.md`
- UI pages: `docs/design/ui-pages.md`
- Multi-tenancy: `docs/design/multi-tenancy.md`
- Architecture: `docs/design/architecture.md`
- ERD: `docs/design/full_ERD.md`
- ADRs: `docs/adr/`

## Milestones (Summary)

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

## Tests

```bash
# Windows
.\mvnw.cmd test
```

Optional Testcontainers (Docker required):

```bash
.\mvnw.cmd test -Dtest=OrgMembersTcIntegrationTest -DrunTestcontainers=true
.\mvnw.cmd test -Dtest=TicketsTcIntegrationTest -DrunTestcontainers=true
.\mvnw.cmd test -Dtest=TicketCommentsTcIntegrationTest -DrunTestcontainers=true
.\mvnw.cmd test -Dtest=TicketAttachmentsTcIntegrationTest -DrunTestcontainers=true
```
