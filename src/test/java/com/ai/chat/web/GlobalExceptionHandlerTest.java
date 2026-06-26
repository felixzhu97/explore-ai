package com.ai.chat.web;

import com.ai.chat.web.dto.ErrorResponse;
import com.ai.common.domain.exception.AiServiceException;
import com.ai.rag.domain.exception.DocumentNotFoundException;
import com.ai.rag.domain.exception.RagServiceException;
import com.ai.chat.domain.exception.ChatSessionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler Unit Tests
 * 
 * Tests all exception handler methods using AAA pattern:
 * - Naming convention: should_expected_when_condition
 * - Covers all exception types handled
 * - Tests edge cases and boundary conditions
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("ChatSessionNotFoundException")
    class HandleSessionNotFound {

        @Test
        @DisplayName("should return 404 with SESSION_NOT_FOUND error code")
        void shouldReturn404WithSessionNotFoundErrorCode() {
            ChatSessionNotFoundException exception = new ChatSessionNotFoundException("test-session-123");

            ResponseEntity<ErrorResponse> response = handler.handleSessionNotFound(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("SESSION_NOT_FOUND");
            assertThat(response.getBody().message()).contains("test-session-123");
        }

        @Test
        @DisplayName("should handle session not found with different session id")
        void shouldHandleSessionNotFoundWithDifferentSessionId() {
            ChatSessionNotFoundException exception = new ChatSessionNotFoundException("another-session");

            ResponseEntity<ErrorResponse> response = handler.handleSessionNotFound(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().message()).contains("another-session");
        }
    }

    @Nested
    @DisplayName("AiServiceException")
    class HandleAiServiceError {

        @Test
        @DisplayName("should return 503 with AI_SERVICE_ERROR error code")
        void shouldReturn503WithAiServiceErrorCode() {
            AiServiceException exception = new AiServiceException("OpenAI API error");

            ResponseEntity<ErrorResponse> response = handler.handleAiServiceError(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("AI_SERVICE_ERROR");
            assertThat(response.getBody().message()).contains("OpenAI API error");
        }

        @Test
        @DisplayName("should return 503 with custom error code")
        void shouldReturn503WithCustomErrorCode() {
            AiServiceException exception = new AiServiceException("Rate limit exceeded", "RATE_LIMIT", null);

            ResponseEntity<ErrorResponse> response = handler.handleAiServiceError(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody().errorCode()).isEqualTo("RATE_LIMIT");
        }

        @Test
        @DisplayName("should handle exception with cause")
        void shouldHandleExceptionWithCause() {
            Throwable cause = new RuntimeException("Network timeout");
            AiServiceException exception = new AiServiceException("Service unavailable", cause);

            ResponseEntity<ErrorResponse> response = handler.handleAiServiceError(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody().message()).contains("Service unavailable");
        }
    }

    @Nested
    @DisplayName("DocumentNotFoundException")
    class HandleDocumentNotFound {

        @Test
        @DisplayName("should return 404 with DOCUMENT_NOT_FOUND error code")
        void shouldReturn404WithDocumentNotFoundErrorCode() {
            UUID docId = UUID.randomUUID();
            DocumentNotFoundException exception = new DocumentNotFoundException(docId);

            ResponseEntity<ErrorResponse> response = handler.handleDocumentNotFound(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("DOCUMENT_NOT_FOUND");
            assertThat(response.getBody().message()).contains(docId.toString());
        }

        @Test
        @DisplayName("should handle document not found with specific UUID")
        void shouldHandleDocumentNotFoundWithSpecificUuid() {
            UUID specificId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            DocumentNotFoundException exception = new DocumentNotFoundException(specificId);

            ResponseEntity<ErrorResponse> response = handler.handleDocumentNotFound(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().errorCode()).isEqualTo("DOCUMENT_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("RagServiceException")
    class HandleRagServiceError {

        @Test
        @DisplayName("should return 500 with RAG_SERVICE_ERROR error code")
        void shouldReturn500WithRagServiceErrorCode() {
            RagServiceException exception = new RagServiceException("Vector store connection failed");

            ResponseEntity<ErrorResponse> response = handler.handleRagServiceError(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("RAG_SERVICE_ERROR");
            assertThat(response.getBody().message()).contains("Vector store connection failed");
        }

        @Test
        @DisplayName("should handle exception with nested cause")
        void shouldHandleExceptionWithNestedCause() {
            Throwable cause = new RuntimeException("Database error");
            RagServiceException exception = new RagServiceException("Search failed", cause);

            ResponseEntity<ErrorResponse> response = handler.handleRagServiceError(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().message()).contains("Search failed");
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException")
    class HandleValidationError {

        @Test
        @DisplayName("should return 400 with VALIDATION_ERROR error code")
        void shouldReturn400WithValidationErrorCode() {
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(
                new FieldError("object", "field1", "must not be null"),
                new FieldError("object", "field2", "must not be blank")
            ));

            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                null, bindingResult
            );

            ResponseEntity<ErrorResponse> response = handler.handleValidationError(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getBody().message()).contains("field1");
            assertThat(response.getBody().message()).contains("field2");
        }

        @Test
        @DisplayName("should format field errors as comma-separated")
        void shouldFormatFieldErrorsAsCommaSeparated() {
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(
                new FieldError("object", "name", "required"),
                new FieldError("object", "email", "invalid format")
            ));

            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                null, bindingResult
            );

            ResponseEntity<ErrorResponse> response = handler.handleValidationError(exception);

            assertThat(response.getBody().message()).contains("name: required");
            assertThat(response.getBody().message()).contains("email: invalid format");
        }

        @Test
        @DisplayName("should handle exception with empty field errors")
        void shouldHandleExceptionWithEmptyFieldErrors() {
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of());

            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                null, bindingResult
            );

            ResponseEntity<ErrorResponse> response = handler.handleValidationError(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException")
    class HandleIllegalArgument {

        @Test
        @DisplayName("should return 400 with BAD_REQUEST error code")
        void shouldReturn400WithBadRequestErrorCode() {
            IllegalArgumentException exception = new IllegalArgumentException("Invalid parameter value");

            ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("BAD_REQUEST");
            assertThat(response.getBody().message()).contains("Invalid parameter value");
        }

        @Test
        @DisplayName("should handle empty message")
        void shouldHandleEmptyMessage() {
            IllegalArgumentException exception = new IllegalArgumentException("");

            ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().errorCode()).isEqualTo("BAD_REQUEST");
        }
    }

    @Nested
    @DisplayName("MaxUploadSizeExceededException")
    class HandleMaxUploadSizeExceeded {

        @Test
        @DisplayName("should return 413 with FILE_TOO_LARGE error code")
        void shouldReturn413WithFileTooLargeErrorCode() {
            MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(52428800L);

            ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSizeExceeded(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("FILE_TOO_LARGE");
            assertThat(response.getBody().message()).contains("50MB");
        }

        @Test
        @DisplayName("should handle exception with different max size")
        void shouldHandleExceptionWithDifferentMaxSize() {
            MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(1024L);

            ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSizeExceeded(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
            assertThat(response.getBody().errorCode()).isEqualTo("FILE_TOO_LARGE");
        }
    }

    @Nested
    @DisplayName("Generic Exception Handler")
    class HandleGenericException {

        @Test
        @DisplayName("should return 500 with INTERNAL_ERROR error code")
        void shouldReturn500WithInternalErrorCode() {
            Exception exception = new RuntimeException("Unexpected error");

            ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
            assertThat(response.getBody().message()).contains("unexpected error");
        }

        @Test
        @DisplayName("should handle NullPointerException")
        void shouldHandleNullPointerException() {
            NullPointerException exception = new NullPointerException("Cannot invoke method on null");

            ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
        }

        @Test
        @DisplayName("should handle exception with cause")
        void shouldHandleExceptionWithCause() {
            Exception exception = new RuntimeException("Outer error", new RuntimeException("Inner error"));

            ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
        }
    }
}
