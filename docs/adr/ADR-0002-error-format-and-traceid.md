# ADR-0002: Unified Error Format and TraceId Propagation

## Status

ACCEPTED

## Context

The backend needs to provide:

1. A consistent error response format across all endpoints
2. Request tracing capabilities for debugging and monitoring
3. Stack trace suppression for security (not leaking internal details to clients)

## Decision

We have decided to implement:

### 1. Unified Error Response Format

All API errors return JSON with exactly this structure:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable message",
  "traceId": "request-trace-id"
}
```

### 2. TraceId Propagation

- Accept `X-Trace-Id` request header (optional)
- Generate UUID if header is missing
- Store in MDC (SLF4J Mapped Diagnostic Context) with key `traceId`
- Set response header `X-Trace-Id` with the trace ID
- Include traceId in all error response bodies

### 3. Global Exception Handler

- Use Spring `@RestControllerAdvice` for centralized exception handling
- Map specific exceptions to appropriate HTTP status codes and error codes
- Never leak stack traces to clients

## Rationale

### Error Format

- **Consistency**: All clients know exactly what fields to expect
- **Machine-readable**: The `code` field enables programmatic error handling
- **Debugging**: The `message` field provides human context
- **Tracing**: The `traceId` field correlates logs with requests

### TraceId Propagation

- **Distributed Tracing**: Clients can provide their own trace IDs to correlate across systems
- **Request Correlation**: Logs can be filtered by trace ID to find all activity for a request
- **Security**: MDC enables automatic inclusion of trace ID in all logs without code changes
- **Flexibility**: Optional header accepts client-provided trace ID or auto-generates UUID

### Global Exception Handler

- **Separation of Concerns**: HTTP error handling is centralized, not scattered across controllers
- **Consistency**: All exceptions follow the same response format
- **Security**: Stack traces are never exposed to clients
- **Maintainability**: Adding new error types is straightforward

## Implementation

### Components

1. **TraceIdFilter** (`com.jiralite.backend.filter.TraceIdFilter`)

   - OncePerRequestFilter that intercepts all requests
   - Reads/generates traceId
   - Sets response header
   - Puts traceId in MDC

2. **ErrorResponse** (DTO record)

   - Immutable error response structure
   - Fields: code, message, traceId

3. **ErrorCode** (Enum)

   - VALIDATION_ERROR (400)
   - BAD_REQUEST (400)
   - NOT_FOUND (404)
   - FORBIDDEN (403)
   - INTERNAL_ERROR (500)

4. **GlobalExceptionHandler** (`com.jiralite.backend.handler.GlobalExceptionHandler`)

   - @RestControllerAdvice
   - Handles all exception types
   - Retrieves traceId from MDC for error responses

5. **Logging Configuration** (`logback-spring.xml`)
   - Includes `%X{traceId}` in log pattern
   - Automatically appends trace ID to all logs

## Consequences

### Positive

- Clear, predictable error responses for clients
- Enhanced debuggability through request tracing
- Improved security (no stack traces exposed)
- Easy to add observability tooling later (correlation with distributed tracing systems)
- All errors handled consistently

### Negative

- Slight overhead from MDC operations (negligible)
- Requires discipline to use GlobalExceptionHandler instead of custom error responses
- All DTOs must follow the ErrorResponse format

## Alternatives Considered

1. **Per-controller error handling** - Rejected: inconsistent responses, harder to maintain
2. **Leaking stack traces** - Rejected: security risk, verbose responses
3. **No tracing** - Rejected: debugging production issues becomes difficult
