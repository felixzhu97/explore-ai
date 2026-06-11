package com.ai.interfaces.controller;

import com.ai.domain.exception.DocumentNotFoundException;
import com.ai.domain.exception.RagServiceException;
import com.ai.domain.model.AiServiceException;
import com.ai.domain.model.ChatSessionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler.
 * Converts domain exceptions to HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ChatSessionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSessionNotFound(ChatSessionNotFoundException e) {
        log.warn("Session not found: {}", e.getSessionId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(errorResponse("SESSION_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAiServiceError(AiServiceException e) {
        log.error("AI service error", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(errorResponse("AI_SERVICE_ERROR", e.getMessage()));
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentNotFound(DocumentNotFoundException e) {
        log.warn("Document not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "Document not found",
                "documentId", e.getMessage().replace("Document not found: ", ""),
                "type", "error",
                "timestamp", Instant.now().toString()
            ));
    }

    @ExceptionHandler(RagServiceException.class)
    public ResponseEntity<Map<String, Object>> handleRagServiceError(RagServiceException e) {
        log.error("RAG service error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse("RAG_SERVICE_ERROR", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", message);
        return ResponseEntity.badRequest()
            .body(errorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(errorResponse("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("Upload size exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(errorResponse("FILE_TOO_LARGE", "Uploaded file exceeds the maximum allowed size of 50MB"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private Map<String, Object> errorResponse(String type, String message) {
        return Map.of(
            "error", type,
            "message", message,
            "type", "error",
            "timestamp", Instant.now().toString()
        );
    }
}
