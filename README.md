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
