# Error Handling

## Overview

All API errors return a unified JSON response format to ensure consistency and facilitate client-side error handling.

## Error Response Format

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable error message",
  "traceId": "uuid-for-request-tracking"
}
```

### Fields

- **code**: Machine-readable error code from the `ErrorCode` enum
- **message**: Human-readable message describing the error
- **traceId**: Unique identifier for request tracing and debugging

## Error Codes

| Code               | HTTP Status | Description                                                          |
| ------------------ | ----------- | -------------------------------------------------------------------- |
| `UNAUTHORIZED`     | 401         | Authentication required (missing or invalid JWT)                     |
| `VALIDATION_ERROR` | 400         | Input validation failed (invalid JSON schema, constraint violations) |
| `BAD_REQUEST`      | 400         | Malformed request (invalid JSON, missing required fields)            |
| `NOT_FOUND`        | 404         | Requested resource not found                                         |
| `FORBIDDEN`        | 403         | Access denied                                                        |
| `INTERNAL_ERROR`   | 500         | Unexpected server error                                              |

## Error Handling Implementation

### Global Exception Handler

All exceptions are centrally handled by `GlobalExceptionHandler` (@RestControllerAdvice):

- `MethodArgumentNotValidException` → 400 VALIDATION_ERROR
- `ConstraintViolationException` → 400 VALIDATION_ERROR
- `HttpMessageNotReadableException` → 400 BAD_REQUEST
- `EntityNotFoundException` → 404 NOT_FOUND
- `IllegalArgumentException` → 400 BAD_REQUEST
- `ApiException` → Configured status code (supports custom error codes)
- Generic `Exception` → 500 INTERNAL_ERROR

### Security Error Handling

Security failures are handled by dedicated handlers to ensure the same JSON format:

- AuthenticationEntryPoint → 401 UNAUTHORIZED
- AccessDeniedHandler → 403 FORBIDDEN

### Stack Trace Suppression

Stack traces are NEVER leaked to clients. Only message and code are returned.

## Examples

### Validation Error

```bash
curl.exe -i -X POST http://localhost:8080/demo/echo
-H "Content-Type: application/json"
-d '{\"title\":\"\"}'
```

Response:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed: title: title must not be blank",
  "traceId": "a1b2c3d4-e5f6-4g7h-8i9j-0k1l2m3n4o5p"
}
```

### Internal Error

```bash
curl.exe -i -X POST http://localhost:8080/demo/echo
-H "Content-Type: application/json"
-d '{\"title\":\"panic\"}'
```

Response:

```json
{
  "code": "INTERNAL_ERROR",
  "message": "Unexpected error",
  "traceId": "a1b2c3d4-e5f6-4g7h-8i9j-0k1l2m3n4o5p"
}
```
