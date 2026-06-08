package com.ai.vision.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Global exception handler for Vision Service.
 */
@RestControllerAdvice
public class VisionExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleIllegalArgument(IllegalArgumentException e) {
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
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "error", "Service Unavailable",
                "message", e.getMessage(),
                "timestamp", Instant.now().toString()
            )));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleUnsupportedOperation(UnsupportedOperationException e) {
        return Mono.just(ResponseEntity
            .status(HttpStatus.NOT_IMPLEMENTED)
            .body(Map.of(
                "error", "Not Implemented",
                "message", e.getMessage(),
                "timestamp", Instant.now().toString()
            )));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return Mono.just(ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(Map.of(
                "error", "Payload Too Large",
                "message", "File size exceeds maximum allowed size",
                "timestamp", Instant.now().toString()
            )));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(Exception e) {
        return Mono.just(ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "error", "Internal Server Error",
                "message", e.getMessage(),
                "timestamp", Instant.now().toString()
            )));
    }
}
