package com.ai.ai.domain.exception;

import com.ai.ai.domain.exception.AiServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AiServiceException Tests")
class AiServiceExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("should set default error code when using simple message constructor")
        void shouldSetDefaultErrorCode_whenUsingSimpleMessageConstructor() {
            // When
            var exception = new AiServiceException("Test message");

            // Then
            assertThat(exception.getMessage()).isEqualTo("Test message");
            assertThat(exception.getErrorCode()).isEqualTo("AI_SERVICE_ERROR");
        }

        @Test
        @DisplayName("should set default error code when using message and cause constructor")
        void shouldSetDefaultErrorCode_whenUsingMessageAndCauseConstructor() {
            // Given
            var cause = new RuntimeException("Original error");

            // When
            var exception = new AiServiceException("Test message", cause);

            // Then
            assertThat(exception.getMessage()).isEqualTo("Test message");
            assertThat(exception.getCause()).isSameAs(cause);
            assertThat(exception.getErrorCode()).isEqualTo("AI_SERVICE_ERROR");
        }

        @Test
        @DisplayName("should set custom error code when using full constructor")
        void shouldSetCustomErrorCode_whenUsingFullConstructor() {
            // Given
            var cause = new RuntimeException("Original error");
            var customCode = "CUSTOM_ERROR_CODE";

            // When
            var exception = new AiServiceException("Test message", customCode, cause);

            // Then
            assertThat(exception.getMessage()).isEqualTo("Test message");
            assertThat(exception.getErrorCode()).isEqualTo(customCode);
            assertThat(exception.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("Error Code Tests")
    class ErrorCodeTests {

        @Test
        @DisplayName("should return correct default error code")
        void shouldReturnCorrectDefaultErrorCode() {
            // Given
            var exception = new AiServiceException("Message");

            // When & Then
            assertThat(exception.getErrorCode()).isEqualTo("AI_SERVICE_ERROR");
        }

        @Test
        @DisplayName("should return custom error code when provided")
        void shouldReturnCustomErrorCode_whenProvided() {
            // Given
            var customCode = "MODEL_NOT_FOUND";

            // When
            var exception = new AiServiceException("Error", customCode, null);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(customCode);
        }

        @Test
        @DisplayName("should handle null cause with custom error code")
        void shouldHandleNullCause_withCustomErrorCode() {
            // Given
            var customCode = "NULL_POINTER";

            // When
            var exception = new AiServiceException("Error", customCode, null);

            // Then
            assertThat(exception.getCause()).isNull();
            assertThat(exception.getErrorCode()).isEqualTo(customCode);
        }
    }
}
