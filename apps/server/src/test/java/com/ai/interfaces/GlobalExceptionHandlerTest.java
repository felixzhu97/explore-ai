package com.ai.interfaces;

import com.ai.domain.exception.DocumentNotFoundException;
import com.ai.domain.exception.RagServiceException;
import com.ai.domain.model.AiServiceException;
import com.ai.domain.model.ChatSessionNotFoundException;
import com.ai.interfaces.controller.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GlobalExceptionHandler Tests
 *
 * Tests exception-to-HTTP status code mappings:
 * - ChatSessionNotFoundException → 404
 * - DocumentNotFoundException → 404
 * - AiServiceException → 503
 * - RagServiceException → 500
 * - MethodArgumentNotValidException → 400
 * - IllegalArgumentException → 400
 * - MaxUploadSizeExceededException → 413
 * - Generic Exception → 500
 */
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("shouldReturn404_WhenSessionNotFound")
    class ChatSessionNotFoundTests {

        @Test
        @DisplayName("should return 404 when ChatSessionNotFoundException is thrown")
        void shouldReturn404WhenChatSessionNotFoundExceptionThrown() {
            // Arrange
            String sessionId = "session-123";
            ChatSessionNotFoundException exception = new ChatSessionNotFoundException(sessionId);

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleSessionNotFound(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).containsEntry("error", "SESSION_NOT_FOUND");
            assertThat(response.getBody()).containsEntry("type", "error");
            assertThat(response.getBody()).containsKey("timestamp");
        }

        @Test
        @DisplayName("should include session ID in error message")
        void shouldIncludeSessionIdInErrorMessage() {
            // Arrange
            String sessionId = "abc-456";
            ChatSessionNotFoundException exception = new ChatSessionNotFoundException(sessionId);

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleSessionNotFound(exception);

            // Assert
            assertThat(response.getBody()).containsEntry("message", exception.getMessage());
            assertThat(response.getBody()).containsEntry("message", "Chat session not found: abc-456");
        }
    }

    @Nested
    @DisplayName("shouldReturn404_WhenDocumentNotFound")
    class DocumentNotFoundTests {

        @Test
        @DisplayName("should return 404 when DocumentNotFoundException is thrown")
        void shouldReturn404WhenDocumentNotFoundExceptionThrown() {
            // Arrange
            UUID docId = UUID.randomUUID();
            DocumentNotFoundException exception = new DocumentNotFoundException(docId);

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleDocumentNotFound(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).containsEntry("error", "Document not found");
            assertThat(response.getBody()).containsEntry("type", "error");
        }

        @Test
        @DisplayName("should include document ID in error response")
        void shouldIncludeDocumentIdInErrorResponse() {
            // Arrange
            UUID docId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            DocumentNotFoundException exception = new DocumentNotFoundException(docId);

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleDocumentNotFound(exception);

            // Assert
            assertThat(response.getBody()).containsEntry("documentId", "123e4567-e89b-12d3-a456-426614174000");
        }
    }

    @Nested
    @DisplayName("shouldReturn503_WhenAiServiceError")
    class AiServiceExceptionTests {

        @Test
        @DisplayName("should return 503 when AiServiceException is thrown")
        void shouldReturn503WhenAiServiceExceptionThrown() {
            // Arrange
            AiServiceException exception = new AiServiceException("AI service unavailable");

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleAiServiceError(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody()).containsEntry("error", "AI_SERVICE_ERROR");
            assertThat(response.getBody()).containsEntry("message", "AI service unavailable");
            assertThat(response.getBody()).containsEntry("type", "error");
        }

        @Test
        @DisplayName("should handle AiServiceException with cause")
        void shouldHandleAiServiceExceptionWithCause() {
            // Arrange
            Throwable cause = new RuntimeException("Connection timeout");
            AiServiceException exception = new AiServiceException("AI service failed", cause);

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleAiServiceError(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody()).containsEntry("error", "AI_SERVICE_ERROR");
        }
    }

    @Nested
    @DisplayName("shouldReturn500_WhenRagServiceError")
    class RagServiceExceptionTests {

        @Test
        @DisplayName("should return 500 when RagServiceException is thrown")
        void shouldReturn500WhenRagServiceExceptionThrown() {
            // Arrange
            RagServiceException exception = new RagServiceException("RAG processing failed");

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleRagServiceError(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).containsEntry("error", "RAG_SERVICE_ERROR");
            assertThat(response.getBody()).containsEntry("message", "RAG processing failed");
            assertThat(response.getBody()).containsEntry("type", "error");
        }

        @Test
        @DisplayName("should handle RagServiceException with cause")
        void shouldHandleRagServiceExceptionWithCause() {
            // Arrange
            Throwable cause = new RuntimeException("Database connection error");
            RagServiceException exception = new RagServiceException("RAG service error", cause);

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleRagServiceError(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).containsEntry("error", "RAG_SERVICE_ERROR");
        }
    }

    @Nested
    @DisplayName("shouldReturn400_WhenValidationError")
    class MethodArgumentNotValidExceptionTests {

        @Test
        @DisplayName("should return 400 when MethodArgumentNotValidException is thrown")
        void shouldReturn400WhenMethodArgumentNotValidExceptionThrown() {
            // Arrange
            MethodArgumentNotValidException exception = createValidationException("message", "Message is required");

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleValidationError(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).containsEntry("error", "VALIDATION_ERROR");
            assertThat(response.getBody()).containsEntry("type", "error");
        }

        @Test
        @DisplayName("should include field errors in message")
        void shouldIncludeFieldErrorsInMessage() {
            // Arrange
            MethodArgumentNotValidException exception = createValidationException("message", "must not be blank");

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleValidationError(exception);

            // Assert
            assertThat(response.getBody()).containsEntry("message", "message: must not be blank");
        }

        @Test
        @DisplayName("should format multiple field errors as comma-separated")
        void shouldFormatMultipleFieldErrorsAsCommaSeparated() {
            // Arrange
            MethodArgumentNotValidException exception = createValidationExceptionWithMultipleErrors();

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleValidationError(exception);

            // Assert
            assertThat(response.getBody()).containsEntry("error", "VALIDATION_ERROR");
            assertThat(response.getBody().get("message").toString()).contains("message:");
            assertThat(response.getBody().get("message").toString()).contains("name:");
        }
    }

    @Nested
    @DisplayName("shouldReturn400_WhenIllegalArgument")
    class IllegalArgumentExceptionTests {

        @Test
        @DisplayName("should return 400 when IllegalArgumentException is thrown")
        void shouldReturn400WhenIllegalArgumentExceptionThrown() {
            // Arrange
            IllegalArgumentException exception = new IllegalArgumentException("Invalid parameter");

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).containsEntry("error", "BAD_REQUEST");
            assertThat(response.getBody()).containsEntry("message", "Invalid parameter");
            assertThat(response.getBody()).containsEntry("type", "error");
        }

        @Test
        @DisplayName("should preserve exception message")
        void shouldPreserveExceptionMessage() {
            // Arrange
            String customMessage = "Session ID cannot be empty";
            IllegalArgumentException exception = new IllegalArgumentException(customMessage);

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(exception);

            // Assert
            assertThat(response.getBody()).containsEntry("message", customMessage);
        }
    }

    @Nested
    @DisplayName("shouldReturn413_WhenFileTooLarge")
    class MaxUploadSizeExceededExceptionTests {

        @Test
        @DisplayName("should return 413 when MaxUploadSizeExceededException is thrown")
        void shouldReturn413WhenMaxUploadSizeExceededExceptionThrown() {
            // Arrange - MaxUploadSizeExceededException requires a long parameter for max upload size
            MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(52428800L);

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleMaxUploadSizeExceeded(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
            assertThat(response.getBody()).containsEntry("error", "FILE_TOO_LARGE");
            assertThat(response.getBody()).containsEntry("message", "Uploaded file exceeds the maximum allowed size of 50MB");
            assertThat(response.getBody()).containsEntry("type", "error");
        }
    }

    @Nested
    @DisplayName("shouldReturn500_WhenGenericException")
    class GenericExceptionTests {

        @Test
        @DisplayName("should return 500 when generic Exception is thrown")
        void shouldReturn500WhenGenericExceptionThrown() {
            // Arrange
            Exception exception = new Exception("Something went wrong");

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleGenericException(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).containsEntry("error", "INTERNAL_ERROR");
            assertThat(response.getBody()).containsEntry("message", "An unexpected error occurred");
            assertThat(response.getBody()).containsEntry("type", "error");
        }

        @Test
        @DisplayName("should hide internal error details from client")
        void shouldHideInternalErrorDetailsFromClient() {
            // Arrange
            Exception exception = new RuntimeException("Database connection failed");

            // Act
            ResponseEntity<Map<String, Object>> response = handler.handleGenericException(exception);

            // Assert
            assertThat(response.getBody()).containsEntry("message", "An unexpected error occurred");
            assertThat(response.getBody().get("message").toString()).doesNotContain("Database");
        }
    }

    // Helper methods
    private MethodArgumentNotValidException createValidationException(String field, String message) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new TestRequest(), "testRequest");
        bindingResult.addError(new FieldError("testRequest", field, message));
        return new MethodArgumentNotValidException(null, bindingResult);
    }

    private MethodArgumentNotValidException createValidationExceptionWithMultipleErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new TestRequest(), "testRequest");
        bindingResult.addError(new FieldError("testRequest", "message", "must not be blank"));
        bindingResult.addError(new FieldError("testRequest", "name", "must not be empty"));
        return new MethodArgumentNotValidException(null, bindingResult);
    }

    // Test classes for validation
    static class TestRequest {
        private String message;
        private String name;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
