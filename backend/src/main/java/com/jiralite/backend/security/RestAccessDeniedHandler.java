package com.jiralite.backend.security;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiralite.backend.dto.ErrorCode;
import com.jiralite.backend.dto.ErrorResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Sends unified JSON response for 403 Forbidden.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final String MDC_TRACE_KEY = "traceId";

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        ErrorResponse error = new ErrorResponse(
                ErrorCode.FORBIDDEN.name(),
                "Access denied",
                MDC.get(MDC_TRACE_KEY));
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
