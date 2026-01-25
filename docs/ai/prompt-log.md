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

## Day 7: Tickets CRUD + Status Flow (2026-01-19)

- Tool: Codex
- Prompt Summary: Implement tickets list/detail/create/update/transition with pagination, tenant isolation, and docs/tests updates.
- Adopted Output: Added ticket entity/repository/service/controller/DTOs, migration indexes, MockMvc tests, and docs/ADR updates.
- Files Changed:
  - backend/src/main/java/com/jiralite/backend/controller/TicketsController.java
  - backend/src/main/java/com/jiralite/backend/dto/CreateTicketRequest.java
  - backend/src/main/java/com/jiralite/backend/dto/UpdateTicketRequest.java
  - backend/src/main/java/com/jiralite/backend/dto/TransitionTicketRequest.java
  - backend/src/main/java/com/jiralite/backend/dto/TicketResponse.java
  - backend/src/main/java/com/jiralite/backend/dto/PageMeta.java
  - backend/src/main/java/com/jiralite/backend/dto/PagedResponse.java
  - backend/src/main/java/com/jiralite/backend/entity/TicketEntity.java
  - backend/src/main/java/com/jiralite/backend/repository/TicketRepository.java
  - backend/src/main/java/com/jiralite/backend/service/TicketService.java
  - backend/src/main/resources/db/migration/V7__tickets_indexes.sql
  - backend/src/test/java/com/jiralite/backend/TicketsIntegrationTest.java
  - backend/src/test/java/com/jiralite/backend/TicketsTcIntegrationTest.java
  - docs/adr/ADR-0007-tickets-crud-and-status-flow.md
  - docs/design/multi-tenancy.md
  - docs/design/openapi.md
  - docs/design/tickets.md
  - docs/runbooks/local-dev.md
- Verification:
  - ./mvnw test

## Day 8: Comments + Attachments Presigned (2026-01-20)

- Tool: Codex
- Prompt Summary: Implement ticket comments and attachments with S3 presigned upload/download, tenant isolation, and docs/tests updates.
- Adopted Output: Added comment/attachment entities, repositories, services, controllers, presign service, migrations, tests, and docs/ADR updates.
- Files Changed:
  - backend/pom.xml
  - backend/src/main/java/com/jiralite/backend/config/S3Config.java
  - backend/src/main/java/com/jiralite/backend/controller/TicketCommentsController.java
  - backend/src/main/java/com/jiralite/backend/controller/TicketAttachmentsController.java
  - backend/src/main/java/com/jiralite/backend/dto/AttachmentResponse.java
  - backend/src/main/java/com/jiralite/backend/dto/CommentResponse.java
  - backend/src/main/java/com/jiralite/backend/dto/CreateCommentRequest.java
  - backend/src/main/java/com/jiralite/backend/dto/PresignDownloadResponse.java
  - backend/src/main/java/com/jiralite/backend/dto/PresignUploadRequest.java
  - backend/src/main/java/com/jiralite/backend/dto/PresignUploadResponse.java
  - backend/src/main/java/com/jiralite/backend/entity/TicketAttachmentEntity.java
  - backend/src/main/java/com/jiralite/backend/entity/TicketCommentEntity.java
  - backend/src/main/java/com/jiralite/backend/repository/TicketAttachmentRepository.java
  - backend/src/main/java/com/jiralite/backend/repository/TicketCommentRepository.java
  - backend/src/main/java/com/jiralite/backend/service/S3PresignService.java
  - backend/src/main/java/com/jiralite/backend/service/TicketAttachmentService.java
  - backend/src/main/java/com/jiralite/backend/service/TicketCommentService.java
  - backend/src/main/resources/application-local.yml
  - backend/src/main/resources/db/migration/V8__comments_attachments.sql
  - backend/src/test/java/com/jiralite/backend/TicketAttachmentsIntegrationTest.java
  - backend/src/test/java/com/jiralite/backend/TicketCommentsTcIntegrationTest.java
  - backend/src/test/java/com/jiralite/backend/TicketAttachmentsTcIntegrationTest.java
  - backend/src/test/java/com/jiralite/backend/TicketCommentsIntegrationTest.java
  - docs/adr/ADR-0008-comments-attachments-presigned.md
  - docs/design/attachments.md
  - docs/design/comments.md
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

