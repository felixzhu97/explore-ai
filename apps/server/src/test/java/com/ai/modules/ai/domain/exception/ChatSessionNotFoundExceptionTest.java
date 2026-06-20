package com.ai.modules.ai.domain.exception;

import com.ai.modules.ai.domain.exception.ChatSessionNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ChatSessionNotFoundException Tests
 * 
 * Tests for ChatSessionNotFoundException following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests exception construction and session id access
 */
@DisplayName("ChatSessionNotFoundException")
class ChatSessionNotFoundExceptionTest {

    private static final String TEST_SESSION_ID = UUID.randomUUID().toString();

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("should create exception with formatted message")
        void shouldCreateExceptionWithFormattedMessage() {
            // Act
            ChatSessionNotFoundException exception = new ChatSessionNotFoundException(TEST_SESSION_ID);

            // Assert
            assertThat(exception.getMessage()).isEqualTo("Chat session not found: " + TEST_SESSION_ID);
        }

        @Test
        @DisplayName("should extend RuntimeException")
        void shouldExtendRuntimeException() {
            // Act
            ChatSessionNotFoundException exception = new ChatSessionNotFoundException(TEST_SESSION_ID);

            // Assert
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should store session id")
        void shouldStoreSessionId() {
            // Act
            ChatSessionNotFoundException exception = new ChatSessionNotFoundException(TEST_SESSION_ID);

            // Assert
            assertThat(exception.getSessionId()).isEqualTo(TEST_SESSION_ID);
        }
    }

    @Nested
    @DisplayName("getSessionId")
    class GetSessionId {

        @Test
        @DisplayName("should return the session id passed to constructor")
        void shouldReturnTheSessionIdPassedToConstructor() {
            // Arrange
            String sessionId = UUID.randomUUID().toString();

            // Act
            ChatSessionNotFoundException exception = new ChatSessionNotFoundException(sessionId);

            // Assert
            assertThat(exception.getSessionId()).isEqualTo(sessionId);
        }

        @Test
        @DisplayName("should return same session id as in message")
        void shouldReturnSameSessionIdAsInMessage() {
            // Arrange
            String sessionId = UUID.randomUUID().toString();

            // Act
            ChatSessionNotFoundException exception = new ChatSessionNotFoundException(sessionId);

            // Assert
            assertThat(exception.getMessage()).contains(sessionId);
            assertThat(exception.getSessionId()).isEqualTo(sessionId);
        }

        @Test
        @DisplayName("should generate different exceptions for different session ids")
        void shouldGenerateDifferentExceptionsForDifferentSessionIds() {
            // Arrange
            String sessionId1 = UUID.randomUUID().toString();
            String sessionId2 = UUID.randomUUID().toString();

            // Act
            ChatSessionNotFoundException exception1 = new ChatSessionNotFoundException(sessionId1);
            ChatSessionNotFoundException exception2 = new ChatSessionNotFoundException(sessionId2);

            // Assert
            assertThat(exception1.getSessionId()).isEqualTo(sessionId1);
            assertThat(exception2.getSessionId()).isEqualTo(sessionId2);
            assertThat(exception1.getSessionId()).isNotEqualTo(exception2.getSessionId());
        }
    }

    @Nested
    @DisplayName("Message format")
    class MessageFormat {

        @Test
        @DisplayName("should contain 'Chat session not found' in message")
        void shouldContainChatSessionNotFoundInMessage() {
            // Act
            ChatSessionNotFoundException exception = new ChatSessionNotFoundException(TEST_SESSION_ID);

            // Assert
            assertThat(exception.getMessage()).contains("Chat session not found");
        }

        @Test
        @DisplayName("should include session id in message")
        void shouldIncludeSessionIdInMessage() {
            // Act
            ChatSessionNotFoundException exception = new ChatSessionNotFoundException(TEST_SESSION_ID);

            // Assert
            assertThat(exception.getMessage()).contains(TEST_SESSION_ID);
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
                throw new ChatSessionNotFoundException(TEST_SESSION_ID);
            })
                .isInstanceOf(ChatSessionNotFoundException.class)
                .hasMessageContaining("Chat session not found")
                .hasMessageContaining(TEST_SESSION_ID);
        }

        @Test
        @DisplayName("should allow accessing session id after throwing")
        void shouldAllowAccessingSessionIdAfterThrowing() {
            // Act & Assert
            assertThatThrownBy(() -> {
                throw new ChatSessionNotFoundException(TEST_SESSION_ID);
            })
                .isInstanceOf(ChatSessionNotFoundException.class)
                .extracting(e -> ((ChatSessionNotFoundException) e).getSessionId())
                .isEqualTo(TEST_SESSION_ID);
        }

        @Test
        @DisplayName("should be catchable as RuntimeException")
        void shouldBeCatchableAsRuntimeException() {
            // Arrange
            RuntimeException caught = null;

            // Act
            try {
                throw new ChatSessionNotFoundException(TEST_SESSION_ID);
            } catch (RuntimeException e) {
                caught = e;
            }

            // Assert
            assertThat(caught).isNotNull();
            assertThat(caught).isInstanceOf(ChatSessionNotFoundException.class);
        }
    }
}
