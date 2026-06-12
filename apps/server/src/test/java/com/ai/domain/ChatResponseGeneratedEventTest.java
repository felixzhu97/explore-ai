package com.ai.domain;

import com.ai.domain.event.ChatResponseGeneratedEvent;
import com.ai.domain.vo.ChatSessionId;
import com.ai.domain.vo.MessageId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChatResponseGeneratedEvent Tests
 * 
 * Tests for ChatResponseGeneratedEvent following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests event data integrity
 */
@DisplayName("ChatResponseGeneratedEvent")
class ChatResponseGeneratedEventTest {

    private static final Instant TEST_INSTANT = Instant.parse("2024-01-15T10:30:00Z");
    private static final ChatSessionId TEST_SESSION_ID = ChatSessionId.of(UUID.randomUUID().toString());
    private static final MessageId TEST_USER_MESSAGE_ID = MessageId.of(UUID.randomUUID().toString());
    private static final MessageId TEST_RESPONSE_MESSAGE_ID = MessageId.of(UUID.randomUUID().toString());
    private static final String TEST_RESPONSE_CONTENT = "I'm here to help you with your questions.";

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create event with all provided values")
        void shouldCreateEventWithAllProvidedValues() {
            // Act
            ChatResponseGeneratedEvent event = new ChatResponseGeneratedEvent(
                TEST_SESSION_ID, TEST_USER_MESSAGE_ID, TEST_RESPONSE_MESSAGE_ID, TEST_RESPONSE_CONTENT, TEST_INSTANT
            );

            // Assert
            assertThat(event.getSessionId()).isEqualTo(TEST_SESSION_ID);
            assertThat(event.getUserMessageId()).isEqualTo(TEST_USER_MESSAGE_ID);
            assertThat(event.getResponseMessageId()).isEqualTo(TEST_RESPONSE_MESSAGE_ID);
            assertThat(event.getResponseContent()).isEqualTo(TEST_RESPONSE_CONTENT);
            assertThat(event.getOccurredAt()).isEqualTo(TEST_INSTANT);
        }

        @Test
        @DisplayName("should store session id correctly")
        void shouldStoreSessionIdCorrectly() {
            // Act
            ChatResponseGeneratedEvent event = new ChatResponseGeneratedEvent(
                TEST_SESSION_ID, TEST_USER_MESSAGE_ID, TEST_RESPONSE_MESSAGE_ID, TEST_RESPONSE_CONTENT, TEST_INSTANT
            );

            // Assert
            assertThat(event.getSessionId().value()).isEqualTo(TEST_SESSION_ID.value());
        }

        @Test
        @DisplayName("should store user message id correctly")
        void shouldStoreUserMessageIdCorrectly() {
            // Act
            ChatResponseGeneratedEvent event = new ChatResponseGeneratedEvent(
                TEST_SESSION_ID, TEST_USER_MESSAGE_ID, TEST_RESPONSE_MESSAGE_ID, TEST_RESPONSE_CONTENT, TEST_INSTANT
            );

            // Assert
            assertThat(event.getUserMessageId().value()).isEqualTo(TEST_USER_MESSAGE_ID.value());
        }

        @Test
        @DisplayName("should store response message id correctly")
        void shouldStoreResponseMessageIdCorrectly() {
            // Act
            ChatResponseGeneratedEvent event = new ChatResponseGeneratedEvent(
                TEST_SESSION_ID, TEST_USER_MESSAGE_ID, TEST_RESPONSE_MESSAGE_ID, TEST_RESPONSE_CONTENT, TEST_INSTANT
            );

            // Assert
            assertThat(event.getResponseMessageId().value()).isEqualTo(TEST_RESPONSE_MESSAGE_ID.value());
        }

        @Test
        @DisplayName("should store response content correctly")
        void shouldStoreResponseContentCorrectly() {
            // Act
            ChatResponseGeneratedEvent event = new ChatResponseGeneratedEvent(
                TEST_SESSION_ID, TEST_USER_MESSAGE_ID, TEST_RESPONSE_MESSAGE_ID, TEST_RESPONSE_CONTENT, TEST_INSTANT
            );

            // Assert
            assertThat(event.getResponseContent()).isEqualTo(TEST_RESPONSE_CONTENT);
        }

        @Test
        @DisplayName("should store occurred at correctly")
        void shouldStoreOccurredAtCorrectly() {
            // Act
            ChatResponseGeneratedEvent event = new ChatResponseGeneratedEvent(
                TEST_SESSION_ID, TEST_USER_MESSAGE_ID, TEST_RESPONSE_MESSAGE_ID, TEST_RESPONSE_CONTENT, TEST_INSTANT
            );

            // Assert
            assertThat(event.getOccurredAt()).isEqualTo(TEST_INSTANT);
        }
    }

    @Nested
    @DisplayName("getEventType")
    class GetEventType {

        @Test
        @DisplayName("should return ChatResponseGenerated")
        void shouldReturnChatResponseGenerated() {
            // Arrange
            ChatResponseGeneratedEvent event = new ChatResponseGeneratedEvent(
                TEST_SESSION_ID, TEST_USER_MESSAGE_ID, TEST_RESPONSE_MESSAGE_ID, TEST_RESPONSE_CONTENT, TEST_INSTANT
            );

            // Act & Assert
            assertThat(event.getEventType()).isEqualTo("ChatResponseGenerated");
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should include event type in toString")
        void shouldIncludeEventTypeInToString() {
            // Arrange
            ChatResponseGeneratedEvent event = new ChatResponseGeneratedEvent(
                TEST_SESSION_ID, TEST_USER_MESSAGE_ID, TEST_RESPONSE_MESSAGE_ID, TEST_RESPONSE_CONTENT, TEST_INSTANT
            );

            // Act
            String result = event.toString();

            // Assert
            assertThat(result).contains("ChatResponseGeneratedEvent");
        }

        @Test
        @DisplayName("should include session id in toString")
        void shouldIncludeSessionIdInToString() {
            // Arrange
            ChatResponseGeneratedEvent event = new ChatResponseGeneratedEvent(
                TEST_SESSION_ID, TEST_USER_MESSAGE_ID, TEST_RESPONSE_MESSAGE_ID, TEST_RESPONSE_CONTENT, TEST_INSTANT
            );

            // Act
            String result = event.toString();

            // Assert
            assertThat(result).contains(TEST_SESSION_ID.value());
        }

        @Test
        @DisplayName("should include user message id in toString")
        void shouldIncludeUserMessageIdInToString() {
            // Arrange
            ChatResponseGeneratedEvent event = new ChatResponseGeneratedEvent(
                TEST_SESSION_ID, TEST_USER_MESSAGE_ID, TEST_RESPONSE_MESSAGE_ID, TEST_RESPONSE_CONTENT, TEST_INSTANT
            );

            // Act
            String result = event.toString();

            // Assert
            assertThat(result).contains(TEST_USER_MESSAGE_ID.value());
        }

        @Test
        @DisplayName("should include response message id in toString")
        void shouldIncludeResponseMessageIdInToString() {
            // Arrange
            ChatResponseGeneratedEvent event = new ChatResponseGeneratedEvent(
                TEST_SESSION_ID, TEST_USER_MESSAGE_ID, TEST_RESPONSE_MESSAGE_ID, TEST_RESPONSE_CONTENT, TEST_INSTANT
            );

            // Act
            String result = event.toString();

            // Assert
            assertThat(result).contains(TEST_RESPONSE_MESSAGE_ID.value());
        }
    }
}