## Day 6: Projects CRUD + Archive + OpenAPI (2026-01-19)

- Tool: Codex
- Prompt Summary: Implement project CRUD with archive/unarchive, tenant-scoped service/repository, OpenAPI docs, and tests.
- Adopted Output: Added project entity/repository/service/controller/DTOs, Flyway migration, MockMvc tests, and docs/ADR updates.
- Files Changed:
  - backend/src/main/java/com/jiralite/backend/controller/ProjectsController.java
  - backend/src/main/java/com/jiralite/backend/dto/CreateProjectRequest.java
  - backend/src/main/java/com/jiralite/backend/dto/ProjectResponse.java
  - backend/src/main/java/com/jiralite/backend/dto/UpdateProjectRequest.java
  - backend/src/main/java/com/jiralite/backend/entity/ProjectEntity.java
  - backend/src/main/java/com/jiralite/backend/repository/ProjectRepository.java
  - backend/src/main/java/com/jiralite/backend/service/ProjectService.java
  - backend/src/main/resources/db/migration/V6__projects.sql
  - backend/src/test/java/com/jiralite/backend/OrgMembersTcIntegrationTest.java
  - backend/src/test/java/com/jiralite/backend/ProjectsIntegrationTest.java
  - docs/adr/ADR-0006-projects-crud-and-archive.md
  - docs/design/multi-tenancy.md
  - docs/design/openapi.md
  - docs/design/projects.md
  - docs/runbooks/local-dev.md
- Verification:
  - ./mvnw test

## Day 9: Frontend Pages + Cognito Login + React Query (2026-01-20)

- Tool: Codex
- Prompt Summary: Scaffold React frontend, integrate Cognito Hosted UI login (PKCE), build React Query data layer, and add MVP pages for projects/tickets/comments/attachments.
- Adopted Output: Added Vite React app with auth, API client, React Query hooks, pages, and docs/runbook updates.
- Files Changed:
  - frontend/package.json
  - frontend/vite.config.ts
  - frontend/tsconfig.json
  - frontend/tsconfig.node.json
  - frontend/index.html
  - frontend/.env.example
  - frontend/src/main.tsx
  - frontend/src/App.tsx
  - frontend/src/styles.css
  - frontend/src/env.d.ts
  - frontend/src/auth/AuthContext.tsx
  - frontend/src/auth/auth.ts
  - frontend/src/auth/pkce.ts
  - frontend/src/auth/storage.ts
  - frontend/src/api/client.ts
  - frontend/src/api/types.ts
  - frontend/src/api/projects.ts
  - frontend/src/api/tickets.ts
  - frontend/src/api/comments.ts
  - frontend/src/api/attachments.ts
  - frontend/src/query/projectQueries.ts
  - frontend/src/query/ticketQueries.ts
  - frontend/src/query/commentQueries.ts
  - frontend/src/query/attachmentQueries.ts
  - frontend/src/components/Layout.tsx
  - frontend/src/components/ProtectedRoute.tsx
  - frontend/src/components/ErrorBanner.tsx
  - frontend/src/components/Loading.tsx
  - frontend/src/components/PaginationControls.tsx
  - frontend/src/pages/LoginPage.tsx
  - frontend/src/pages/ProjectsPage.tsx
  - frontend/src/pages/TicketsPage.tsx
  - frontend/src/pages/TicketFormPage.tsx
  - frontend/src/pages/TicketDetailPage.tsx
  - docs/design/frontend-auth.md
  - docs/design/frontend-data.md
  - docs/design/ui-pages.md
  - docs/adr/ADR-0009-frontend-cognito-login.md
  - docs/runbooks/local-dev.md
  - README.md
- Verification:
  - npm run build (pending: requires npm install)
