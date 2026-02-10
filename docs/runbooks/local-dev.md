# Local Development Runbook

This runbook is the practical local workflow for the current codebase.

## Prerequisites

- Docker Desktop
- Java 17
- Node.js 20+
- Maven Wrapper (`backend/mvnw`)

## 1) Start Local Database

From repo root:

```bash
docker compose up -d
```

Verify:

```bash
docker ps
docker logs -f jira_lite_postgres
```

Stop:

```bash
docker compose down
```

Reset (drop local DB volume):

```bash
docker compose down -v
docker compose up -d
```

## 2) Run Backend

From repo root (PowerShell):

```bash
.\mvnw.cmd --% spring-boot:run -Dspring-boot.run.profiles=local
```

Health check:

```bash
curl.exe -i http://localhost:8080/api/health
```

Notes:

- Backend context path is `/api`.
- Local profile reads DB config from `backend/src/main/resources/application-local.yml`.

## 3) Run Frontend

```bash
cd frontend
copy .env.example .env.local
npm install
npm run dev
```

Minimum `.env.local` values:

- `VITE_API_BASE_URL=http://localhost:8080/api`
- `VITE_COGNITO_DOMAIN=<your-cognito-domain>.auth.ap-southeast-2.amazoncognito.com`
- `VITE_COGNITO_CLIENT_ID=<your-client-id>`
- `VITE_COGNITO_REDIRECT_URI=http://localhost:5173/login`
- `VITE_COGNITO_LOGOUT_URI=http://localhost:5173/login`
- `VITE_COGNITO_REGION=ap-southeast-2`

## 4) Local Test Commands

Frontend:

```bash
cd frontend
npm run lint
npm run test
npm run test -- --coverage
npm run build
```

Backend:

```bash
cd backend
.\mvnw.cmd test
.\mvnw.cmd verify -DrunTestcontainers=true
```

## 5) API and Swagger

- Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

## 6) Common Local Verification

- Login and onboarding:
  - `/login`
  - `/create-org`
  - `/invite?token=...`
- Core pages:
  - `/dashboard`, `/projects`, `/tickets`, `/trash`
  - `/settings/profile`, `/settings/members`, `/notifications`

## 7) Frequent Issues

### PowerShell `curl` alias problem

Use `curl.exe`, not `curl`, to avoid `Invoke-WebRequest` alias issues.

### Session expires unexpectedly

If token TTL is short in Cognito app client, users will be redirected to login after expiry.

### 403 behavior differences

Some pages intentionally show in-page permission messaging (for example, members viewing trash or members settings page), while global forbidden toasts are suppressed there.

### DB connection pool saturation (prod-like testing)

If backend logs show Hikari timeout, inspect active sessions in Postgres and check Lambda/Cognito trigger DB usage strategy.

## 8) Related Runbooks

- CI/CD: `docs/runbooks/ci-cd.md`
- AWS first-time setup: `docs/runbooks/getting-started-aws.md`
- Terraform operations: `docs/runbooks/terraform.md`
- API catalog: `docs/runbooks/openapi.md`
