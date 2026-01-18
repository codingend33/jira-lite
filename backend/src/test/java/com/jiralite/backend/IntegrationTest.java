package com.jiralite.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiralite.backend.security.TestJwtDecoderConfig;

/**
 * Integration tests for unified error handling and trace ID propagation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class IntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpEntity<String> createJsonRequest(String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("admin-token");
        return new HttpEntity<>(jsonBody, headers);
    }

    /**
     * Test that /health endpoint returns X-Trace-Id header.
     */
    @Test
    void testHealthEndpointReturnsTraceIdHeader() {
        ResponseEntity<String> response = restTemplate.getForEntity("/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get("X-Trace-Id")).isNotNull();
        assertThat(response.getHeaders().get("X-Trace-Id")).isNotEmpty();
    }

    /**
     * Test that validation error returns 400 with VALIDATION_ERROR code and
     * matching traceId.
     */
    @Test
    void testValidationErrorReturnsUnifiedErrorResponse() throws Exception {
        String requestBody = "{\"title\": \"\"}";

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/demo/echo",
                createJsonRequest(requestBody),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.has("message")).isTrue();
        assertThat(body.has("traceId")).isTrue();

        String responseTraceId = body.get("traceId").asText();
        String headerTraceId = response.getHeaders().get("X-Trace-Id").get(0);
        assertThat(responseTraceId).isEqualTo(headerTraceId);
    }

    /**
     * Test that internal error (panic) returns 500 with INTERNAL_ERROR code and
     * matching traceId.
     */
    @Test
    void testInternalErrorReturnsUnifiedErrorResponse() throws Exception {
        String requestBody = "{\"title\": \"panic\"}";

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/demo/echo",
                createJsonRequest(requestBody),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("code").asText()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.has("message")).isTrue();
        assertThat(body.has("traceId")).isTrue();

        String responseTraceId = body.get("traceId").asText();
        String headerTraceId = response.getHeaders().get("X-Trace-Id").get(0);
        assertThat(responseTraceId).isEqualTo(headerTraceId);
    }

    /**
     * Test that successful echo returns 200 with response and matching traceId.
     */
    @Test
    void testEchoSuccessReturnsResponseAndTraceId() throws Exception {
        String requestBody = "{\"title\": \"Hello World\"}";

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/demo/echo",
                createJsonRequest(requestBody),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get("X-Trace-Id")).isNotNull();

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("title").asText()).isEqualTo("Hello World");
        assertThat(body.has("timestamp")).isTrue();
    }

    /**
     * Test that malformed JSON returns 400 with BAD_REQUEST code.
     */
    @Test
    void testMalformedJsonReturnsErrorResponse() throws Exception {
        String requestBody = "{ invalid json }";

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/demo/echo",
                createJsonRequest(requestBody),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("code").asText()).isEqualTo("BAD_REQUEST");
        assertThat(body.has("traceId")).isTrue();
    }
}
