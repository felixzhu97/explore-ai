package com.ai.common.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

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
