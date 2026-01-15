# AI Prompt Log

## 2026-01-11 (Day 0)

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
  - backend/src/main/resources/db/migration/V1\_\_init.sql
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

## Day 2 Backend Engineering Baseline (2026-01-13)

### Tool

GitHub Copilot Chat (Agent mode)

### Prompt Summary

Implement Day 2 backend engineering baseline end-to-end with:

- Unified error response format for all endpoints
- TraceId propagation (request header, response header, error body, MDC, logging)
- OpenAPI/Swagger documentation
- Layering constraints enforced by ArchUnit tests
- Minimal demo endpoints (/health, /demo/echo)
- Comprehensive integration tests
- Documentation and ADRs

### Task Complexity

High - Multi-component implementation requiring:

- Filter implementation for cross-cutting traceId propagation
- Global exception handler for unified error responses
- OpenAPI configuration and dependency management
- ArchUnit layering constraint tests
- Integration tests with custom assertions
- 7 documentation files

### Components Adopted

#### Java Classes Created (11 files)

1. **TraceIdFilter** - OncePerRequestFilter for traceId propagation
2. **ErrorResponse** - Record DTO for unified error format
3. **ErrorCode** - Enum for error codes (VALIDATION_ERROR, BAD_REQUEST, NOT_FOUND, FORBIDDEN, INTERNAL_ERROR)
4. **ApiException** - Custom exception with error code and HTTP status
5. **GlobalExceptionHandler** - @RestControllerAdvice for centralized error handling
6. **EchoRequest** - Request DTO with @NotBlank validation
7. **EchoResponse** - Response DTO
8. **DemoService** - Service layer with business logic
9. **DemoController** - REST controller for /demo/echo endpoint
10. **OpenApiConfig** - Spring configuration for OpenAPI/Swagger
11. **LayeringTest** - ArchUnit tests for layering constraints

#### Configuration Files

1. **logback-spring.xml** - Logging config with traceId=%X{traceId}

#### Test Classes (2 files)

1. **IntegrationTest** - SpringBootTest integration tests
2. **LayeringTest** - ArchUnit JUnit5 tests

#### Documentation (5 files)

1. **docs/design/error-handling.md** - Error handling architecture and examples
2. **docs/design/observability-traceid.md** - TraceId propagation workflow
3. **docs/design/openapi.md** - Swagger UI and OpenAPI documentation
4. **docs/design/layering.md** - Layering architecture constraints
5. **docs/adr/ADR-0002-error-format-and-traceid.md** - Architecture Decision Record

#### Dependencies Added

- springdoc-openapi-starter-webmvc-ui (2.6.0) - Swagger UI + OpenAPI
- archunit-junit5 (1.3.0) - ArchUnit tests

### Files Modified

1. **pom.xml** - Added springdoc-openapi and archunit dependencies
2. **HealthController.java** - Added Swagger annotations

### Key Features Implemented

#### 1. Unified Error Response

All errors return:

```json
{
  "code": "ERROR_CODE",
  "message": "description",
  "traceId": "uuid"
}
```

#### 2. TraceId Propagation

- Reads X-Trace-Id header (optional)
- Generates UUID if missing
- Sets response header
- Stores in MDC
- Included in error responses and logs

#### 3. Error Handling

- MethodArgumentNotValidException → 400 VALIDATION_ERROR
- ConstraintViolationException → 400 VALIDATION_ERROR
- HttpMessageNotReadableException → 400 BAD_REQUEST
- EntityNotFoundException → 404 NOT_FOUND
- IllegalArgumentException → 400 BAD_REQUEST
- Generic Exception → 500 INTERNAL_ERROR

#### 4. OpenAPI/Swagger

- Enabled at /swagger-ui.html
- OpenAPI JSON at /v3/api-docs
- All endpoints documented with @Operation, @ApiResponse

#### 5. Layering Constraints

ArchUnit tests enforce:

- Controllers don't depend on repositories
- Repositories don't depend on controllers/services

#### 6. Demo Endpoints

- GET /health - Returns status and timestamp
- POST /demo/echo - Validates title, echoes back with timestamp
  - title blank → 400 VALIDATION_ERROR
  - title="panic" → 500 INTERNAL_ERROR
  - otherwise → 200 with EchoResponse

#### 7. Integration Tests

- Health endpoint returns X-Trace-Id header
- Validation errors return unified format with matching traceId
- Internal errors return unified format with matching traceId
- Successful echo returns response with matching traceId
- Malformed JSON returns BAD_REQUEST error

## Day 3: Database ERD Finalization + Flyway Core Tables (2026-01-15)

### Tool

GitHub Copilot Agent

### Prompt Summary

Design comprehensive Entity Relationship Diagram (ERD) for multi-tenant Jira Lite system with:
- Complete ERD covering 7 core entities (org, app_user, org_membership, project, ticket, ticket_comment, ticket_attachment)
- Multi-tenant isolation strategy (org_id on every tenant-owned table)
- Flyway migration extension with schema enhancements
- Detailed documentation with Chinese explanations and query pattern analysis
- Index optimization for common query scenarios
- Complete audit trail (timestamps on all tables)

### Task Complexity

Medium - Focused documentation and schema enhancement:
- Mermaid ERD diagram with 7+ entities and relationships
- Multi-tenant isolation rationale explanation
- Query pattern analysis with index justification
- Flyway V2 migration with selective table enhancements
- Index optimization for common queries

### Components Adopted

#### Documentation (1 new file)

1. **docs/design/erd.md** - Complete ERD with:
   - Mermaid diagram showing all 7 entities and relationships
   - Detailed table documentation (Chinese) with:
     - Table purpose
     - Key fields (PK/FK/UK/CHECK, org_id strategy)
     - Relationship descriptions
   - Multi-tenant isolation rationale:
     - Data security via org_id enforcement
     - Query performance benefits
     - Simplified foreign key design with composite keys (org_id, id)
     - Industry best practice alignment
   - 6 common query patterns with SQL examples:
     - List projects by organization
     - List tickets by project with filtering/sorting
     - Get ticket detail with comments and attachments
     - Get ticket attachments list
     - Get user's organizations (cross-tenant)
     - Get organization members (multi-org context)
   - Index design summary table:
     - org_id indexes (tenant isolation)
     - Composite indexes (org_id, project_id, status) for complex queries
     - Timestamp indexes for ordering/pagination
     - FK destination indexes for join optimization

#### Database Migration (1 new file)

1. **V2__core_domain_enhancements.sql** - Flyway migration adding:
   - org_membership.status (ACTIVE|INVITED|DISABLED) with CHECK
   - org_membership.updated_at for audit trail
   - app_user.cognito_sub for AWS Cognito integration
   - ticket_comments.updated_at for audit trail
   - ticket_attachments.upload_status (PENDING|UPLOADED|FAILED) with CHECK
   - ticket_attachments.updated_at for audit trail
   - Composite index (org_id, project_id, status) for ticket filtering
   - Index on ticket.updated_at DESC for recent ordering
   - Composite index (ticket_id, created_at) for comment pagination
   - Composite index (ticket_id, created_at) for attachment pagination
   - Index on project.created_by for user's projects listing

### Files Created/Modified

1. **docs/design/erd.md** (new) - ERD documentation
2. **backend/src/main/resources/db/migration/V2__core_domain_enhancements.sql** (new)
3. **docs/ai/prompt-log.md** (updated) - This log entry

