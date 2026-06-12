package com.ai.domain;

import com.ai.domain.event.ChatMessageReceivedEvent;
import com.ai.domain.event.ChatResponseGeneratedEvent;
import com.ai.domain.event.DomainEvent;
import com.ai.domain.vo.ChatSessionId;
import com.ai.domain.vo.MessageId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DomainEvent Tests
 * 
 * Tests for DomainEvent following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests sealed class behavior and type safety
 */
@DisplayName("DomainEvent")
class DomainEventTest {

    private static final Instant TEST_INSTANT = Instant.parse("2024-01-15T10:30:00Z");
    private static final ChatSessionId TEST_SESSION_ID = ChatSessionId.of(UUID.randomUUID().toString());
    private static final MessageId TEST_MESSAGE_ID = MessageId.of(UUID.randomUUID().toString());
    private static final MessageId TEST_RESPONSE_MESSAGE_ID = MessageId.of(UUID.randomUUID().toString());

    @Nested
    @DisplayName("Sealed class type safety")
    class SealedClassTypeSafety {

        @Test
        @DisplayName("should allow ChatMessageReceivedEvent as subclass")
        void shouldAllowChatMessageReceivedEventAsSubclass() {
            // Arrange
            ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(
                TEST_SESSION_ID, TEST_MESSAGE_ID, "Hello", TEST_INSTANT
            );

            // Assert
            assertThat(event).isInstanceOf(DomainEvent.class);
        }

        @Test
        @DisplayName("should allow ChatResponseGeneratedEvent as subclass")
        void shouldAllowChatResponseGeneratedEventAsSubclass() {
            // Arrange
            ChatResponseGeneratedEvent event = new ChatResponseGeneratedEvent(
                TEST_SESSION_ID, TEST_MESSAGE_ID, TEST_RESPONSE_MESSAGE_ID, "Response", TEST_INSTANT
            );

            // Assert
            assertThat(event).isInstanceOf(DomainEvent.class);
        }

        @Test
        @DisplayName("should have getOccurredAt returning the timestamp")
        void shouldHaveGetOccurredAtReturningTheTimestamp() {
            // Arrange
            ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(
                TEST_SESSION_ID, TEST_MESSAGE_ID, "Hello", TEST_INSTANT
            );

            // Act & Assert
            assertThat(event.getOccurredAt()).isEqualTo(TEST_INSTANT);
        }
    }

    @Nested
    @DisplayName("getEventType")
    class GetEventType {

        @Test
        @DisplayName("should return ChatMessageReceived for ChatMessageReceivedEvent")
        void shouldReturnChatMessageReceivedForChatMessageReceivedEvent() {
            // Arrange
            ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(
                TEST_SESSION_ID, TEST_MESSAGE_ID, "Hello", TEST_INSTANT
            );

            // Act & Assert
            assertThat(event.getEventType()).isEqualTo("ChatMessageReceived");
        }

        @Test
        @DisplayName("should return ChatResponseGenerated for ChatResponseGeneratedEvent")
        void shouldReturnChatResponseGeneratedForChatResponseGeneratedEvent() {
            // Arrange
            ChatResponseGeneratedEvent event = new ChatResponseGeneratedEvent(
                TEST_SESSION_ID, TEST_MESSAGE_ID, TEST_RESPONSE_MESSAGE_ID, "Response", TEST_INSTANT
            );

            // Act & Assert
            assertThat(event.getEventType()).isEqualTo("ChatResponseGenerated");
        }
    }
}
