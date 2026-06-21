package com.ai.modules.ai.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standard error response format for API errors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String message,
    String errorCode,
    Instant timestamp,
    String path
) {
    public static ErrorResponse of(String message, String errorCode) {
        return new ErrorResponse(message, errorCode, Instant.now(), null);
    }

    public static ErrorResponse of(String message, String errorCode, String path) {
        return new ErrorResponse(message, errorCode, Instant.now(), path);
    }
}
