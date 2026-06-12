package com.ai.domain;

import com.ai.domain.exception.RagServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RagServiceException Tests
 * 
 * Tests for RagServiceException following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests exception construction and exception chaining
 */
@DisplayName("RagServiceException")
class RagServiceExceptionTest {

    private static final String TEST_MESSAGE = "Failed to retrieve context from vector store";

    @Nested
    @DisplayName("Constructor with message only")
    class ConstructorWithMessageOnly {

        @Test
        @DisplayName("should create exception with message")
        void shouldCreateExceptionWithMessage() {
            // Act
            RagServiceException exception = new RagServiceException(TEST_MESSAGE);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(TEST_MESSAGE);
        }

        @Test
        @DisplayName("should extend RuntimeException")
        void shouldExtendRuntimeException() {
            // Act
            RagServiceException exception = new RagServiceException(TEST_MESSAGE);

            // Assert
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should have no cause when not provided")
        void shouldHaveNoCauseWhenNotProvided() {
            // Act
            RagServiceException exception = new RagServiceException(TEST_MESSAGE);

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
            Throwable cause = new RuntimeException("Connection refused");

            // Act
            RagServiceException exception = new RagServiceException(TEST_MESSAGE, cause);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(TEST_MESSAGE);
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("should preserve exception chain")
        void shouldPreserveExceptionChain() {
            // Arrange
            Throwable rootCause = new RuntimeException("Socket timeout");
            Throwable intermediateCause = new RuntimeException("Request timeout", rootCause);

            // Act
            RagServiceException exception = new RagServiceException(TEST_MESSAGE, intermediateCause);

            // Assert
            assertThat(exception.getCause()).isEqualTo(intermediateCause);
            assertThat(exception.getCause().getCause()).isEqualTo(rootCause);
        }

        @Test
        @DisplayName("should support nested RagServiceException chaining")
        void shouldSupportNestedRagServiceExceptionChaining() {
            // Arrange
            RagServiceException cause = new RagServiceException("Original error");

            // Act
            RagServiceException exception = new RagServiceException(TEST_MESSAGE, cause);

            // Assert
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getCause().getMessage()).isEqualTo("Original error");
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
                throw new RagServiceException(TEST_MESSAGE);
            })
                .isInstanceOf(RagServiceException.class)
                .hasMessage(TEST_MESSAGE);
        }

        @Test
        @DisplayName("should preserve cause when caught and rethrown")
        void shouldPreserveCauseWhenCaughtAndRethrown() {
            // Arrange
            Throwable originalCause = new RuntimeException("Original cause");

            // Act & Assert
            assertThatThrownBy(() -> {
                throw new RagServiceException(TEST_MESSAGE, originalCause);
            })
                .isInstanceOf(RagServiceException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining(TEST_MESSAGE)
                .hasRootCauseMessage("Original cause");
        }

        @Test
        @DisplayName("should allow catching as RuntimeException")
        void shouldAllowCatchingAsRuntimeException() {
            // Arrange
            RuntimeException caught = null;

            // Act
            try {
                throw new RagServiceException(TEST_MESSAGE);
            } catch (RuntimeException e) {
                caught = e;
            }

            // Assert
            assertThat(caught).isNotNull();
            assertThat(caught).isInstanceOf(RagServiceException.class);
        }
    }
}
