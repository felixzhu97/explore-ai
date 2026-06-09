package com.ai.text.infrastructure.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Global exception handler for Text Service.
 */
@RestControllerAdvice
public class TextExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TextExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Bad Request",
                        "message", e.getMessage(),
                        "timestamp", Instant.now().toString()
                )));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleIllegalState(IllegalStateException e) {
        log.warn("Service unavailable: {}", e.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service Unavailable",
                        "message", e.getMessage(),
                        "timestamp", Instant.now().toString()
                )));
    }

    @ExceptionHandler(ApiKeyNotConfiguredException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleApiKeyNotConfigured(ApiKeyNotConfiguredException e) {
        log.warn("API key not configured: {}", e.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "API Key Not Configured",
                        "message", e.getMessage(),
                        "timestamp", Instant.now().toString()
                )));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal Server Error",
                        "message", e.getMessage(),
                        "timestamp", Instant.now().toString()
                )));
    }

    /**
     * Exception thrown when AI API key is not configured.
     */
    public static class ApiKeyNotConfiguredException extends RuntimeException {
        public ApiKeyNotConfiguredException(String message) {
            super(message);
        }
    }
}
