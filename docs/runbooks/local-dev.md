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
