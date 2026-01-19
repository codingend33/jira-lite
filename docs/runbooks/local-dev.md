# Local Dev Runbook

## Day 1: Local Setup

### Start DB

```bash
docker compose up -d
docker ps
docker logs -f jira_lite_postgres
```

### Stop DB

```bash
docker compose down
```

### Reset DB (drop volumes)

```bash
docker compose down -v
docker compose up -d
```

### Run backend with local profile

```bash
# Optional: set Cognito issuer for Day 4 auth tests
$env:COGNITO_ISSUER_URI="https://cognito-idp.<region>.amazonaws.com/<userPoolId>"

.\mvnw.cmd --% spring-boot:run -Dspring-boot.run.profiles=local
```

### Verify health

```bash
curl.exe -i http://localhost:8080/health
```

## Day 2: Error Handling + TraceId

### Verify Day 2 Error Handling + TraceId

```bash
# Validation error (400 VALIDATION_ERROR + traceId)
curl.exe -i -X POST http://localhost:8080/demo/echo ^
  -H "Content-Type: application/json" ^
  -d "{\"title\": \"\"}"

# Internal error (500 INTERNAL_ERROR + traceId)
curl.exe -i -X POST http://localhost:8080/demo/echo ^
  -H "Content-Type: application/json" ^
  -d "{\"title\": \"panic\"}"
```

## Day 3: Flyway + Schema

### Verify Day 3 Flyway Migrations

```bash
# Check Flyway history shows V1, V2, V3 applied
docker exec -it jira-lite-postgres psql -U jira -d jira_lite -c "select version, success from flyway_schema_history order by installed_rank;"

# Confirm core tables exist
docker exec -it jira-lite-postgres psql -U jira -d jira_lite -c "\dt"

# Spot-check new columns added in V2/V3
docker exec -it jira-lite-postgres psql -U jira -d jira_lite -c "\d org_membership"
docker exec -it jira-lite-postgres psql -U jira -d jira_lite -c "\d app_user"
docker exec -it jira-lite-postgres psql -U jira -d jira_lite -c "\d ticket"
```

## Day 4: Cognito + RBAC + TenantContext

### Verify Day 4 Auth + RBAC

```bash
# Protected endpoint (no token -> 401)
curl.exe -i http://localhost:8080/debug/whoami

# Protected endpoint (token required)
curl.exe -i -H "Authorization: Bearer <JWT>" http://localhost:8080/debug/whoami

# Admin-only (requires ADMIN role)
curl.exe -i -H "Authorization: Bearer <JWT>" http://localhost:8080/debug/admin-only
```

## Day 5: Org Members Admin + Tenant Isolation

### Verify Day 5 Org Member Management

```bash
# List members (ADMIN only)
curl.exe -i -H "Authorization: Bearer <JWT>" http://localhost:8080/org/members

# Add member (ADMIN only)
curl.exe -i -X POST http://localhost:8080/org/members ^
  -H "Authorization: Bearer <JWT>" ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":\"<USER_UUID>\"}"

# Update member (ADMIN only)
curl.exe -i -X PATCH http://localhost:8080/org/members/<USER_UUID> ^
  -H "Authorization: Bearer <JWT>" ^
  -H "Content-Type: application/json" ^
  -d "{\"role\":\"MEMBER\",\"status\":\"ACTIVE\"}"

# Delete member (ADMIN only)
curl.exe -i -X DELETE http://localhost:8080/org/members/<USER_UUID> ^
  -H "Authorization: Bearer <JWT>"
```

### Verify Day 5 Testcontainers (optional)

```bash
cd backend
./mvnw test -Dtest=OrgMembersTcIntegrationTest
```

## Troubleshooting

### Port 5432 is already in use

- Change `POSTGRES_PORT` in `.env`
- Or stop existing local Postgres service

### Backend cannot connect to DB

- Check container: `docker ps`
- Check logs: `docker logs jira_lite_postgres`
- Confirm `.env` values match `application-local.yml`

### Flyway migration fails

- Read SQL error in backend logs
- Reset DB with `docker compose down -v` then retry

## PowerShell `curl` behaves differently than expected

**Symptom**

- Running `curl -i http://localhost:8080/health` prompted: `Supply values for the following parameters: Uri:`

**Cause**

- In PowerShell, `curl` is an alias for `Invoke-WebRequest`, not the real curl binary.

**Fix**

- Use the real curl executable:
  - `curl.exe -i http://localhost:8080/health`

**Result**

- The health endpoint returned **HTTP 200** with JSON like:
  - `{"status":"UP"}`

## Flyway migration fails during tests because H2 doesn’t support Postgres SQL

**Symptom**

- `./mvnw test` failed with messages like:
  - `Script V1__init.sql failed`
  - Syntax error at: `CREATE EXTENSION IF NOT EXISTS pgcrypto`
  - `org.h2.jdbc.JdbcSQLSyntaxErrorException`

**Cause**

- Tests default to in-memory **H2**.
- Flyway runs migrations automatically during Spring Boot test startup.
- The migration contains **Postgres-only** SQL: `CREATE EXTENSION IF NOT EXISTS pgcrypto`.

**Fix (Day 1 recommended)**

- Disable Flyway only in tests by adding/overriding:
  - `backend/src/test/resources/application.yml`

```yml
spring:
  flyway:
    enabled: false
  jpa:
    hibernate:
      ddl-auto: none
```

**Result**

-Tests passed: BUILD SUCCESS

```

```
