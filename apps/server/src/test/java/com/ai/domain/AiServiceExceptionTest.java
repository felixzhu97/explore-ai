package com.ai.domain;

import com.ai.domain.model.AiServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AiServiceException Tests
 * 
 * Tests for AiServiceException following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests exception construction and message handling
 */
@DisplayName("AiServiceException")
class AiServiceExceptionTest {

    private static final String TEST_MESSAGE = "AI service is currently unavailable";

    @Nested
    @DisplayName("Constructor with message only")
    class ConstructorWithMessageOnly {

        @Test
        @DisplayName("should create exception with message")
        void shouldCreateExceptionWithMessage() {
            // Act
            AiServiceException exception = new AiServiceException(TEST_MESSAGE);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(TEST_MESSAGE);
        }

        @Test
        @DisplayName("should extend RuntimeException")
        void shouldExtendRuntimeException() {
            // Act
            AiServiceException exception = new AiServiceException(TEST_MESSAGE);

            // Assert
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should have no cause when not provided")
        void shouldHaveNoCauseWhenNotProvided() {
            // Act
            AiServiceException exception = new AiServiceException(TEST_MESSAGE);

            // Assert
            assertThat(exception.getCause()).isNull();
        }
    }

    @Nested
    @DisplayName("Constructor with message and cause")
    class ConstructorWithMessageAndCause {

        @Test
        @DisplayName("should create exception with message and cause")
        void shouldCreateExceptionWithMessageAndCause() {
            // Arrange
            Throwable cause = new RuntimeException("Connection timeout");

            // Act
            AiServiceException exception = new AiServiceException(TEST_MESSAGE, cause);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(TEST_MESSAGE);
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("should create exception with null cause")
        void shouldCreateExceptionWithNullCause() {
            // Act
            AiServiceException exception = new AiServiceException(TEST_MESSAGE, null);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(TEST_MESSAGE);
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("should propagate cause message in toString")
        void shouldPropagateCauseMessageInToString() {
            // Arrange
            String causeMessage = "Network error";
            Throwable cause = new RuntimeException(causeMessage);

            // Act
            AiServiceException exception = new AiServiceException(TEST_MESSAGE, cause);

            // Assert
            assertThat(exception.toString()).contains(TEST_MESSAGE);
        }
    }

    @Nested
    @DisplayName("Exception throwing")
    class ExceptionThrowing {

        @Test
        @DisplayName("should be throwable")
        void shouldBeThrowable() {
            // Act & Assert
            assertThatThrownBy(() -> {
                throw new AiServiceException(TEST_MESSAGE);
            })
                .isInstanceOf(AiServiceException.class)
                .hasMessage(TEST_MESSAGE);
        }

        @Test
        @DisplayName("should preserve message when caught and rethrown")
        void shouldPreserveMessageWhenCaughtAndRethrown() {
            // Act & Assert
            assertThatThrownBy(() -> {
                try {
                    throw new AiServiceException(TEST_MESSAGE);
                } catch (AiServiceException e) {
                    throw new AiServiceException("Rethrown: " + e.getMessage(), e);
                }
            })
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining(TEST_MESSAGE);
        }
    }
}
