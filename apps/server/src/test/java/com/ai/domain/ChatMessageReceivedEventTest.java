package com.ai.domain;

import com.ai.domain.event.ChatMessageReceivedEvent;
import com.ai.domain.vo.ChatSessionId;
import com.ai.domain.vo.MessageId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChatMessageReceivedEvent Tests
 * 
 * Tests for ChatMessageReceivedEvent following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests event data integrity
 */
@DisplayName("ChatMessageReceivedEvent")
class ChatMessageReceivedEventTest {

    private static final Instant TEST_INSTANT = Instant.parse("2024-01-15T10:30:00Z");
    private static final ChatSessionId TEST_SESSION_ID = ChatSessionId.of(UUID.randomUUID().toString());
    private static final MessageId TEST_MESSAGE_ID = MessageId.of(UUID.randomUUID().toString());
    private static final String TEST_CONTENT = "Hello, how can you help me?";

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create event with all provided values")
        void shouldCreateEventWithAllProvidedValues() {
            // Act
            ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(
                TEST_SESSION_ID, TEST_MESSAGE_ID, TEST_CONTENT, TEST_INSTANT
            );

            // Assert
            assertThat(event.getSessionId()).isEqualTo(TEST_SESSION_ID);
            assertThat(event.getMessageId()).isEqualTo(TEST_MESSAGE_ID);
            assertThat(event.getContent()).isEqualTo(TEST_CONTENT);
            assertThat(event.getOccurredAt()).isEqualTo(TEST_INSTANT);
        }

        @Test
        @DisplayName("should store session id correctly")
        void shouldStoreSessionIdCorrectly() {
            // Act
            ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(
                TEST_SESSION_ID, TEST_MESSAGE_ID, TEST_CONTENT, TEST_INSTANT
            );

            // Assert
            assertThat(event.getSessionId().value()).isEqualTo(TEST_SESSION_ID.value());
        }

        @Test
        @DisplayName("should store message id correctly")
        void shouldStoreMessageIdCorrectly() {
            // Act
            ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(
                TEST_SESSION_ID, TEST_MESSAGE_ID, TEST_CONTENT, TEST_INSTANT
            );

            // Assert
            assertThat(event.getMessageId().value()).isEqualTo(TEST_MESSAGE_ID.value());
        }

        @Test
        @DisplayName("should store content correctly")
        void shouldStoreContentCorrectly() {
            // Act
            ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(
                TEST_SESSION_ID, TEST_MESSAGE_ID, TEST_CONTENT, TEST_INSTANT
            );

            // Assert
            assertThat(event.getContent()).isEqualTo(TEST_CONTENT);
        }

        @Test
        @DisplayName("should store occurred at correctly")
        void shouldStoreOccurredAtCorrectly() {
            // Act
            ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(
                TEST_SESSION_ID, TEST_MESSAGE_ID, TEST_CONTENT, TEST_INSTANT
            );

            // Assert
            assertThat(event.getOccurredAt()).isEqualTo(TEST_INSTANT);
        }
    }

    @Nested
    @DisplayName("getEventType")
    class GetEventType {

        @Test
        @DisplayName("should return ChatMessageReceived")
        void shouldReturnChatMessageReceived() {
            // Arrange
            ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(
                TEST_SESSION_ID, TEST_MESSAGE_ID, TEST_CONTENT, TEST_INSTANT
            );

            // Act & Assert
            assertThat(event.getEventType()).isEqualTo("ChatMessageReceived");
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should include event type in toString")
        void shouldIncludeEventTypeInToString() {
            // Arrange
            ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(
                TEST_SESSION_ID, TEST_MESSAGE_ID, TEST_CONTENT, TEST_INSTANT
            );

            // Act
            String result = event.toString();

            // Assert
            assertThat(result).contains("ChatMessageReceivedEvent");
        }

        @Test
        @DisplayName("should include session id in toString")
        void shouldIncludeSessionIdInToString() {
            // Arrange
            ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(
                TEST_SESSION_ID, TEST_MESSAGE_ID, TEST_CONTENT, TEST_INSTANT
            );

            // Act
            String result = event.toString();

            // Assert
            assertThat(result).contains(TEST_SESSION_ID.value());
        }

        @Test
        @DisplayName("should include message id in toString")
        void shouldIncludeMessageIdInToString() {
            // Arrange
            ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(
                TEST_SESSION_ID, TEST_MESSAGE_ID, TEST_CONTENT, TEST_INSTANT
            );

            // Act
            String result = event.toString();

            // Assert
            assertThat(result).contains(TEST_MESSAGE_ID.value());
        }
    }
}
