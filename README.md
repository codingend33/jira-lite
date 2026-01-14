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

## Local Development (Day 1)

### Prerequisitess

- Docker Desktop
- Java 17

### 1 Start Postgres

Copy `.env.example` to `.env` and adjust values if needed.

```bash
cp .env.example .env
docker compose up -d
docker ps
```

### 2 Run Backend (local profile)

```bash
# Windows
.\mvnw.cmd --% spring-boot:run -Dspring-boot.run.profiles=local
```

### 3 Verify health

```bash
curl.exe -i http://localhost:8080/health
```

Expected: HTTP 200 and a JSON body containing `"status":"UP"`.

### 4 Run tests

```bash
./mvnw test
```

### Reset local DB (clean slate)

```bash
docker compose down -v
docker compose up -d
```

## Day 2 Backend Baseline Features

### Unified Error Response Format

All API errors return a consistent JSON structure:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable description",
  "traceId": "request-trace-id"
}
```

**Error Codes:**

- `VALIDATION_ERROR` (400) - Input validation failed
- `BAD_REQUEST` (400) - Malformed request
- `NOT_FOUND` (404) - Resource not found
- `FORBIDDEN` (403) - Access denied
- `INTERNAL_ERROR` (500) - Unexpected server error

### Request Tracing with TraceId

Each request is assigned a unique `traceId` for end-to-end tracking:

```bash
# Provide custom trace ID
curl -X GET http://localhost:8080/health \
  -H "X-Trace-Id: my-custom-id"

# Response includes trace ID header
# X-Trace-Id: my-custom-id
```

Auto-generated UUID if header is missing. TraceId is included in:

- Response header: `X-Trace-Id`
- Error response body: `traceId` field
- All log outputs: `traceId=<uuid>`

### OpenAPI / Swagger Documentation

Interactive API documentation available at:

```
http://localhost:8080/swagger-ui.html
```

Raw OpenAPI JSON:

```
http://localhost:8080/v3/api-docs
```

### Layering Architecture

Clean architectural layers enforced by ArchUnit tests:

```
Controller  (HTTP, DTOs)
    ↓
Service     (Business logic)
    ↓
Repository  (Data access)
    ↓
Database
```

**Rules enforced:**

- Controllers do NOT depend on repositories
- Repositories do NOT depend on controllers/services

### Minimal Demo Endpoints

**Health Check**

```bash
GET /health
```

**Echo Endpoint** (validation & error handling demo)

```bash
# Success
curl -X POST http://localhost:8080/demo/echo \
  -H "Content-Type: application/json" \
  -d '{"title": "Hello World"}'
# Returns: 200 OK with echoed title + timestamp

# Validation error (blank title)
curl -X POST http://localhost:8080/demo/echo \
  -H "Content-Type: application/json" \
  -d '{"title": ""}'
# Returns: 400 VALIDATION_ERROR

# Internal error (panic)
curl -X POST http://localhost:8080/demo/echo \
  -H "Content-Type: application/json" \
  -d '{"title": "panic"}'
# Returns: 500 INTERNAL_ERROR
```

### Documentation

- **Error Handling**: [docs/design/error-handling.md](docs/design/error-handling.md)
- **TraceId Propagation**: [docs/design/observability-traceid.md](docs/design/observability-traceid.md)
- **OpenAPI Setup**: [docs/design/openapi.md](docs/design/openapi.md)
- **Layering Architecture**: [docs/design/layering.md](docs/design/layering.md)
- **ADR-0002**: [docs/adr/ADR-0002-error-format-and-traceid.md](docs/adr/ADR-0002-error-format-and-traceid.md)

### Test Coverage

All tests pass with unified error handling and trace ID validation:

```bash
./mvnw clean test
```

**Test suites:**

- `BackendApplicationTests` - Context load test
- `IntegrationTest` - Error handling, trace ID, endpoints
- `LayeringTest` - ArchUnit architectural constraints
- `SmokeTest` - Basic smoke test
