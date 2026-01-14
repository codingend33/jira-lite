package com.jiralite.backend.exception;

import com.jiralite.backend.dto.ErrorCode;

/**
 * Custom exception for controlled API errors.
 * Maps to specific HTTP status codes and error codes.
 */
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final int statusCode;

    public ApiException(ErrorCode errorCode, String message, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

