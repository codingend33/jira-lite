# OpenAPI / Swagger Documentation

## Overview

The API is documented using OpenAPI 3.0 specification via SpringDoc OpenAPI. Swagger UI provides interactive API documentation.

## Accessing Swagger UI

### Local Development

Start the application and navigate to:

```
http://localhost:8080/swagger-ui.html
```

or

```
http://localhost:8080/swagger-ui/index.html
```

## Accessing OpenAPI JSON

The raw OpenAPI specification is available at:

```
http://localhost:8080/v3/api-docs
```

JSON format is ideal for tools and API gateways.

## Endpoints

All endpoints are automatically documented in Swagger UI:

### Health Check
- **GET** `/health`
- Returns application status and timestamp
- Includes X-Trace-Id header

### Demo Echo
- **POST** `/demo/echo`
- Echoes back the provided title with timestamp
- Validates input (title must not be blank)
- Returns unified error response on validation failure

## Swagger Configuration

### OpenApiConfig

Located in `com.jiralite.backend.config.OpenApiConfig`:

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Jira Lite API")
                .version("1.0.0")
                .description("Day 2 backend baseline API..."));
    }
}
```

### Endpoint Annotations

All endpoints are annotated with `@Operation` and `@Tag` for Swagger documentation:

```java
@PostMapping("/demo/echo")
@Operation(summary = "Echo title and return with timestamp")
@ApiResponse(responseCode = "200", description = "Echo successful")
@ApiResponse(responseCode = "400", description = "Validation error")
@ApiResponse(responseCode = "500", description = "Internal error")
public ResponseEntity<EchoResponse> echo(@Valid @RequestBody EchoRequest request) {
    // ...
}
```

## Dependencies

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

This provides:
- Swagger UI at `/swagger-ui.html`
- OpenAPI JSON at `/v3/api-docs`
- Automatic API discovery and documentation

## Disabled Endpoints

To exclude Spring Boot Actuator endpoints from Swagger, add to `application.yml`:

```yaml
springdoc:
  swagger-ui:
    urls-primary-name: "API"
```

This keeps Swagger UI focused on application endpoints.

## Testing with cURL

All endpoints can be tested via cURL and will be documented in Swagger UI:

```bash
# Health check
curl -X GET http://localhost:8080/health

# Echo endpoint
curl -X POST http://localhost:8080/demo/echo \
  -H "Content-Type: application/json" \
  -d '{"title": "Hello API"}'
```

