# AI Prompt Log

## 2026-01-11
Tool: ChatGPT Project
Purpose: Bootstrap repo structure and workflow (docs + templates)
Outputs:
- Repo structure checklist
- README/ADR/AI usage skeleton
  Files:
- README.md
- docs/adr/ADR-0001-tech-stack.md
- docs/ai/AI_USAGE.md
- docs/ai/prompt-log.md
- .github/pull_request_template.md
  Validation:
- Files created under repo root and committed to Git

## 2026-01-12 (Day 1)

- Tool: Cursor / Claude Code
- Prompt Summary: Generate Day1 local dev loop files (docker-compose, local config, Flyway V1, /health, docs).
- Adopted Output: Created/updated all listed files; adjusted Java package to match project base package.
- Files Changed:
  - docker-compose.yml
  - .env.example
  - .gitignore
  - backend/src/main/resources/application.yml
  - backend/src/main/resources/application-local.yml
  - backend/src/main/resources/db/migration/V1__init.sql
  - backend/src/main/java/.../controller/HealthController.java
  - backend/src/test/java/.../SmokeTest.java
  - docs/runbooks/local-dev.md
  - docs/design/db-schema.md
  - README.md
- Verification:
  - cp .env.example .env
  - docker compose up -d
  - ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
  - curl -i http://localhost:8080/health
  - ./mvnw test