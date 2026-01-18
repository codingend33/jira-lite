# Observability: TraceId Propagation

## Overview

Trace IDs enable end-to-end request tracking for debugging and monitoring. Each request is assigned a unique trace ID that flows through all layers of the application.

## TraceId Workflow

1. **Request Arrival**: `TraceIdFilter` intercepts incoming requests
2. **Header Check**: Looks for `X-Trace-Id` header
3. **Generation**: If missing, generates a new UUID
4. **MDC Storage**: Stores trace ID in SLF4J MDC (Mapped Diagnostic Context) with key `traceId`
5. **Response Header**: Sets `X-Trace-Id` response header
6. **Error Response**: Includes traceId in error response body from MDC
7. **Cleanup**: Removes traceId from MDC after response completes

## Request Header: X-Trace-Id

### Providing a Trace ID

Clients can optionally provide a trace ID header to correlate requests:

```bash
curl -X GET http://localhost:8080/health \
  -H "X-Trace-Id: my-custom-trace-id-123"
```

Response headers will include:
```
X-Trace-Id: my-custom-trace-id-123
```

### Auto-Generated Trace ID

If no `X-Trace-Id` header is provided, the system generates a UUID:

```bash
curl -X GET http://localhost:8080/health
```

Response headers will include:
```
X-Trace-Id: a1b2c3d4-e5f6-4g7h-8i9j-0k1l2m3n4o5p
```

## Logging Integration

The `logback-spring.xml` configuration includes `traceId` in all log outputs:

```
2026-01-13 14:30:45.123 [main] INFO  com.jiralite.backend.controller.DemoController - traceId=a1b2c3d4-e5f6-4g7h-8i9j-0k1l2m3n4o5p - Echo request received
```

This enables correlation between logs and requests for debugging. When a user is authenticated, `org_id` and `user_id` are also added to MDC and logged.

## Error Response Consistency

In case of errors, the traceId in the response body **always matches** the `X-Trace-Id` response header:

```bash
curl -X POST http://localhost:8080/demo/echo \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: custom-trace-123" \
  -d '{"title": ""}'
```

Response:
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed: ...",
  "traceId": "custom-trace-123"
}
```

Response headers:
```
X-Trace-Id: custom-trace-123
```

## Implementation Details

### TraceIdFilter

- **Location**: `com.jiralite.backend.filter.TraceIdFilter`
- **Type**: `OncePerRequestFilter` (ensures single execution per request)
- **MDC Key**: `traceId`
- **Header Name**: `X-Trace-Id`

### Global Exception Handler

The `GlobalExceptionHandler` retrieves traceId from MDC:

```java
MDC.get("traceId")  // Retrieved in each exception handler
```

## Debugging with TraceId

To find all logs related to a specific request:

```bash
grep "traceId=a1b2c3d4-e5f6-4g7h-8i9j-0k1l2m3n4o5p" application.log
```

## Configuration

No additional configuration required. TraceId filtering is enabled by default via:
- `@Component` annotation on `TraceIdFilter`
- Spring automatically registers all `Filter` beans

