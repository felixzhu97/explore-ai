package com.ai.rag.domain.exception;

import com.ai.rag.domain.exception.DocumentNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DocumentNotFoundException Tests
 * 
 * Tests for DocumentNotFoundException following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests exception construction and message format
 */
@DisplayName("DocumentNotFoundException")
class DocumentNotFoundExceptionTest {

    private static final UUID TEST_DOCUMENT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("should create exception with formatted message")
        void shouldCreateExceptionWithFormattedMessage() {
            // Act
            DocumentNotFoundException exception = new DocumentNotFoundException(TEST_DOCUMENT_ID);

            // Assert
            assertThat(exception.getMessage()).isEqualTo("Document not found: " + TEST_DOCUMENT_ID);
        }

        @Test
        @DisplayName("should extend RuntimeException")
        void shouldExtendRuntimeException() {
            // Act
            DocumentNotFoundException exception = new DocumentNotFoundException(TEST_DOCUMENT_ID);

            // Assert
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Message format")
    class MessageFormat {

        @Test
        @DisplayName("should include document id in message")
        void shouldIncludeDocumentIdInMessage() {
            // Act
            DocumentNotFoundException exception = new DocumentNotFoundException(TEST_DOCUMENT_ID);

            // Assert
            assertThat(exception.getMessage()).contains(TEST_DOCUMENT_ID.toString());
        }

        @Test
        @DisplayName("should contain 'Document not found' in message")
        void shouldContainDocumentNotFoundInMessage() {
            // Act
            DocumentNotFoundException exception = new DocumentNotFoundException(TEST_DOCUMENT_ID);

            // Assert
            assertThat(exception.getMessage()).contains("Document not found");
        }

        @Test
        @DisplayName("should generate different messages for different ids")
        void shouldGenerateDifferentMessagesForDifferentIds() {
            // Arrange
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            // Act
            DocumentNotFoundException exception1 = new DocumentNotFoundException(id1);
            DocumentNotFoundException exception2 = new DocumentNotFoundException(id2);

            // Assert
            assertThat(exception1.getMessage()).isNotEqualTo(exception2.getMessage());
            assertThat(exception1.getMessage()).contains(id1.toString());
            assertThat(exception2.getMessage()).contains(id2.toString());
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
                throw new DocumentNotFoundException(TEST_DOCUMENT_ID);
            })
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("Document not found")
                .hasMessageContaining(TEST_DOCUMENT_ID.toString());
        }

        @Test
        @DisplayName("should be catchable as RuntimeException")
        void shouldBeCatchableAsRuntimeException() {
            // Act & Assert
            assertThatThrownBy(() -> {
                try {
                    throw new DocumentNotFoundException(TEST_DOCUMENT_ID);
                } catch (RuntimeException e) {
                    assertThat(e).isInstanceOf(DocumentNotFoundException.class);
                    throw e;
                }
            })
                .isInstanceOf(DocumentNotFoundException.class);
        }
    }
}
