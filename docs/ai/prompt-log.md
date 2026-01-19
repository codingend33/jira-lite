# AI Prompt Log

## Day 5: Org Members Admin + Tenant Isolation (2026-01-18)

- Tool: Codex
- Prompt Summary: Implement org member admin APIs, tenant-scoped service/repositories/entities, multi-tenant isolation tests, and update docs/runbook/ADR.
- Adopted Output: Added entities, repositories, service/controller/DTOs, updated test JwtDecoder and H2 settings, added MockMvc tests, and updated documentation.
- Files Changed:
  - backend/src/main/java/com/jiralite/backend/controller/OrgMembersController.java
  - backend/src/main/java/com/jiralite/backend/dto/CreateMemberRequest.java
  - backend/src/main/java/com/jiralite/backend/dto/MemberResponse.java
  - backend/src/main/java/com/jiralite/backend/dto/UpdateMemberRequest.java
  - backend/src/main/java/com/jiralite/backend/entity/OrgEntity.java
  - backend/src/main/java/com/jiralite/backend/entity/OrgMembershipEntity.java
  - backend/src/main/java/com/jiralite/backend/entity/OrgMembershipId.java
  - backend/src/main/java/com/jiralite/backend/entity/UserEntity.java
  - backend/src/main/java/com/jiralite/backend/repository/OrgMembershipRepository.java
  - backend/src/main/java/com/jiralite/backend/repository/OrgRepository.java
  - backend/src/main/java/com/jiralite/backend/repository/UserRepository.java
  - backend/src/main/java/com/jiralite/backend/service/OrgMemberService.java
  - backend/src/test/java/com/jiralite/backend/OrgMembersIntegrationTest.java
  - backend/src/test/java/com/jiralite/backend/SecurityIntegrationTest.java
  - backend/src/test/java/com/jiralite/backend/security/TestJwtDecoderConfig.java
  - backend/src/test/resources/application.yml
  - docs/adr/ADR-0004-org-members-admin-and-tenant-isolation.md
  - docs/design/multi-tenancy.md
  - docs/design/openapi.md
  - docs/runbooks/local-dev.md
- Verification:
  - ./mvnw test

## Day 5 Upgrade: Flyway V5 + Testcontainers (2026-01-18)

- Tool: Codex
- Prompt Summary: Add Flyway V5 constraints, introduce Testcontainers Postgres integration test, and document testing rationale.
- Adopted Output: Added V5 migration, Testcontainers dependencies and integration test, plus testing and ADR docs.
- Files Changed:
  - backend/pom.xml
  - backend/src/main/resources/db/migration/V5__org_members_admin.sql
  - backend/src/test/java/com/jiralite/backend/OrgMembersTcIntegrationTest.java
  - docs/runbooks/local-dev.md
  - docs/design/api-org-members.md
  - docs/design/security-rbac.md
  - TESTING.md
  - docs/adr/ADR-0005-testcontainers-for-postgres-integration-tests.md
- Verification:
  - ./mvnw test (H2)
  - ./mvnw test -Dtest=OrgMembersTcIntegrationTest (Docker required)
