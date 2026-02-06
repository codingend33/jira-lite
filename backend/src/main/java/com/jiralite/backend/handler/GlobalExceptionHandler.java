package com.jiralite.backend.handler;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.jiralite.backend.dto.ErrorCode;
import com.jiralite.backend.dto.ErrorResponse;
import com.jiralite.backend.exception.ApiException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

/**
 * Global exception handler for all endpoints.
 * Ensures all errors return unified ErrorResponse format.
 */
// RestControllerAdvice is a Spring annotation that allows you to handle
// exceptions across all controllers.
// @RestControllerAdvice = @ControllerAdvice + @ResponseBody。JSON response for
// all exceptions.
@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
        // get the trace ID from the MDC and add it to the error response.
        private static final String MDC_KEY = "traceId";

        /**
         * Handles validation errors from @Valid annotation.
         * every controller method that has @Valid annotation will be handled by this
         * method.
         * json body bind DTO, but DTO validation failed.
         * 
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {

                // get the validation errors from the binding result
                // and convert them to a comma separated string.
                String message = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(err -> err.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                // return a bad request response with the validation errors.
                return ResponseEntity.badRequest()
                                .body(new ErrorResponse(
                                                ErrorCode.VALIDATION_ERROR.name(),
                                                message,
                                                MDC.get(MDC_KEY)));
        }

        /**
         * Handles constraint violation errors.
         * URL/path/query/header parameter validation error.
         */
        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
                String violations = ex.getConstraintViolations()
                                .stream()
                                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                                .collect(Collectors.joining(", "));

                String message = "Constraint violation: " + violations;
                return ResponseEntity.badRequest()
                                .body(new ErrorResponse(
                                                ErrorCode.VALIDATION_ERROR.name(),
                                                message,
                                                MDC.get(MDC_KEY)));
        }

        /**
         * Handles malformed JSON requests.
         * josn body is invalid.
         */
        @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable() {
                return ResponseEntity.badRequest()
                                .body(new ErrorResponse(
                                                ErrorCode.BAD_REQUEST.name(),
                                                "Malformed JSON request",
                                                MDC.get(MDC_KEY)));
        }

        /**
         * Handles entity not found errors.
         * database entity not found.
         */
        @ExceptionHandler(EntityNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(new ErrorResponse(
                                                ErrorCode.NOT_FOUND.name(),
                                                ex.getMessage(),
                                                MDC.get(MDC_KEY)));
        }

        /**
         * Handles illegal argument errors.
         * throw IllegalArgumentException in controller method.
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
                return ResponseEntity.badRequest()
                                .body(new ErrorResponse(
                                                ErrorCode.BAD_REQUEST.name(),
                                                ex.getMessage(),
                                                MDC.get(MDC_KEY)));
        }

        /**
         * Handles database integrity violations (e.g. value too long, unique constraint).
         */
        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
                String rawMessage = ex.getMostSpecificCause() != null
                                ? ex.getMostSpecificCause().getMessage()
                                : ex.getMessage();
                String message = "Database constraint violation";
                if (rawMessage != null && rawMessage.toLowerCase().contains("value too long")) {
                        message = "One or more fields are too long";
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(new ErrorResponse(
                                                ErrorCode.BAD_REQUEST.name(),
                                                message,
                                                MDC.get(MDC_KEY)));
        }

        /**
         * Handles access denied errors from method security.
         */
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(new ErrorResponse(
                                                ErrorCode.FORBIDDEN.name(),
                                                "Access denied",
                                                MDC.get(MDC_KEY)));
        }

        /**
         * Handles custom API exceptions.
         * 
         */
        @ExceptionHandler(ApiException.class)
        public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
                return ResponseEntity.status(ex.getStatusCode())
                                .body(new ErrorResponse(
                                                ex.getErrorCode().name(),
                                                ex.getMessage(),
                                                MDC.get(MDC_KEY)));
        }

        /**
         * Handles resource not found (404).
         * for example, call a invalid URL.
         */
        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(new ErrorResponse(
                                                ErrorCode.NOT_FOUND.name(),
                                                "Resource not found",
                                                MDC.get(MDC_KEY)));
        }

        /**
         * Generic exception handler for unexpected errors.
         * handle all other exceptions.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
                logger.error("Unexpected exception occurred: {}", ex.getClass().getName(), ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new ErrorResponse(
                                                ErrorCode.INTERNAL_ERROR.name(),
                                                "An unexpected internal error occurred: " + ex.getClass().getName()
                                                                + " - " + ex.getMessage(),
                                                MDC.get(MDC_KEY)));
        }
}
