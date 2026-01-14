# Layering Architecture

## Overview

The backend follows a strict layering architecture to ensure separation of concerns and maintainability:

```
┌─────────────────────────────────┐
│      HTTP Layer (Controller)     │  Handles HTTP requests/responses
├─────────────────────────────────┤
│      Service Layer               │  Business logic
├─────────────────────────────────┤
│      Repository Layer            │  Data access
├─────────────────────────────────┤
│      Database                    │  Persistence
└─────────────────────────────────┘
```

## Layer Responsibilities

### Controller Layer

- **Package**: `com.jiralite.backend.controller`
- **Responsibility**: HTTP request/response handling
- **Dependencies**: Service layer only
- **Restrictions**:
  - MUST NOT depend on repository layer
  - MUST NOT contain business logic
  - MUST use DTOs for request/response

**Example**:

```java
@RestController
public class DemoController {
    private final DemoService service;

    @PostMapping("/demo/echo")
    public ResponseEntity<EchoResponse> echo(@Valid @RequestBody EchoRequest request) {
        // Delegate to service
        return ResponseEntity.ok(service.echo(request));
    }
}
```

### Service Layer

- **Package**: `com.jiralite.backend.service`
- **Responsibility**: Business logic and orchestration
- **Dependencies**: Repository layer, utilities
- **Restrictions**: None

**Example**:

```java
@Service
public class DemoService {
    public EchoResponse echo(EchoRequest request) {
        // Business logic here
        return new EchoResponse(...);
    }
}
```

### Repository Layer

- **Package**: `com.jiralite.backend.repository`
- **Responsibility**: Data access and persistence
- **Dependencies**: Entities only (no service or controller dependencies)
- **Restrictions**:
  - MUST NOT depend on controller layer
  - MUST NOT depend on service layer
  - MUST NOT contain business logic

**Example**:

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

### Exception Handling Layer

- **Package**: `com.jiralite.backend.handler`
- **Responsibility**: Centralized exception handling
- **Dependencies**: DTOs, exception classes
- **Use Case**: `@RestControllerAdvice` for global error handling

**Example**:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(...) {
        // Unified error response
    }
}
```

### Filter Layer

- **Package**: `com.jiralite.backend.filter`
- **Responsibility**: Request/response intercepting
- **Dependencies**: None (cross-cutting concerns)
- **Use Case**: TraceId propagation, logging

**Example**:

```java
@Component
public class TraceIdFilter extends OncePerRequestFilter {
    // TraceId propagation
}
```

### Configuration Layer

- **Package**: `com.jiralite.backend.config`
- **Responsibility**: Spring beans and cross-cutting configuration
- **Dependencies**: Utility libraries
- **Use Case**: OpenAPI, custom bean definitions

## Enforced Constraints (ArchUnit Tests)

The following rules are automatically enforced via ArchUnit:

1. **Controllers do NOT depend on repositories**

   - Controllers may use services
   - Controllers receive requests and delegate to services

2. **Repositories do NOT depend on controllers**

   - Repositories are unaware of HTTP layer

3. **Repositories do NOT depend on services**
   - Repositories are independent data access layer
   - Services orchestrate repositories

## Violation Detection

Run ArchUnit tests to verify layering constraints:

```bash
./mvnw test -Dtest=LayeringTest
```

Any violations will fail the test with detailed error messages.

## Communication Example

Request → Controller → Service → Repository → Database

```
POST /demo/echo
  ↓
DemoController.echo(request)  [HTTP layer]
  ↓
DemoService.echo(request)     [Business logic]
  ↓
EchoRepository (future)       [Data access]
  ↓
Database
```

Response flows back with layers properly separated.

## Benefits

- **Testability**: Each layer can be tested independently
- **Maintainability**: Clear responsibility boundaries
- **Scalability**: Easy to add new layers or modify existing ones
- **Reusability**: Services can be reused by multiple controllers
- **Dependency Inversion**: Controllers depend on abstractions (services), not implementations
