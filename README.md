# Jira Lite

Multi-tenant ticket system (portfolio project) for AU junior backend/full-stack roles.

## Tech Stack

- Backend: Java 17, Spring Boot 3, Spring Security, JPA/Hibernate, Flyway, OpenAPI
- Backend Tests: JUnit 5, Mockito, Testcontainers (PostgreSQL), ArchUnit
- Frontend: React + TypeScript, React Router, React Query, MUI, Vitest + RTL
- AWS: Cognito, S3 (pre-signed), RDS Postgres, ECS Fargate, ALB, CloudWatch, ECR, CloudFront + S3
- IaC: Terraform
- CI/CD: GitHub Actions (Branch Protection, PR Gates, Auto-Deploy)

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

## AWS Deployment (Production)

> **📘 完整部署指南**: [docs/runbooks/getting-started-aws.md](docs/runbooks/getting-started-aws.md)

### Architecture

Deployed on AWS using Terraform (IaC):
- **Compute**: EC2 t4g.micro (ARM) running Docker
- **Database**: RDS PostgreSQL t4g.micro (private subnet)
- **Storage**: S3 (attachments + frontend) + CloudFront CDN
- **Auth**: Cognito User Pool + Lambda Pre Token Generation
- **CI/CD**: GitHub Actions with OIDC authentication

**Cost**: ~$0-5/month (Free Tier optimized)

### Quick Deploy

```bash
# 1. Configure AWS CLI and create IAM user
# 2. Bootstrap Terraform state
cd infra/scripts && ./bootstrap-state.sh

# 3. Configure and deploy
cd ../terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars
terraform init && terraform apply

# 4. Configure GitHub Secrets/Variables
# 5. Push to main → auto deploy
```

**First time?** Follow the step-by-step guide: [getting-started-aws.md](docs/runbooks/getting-started-aws.md)

### Access URLs (after deployment)

- **Frontend**: `https://<cloudfront_domain>`
- **Backend API**: `http://<ec2_ip>:8080`
- **API Docs**: `http://<ec2_ip>:8080/swagger-ui.html`

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
- Day 10: AWS Infrastructure (Terraform) + Production Deployment
- Day 11: User Invitation Flow (Lambda Triggers + Email)
- Day 12: Frontend Polish (Toasts, Error Boundary, Loading States)
- Day 13: Engineering Excellence (72% Backend Coverage, 51% Frontend Coverage, CI/CD Hardening)

## Tests

```bash
# Windows
.\mvnw.cmd test
.\mvnw.cmd verify  # Generates JaCoCo coverage report
```

Frontend (Vitest):

```bash
cd frontend
npm run test       # Run unit/component tests
```

**Current Coverage (Day 13):**
- Backend: ~72% (Instruction), ~60% (Branch)
- Frontend: ~51% (Statement)

Optional Testcontainers (Docker required):

```bash
.\mvnw.cmd test -Dtest=OrgMembersTcIntegrationTest -DrunTestcontainers=true
.\mvnw.cmd test -Dtest=TicketsTcIntegrationTest -DrunTestcontainers=true
.\mvnw.cmd test -Dtest=TicketCommentsTcIntegrationTest -DrunTestcontainers=true
.\mvnw.cmd test -Dtest=TicketAttachmentsTcIntegrationTest -DrunTestcontainers=true
```

