# Local Dev Runbook

## Commands

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
.\mvnw.cmd --% spring-boot:run -Dspring-boot.run.profiles=local
```

### Verify health

```bash
curl.exe -i http://localhost:8080/health
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
