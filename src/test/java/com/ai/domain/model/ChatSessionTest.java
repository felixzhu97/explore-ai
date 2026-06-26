package com.ai.domain.model;

import com.ai.ai.domain.vo.ChatSessionId;
import com.ai.ai.domain.model.ChatMessage;
import com.ai.ai.domain.model.ChatSession;
import com.ai.ai.domain.vo.MessageId;
import com.ai.ai.domain.vo.MessageRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ChatSession Domain Model Tests
 * 
 * Tests for ChatSession aggregate root following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests domain model business rules and state transitions
 */
@DisplayName("ChatSession")
class ChatSessionTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create session with generated ID")
        void shouldCreateSessionWithGeneratedId() {
            // Act
            ChatSession session = ChatSession.create("Test Chat");

            // Assert
            assertThat(session.getId()).isNotNull();
            assertThat(session.getId().value()).isNotBlank();
        }

        @Test
        @DisplayName("should create unique session IDs")
        void shouldCreateUniqueSessionIds() {
            // Act
            ChatSession session1 = ChatSession.create("Session 1");
            ChatSession session2 = ChatSession.create("Session 2");

            // Assert
            assertThat(session1.getId()).isNotEqualTo(session2.getId());
        }

        @Test
        @DisplayName("should use provided title")
        void shouldUseProvidedTitle() {
            // Act
            ChatSession session = ChatSession.create("My Custom Title");

            // Assert
            assertThat(session.getTitle()).isEqualTo("My Custom Title");
        }

        @Test
        @DisplayName("should use default title when null")
        void shouldUseDefaultTitleWhenNull() {
            // Act
            ChatSession session = ChatSession.create(null);

            // Assert
            assertThat(session.getTitle()).isEqualTo("New Chat");
        }

        @Test
        @DisplayName("should use default title when blank")
        void shouldUseDefaultTitleWhenBlank() {
            // Act
            ChatSession session = ChatSession.create("   ");

            // Assert
            assertThat(session.getTitle()).isEqualTo("New Chat");
        }

        @Test
        @DisplayName("should truncate title when exceeding 100 characters")
        void shouldTruncateTitleWhenExceeding100Characters() {
            // Arrange
            String longTitle = "A".repeat(150);

            // Act
            ChatSession session = ChatSession.create(longTitle);

            // Assert
            assertThat(session.getTitle()).hasSize(100);
            assertThat(session.getTitle()).isEqualTo("A".repeat(100));
        }

        @Test
        @DisplayName("should trim whitespace from title")
        void shouldTrimWhitespaceFromTitle() {
            // Act
            ChatSession session = ChatSession.create("  Trimmed Title  ");

            // Assert
            assertThat(session.getTitle()).isEqualTo("Trimmed Title");
        }

        @Test
        @DisplayName("should initialize with creation timestamp")
        void shouldInitializeWithCreationTimestamp() {
            // Arrange
            Instant before = Instant.now();

            // Act
            ChatSession session = ChatSession.create("Test");

            // Assert
            Instant after = Instant.now();
            assertThat(session.getCreatedAt()).isAfterOrEqualTo(before);
            assertThat(session.getCreatedAt()).isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("should initialize lastActivityAt same as createdAt")
        void shouldInitializeLastActivityAtSameAsCreatedAt() {
            // Act
            ChatSession session = ChatSession.create("Test");

            // Assert
            assertThat(session.getLastActivityAt()).isEqualTo(session.getCreatedAt());
        }

        @Test
        @DisplayName("should create session with empty messages")
        void shouldCreateSessionWithEmptyMessages() {
            // Act
            ChatSession session = ChatSession.create("Test");

            // Assert
            assertThat(session.getMessages()).isEmpty();
            assertThat(session.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("should create session with of factory method")
        void shouldCreateSessionWithOfFactoryMethod() {
            // Arrange
            ChatSessionId id = ChatSessionId.generate();
            Instant createdAt = Instant.now();

            // Act
            ChatSession session = ChatSession.of(id, "Test", createdAt);

            // Assert
            assertThat(session.getId()).isEqualTo(id);
            assertThat(session.getTitle()).isEqualTo("Test");
            assertThat(session.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("Adding Messages")
    class AddingMessages {

        @Test
        @DisplayName("should add user message with correct role")
        void shouldAddUserMessageWithCorrectRole() {
            // Arrange
            ChatSession session = ChatSession.create("Test");

            // Act
            ChatMessage message = session.addUserMessage("Hello!");

            // Assert
            assertThat(message.getRole()).isEqualTo(MessageRole.USER);
            assertThat(message.isFromUser()).isTrue();
            assertThat(message.isFromAssistant()).isFalse();
        }

        @Test
        @DisplayName("should add user message with correct text")
        void shouldAddUserMessageWithCorrectText() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            String text = "Hello, AI!";

            // Act
            ChatMessage message = session.addUserMessage(text);

            // Assert
            assertThat(message.getText()).isEqualTo(text);
        }

        @Test
        @DisplayName("should add assistant message with correct role")
        void shouldAddAssistantMessageWithCorrectRole() {
            // Arrange
            ChatSession session = ChatSession.create("Test");

            // Act
            ChatMessage message = session.addAssistantMessage("Hello!");

            // Assert
            assertThat(message.getRole()).isEqualTo(MessageRole.ASSISTANT);
            assertThat(message.isFromAssistant()).isTrue();
            assertThat(message.isFromUser()).isFalse();
        }

        @Test
        @DisplayName("should add assistant message with correct text")
        void shouldAddAssistantMessageWithCorrectText() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            String text = "Hello, human!";

            // Act
            ChatMessage message = session.addAssistantMessage(text);

            // Assert
            assertThat(message.getText()).isEqualTo(text);
        }

        @Test
        @DisplayName("should preserve message order")
        void shouldPreserveMessageOrder() {
            // Arrange
            ChatSession session = ChatSession.create("Test");

            // Act
            session.addUserMessage("First");
            session.addAssistantMessage("Second");
            session.addUserMessage("Third");
            session.addAssistantMessage("Fourth");

            // Assert
            List<ChatMessage> messages = session.getMessages();
            assertThat(messages).hasSize(4);
            assertThat(messages.get(0).getText()).isEqualTo("First");
            assertThat(messages.get(1).getText()).isEqualTo("Second");
            assertThat(messages.get(2).getText()).isEqualTo("Third");
            assertThat(messages.get(3).getText()).isEqualTo("Fourth");
        }

        @Test
        @DisplayName("should return unmodifiable messages list")
        void shouldReturnUnmodifiableMessagesList() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");

            // Act & Assert
            assertThatThrownBy(() -> session.getMessages().add(ChatMessage.createUserMessage("Test")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should update lastActivityAt when adding message")
        void shouldUpdateLastActivityAtWhenAddingMessage() throws InterruptedException {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            Instant beforeAdd = session.getLastActivityAt();
            Thread.sleep(10);

            // Act
            session.addUserMessage("New message");

            // Assert
            assertThat(session.getLastActivityAt()).isAfter(beforeAdd);
        }
    }

    @Nested
    @DisplayName("Message Count")
    class MessageCount {

        @Test
        @DisplayName("should return zero when no messages")
        void shouldReturnZeroWhenNoMessages() {
            // Arrange
            ChatSession session = ChatSession.create("Test");

            // Assert
            assertThat(session.getMessageCount()).isZero();
        }

        @Test
        @DisplayName("should return correct count for multiple messages")
        void shouldReturnCorrectCountForMultipleMessages() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");
            session.addAssistantMessage("Hi");
            session.addUserMessage("How are you?");

            // Assert
            assertThat(session.getMessageCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return correct user message count")
        void shouldReturnCorrectUserMessageCount() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");
            session.addAssistantMessage("Hi");
            session.addUserMessage("How are you?");
            session.addAssistantMessage("Good");

            // Assert
            assertThat(session.getUserMessageCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return correct assistant message count")
        void shouldReturnCorrectAssistantMessageCount() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");
            session.addAssistantMessage("Hi");
            session.addAssistantMessage("How can I help?");

            // Assert
            assertThat(session.getAssistantMessageCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Get Last Message")
    class GetLastMessage {

        @Test
        @DisplayName("should return null when no messages")
        void shouldReturnNullWhenNoMessages() {
            // Arrange
            ChatSession session = ChatSession.create("Test");

            // Assert
            assertThat(session.getLastUserMessage()).isNull();
            assertThat(session.getLastAssistantMessage()).isNull();
        }

        @Test
        @DisplayName("should return last user message")
        void shouldReturnLastUserMessage() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("First");
            session.addAssistantMessage("Response");
            ChatMessage lastUser = session.addUserMessage("Second Question");

            // Act
            ChatMessage result = session.getLastUserMessage();

            // Assert
            assertThat(result).isEqualTo(lastUser);
            assertThat(result.getText()).isEqualTo("Second Question");
        }

        @Test
        @DisplayName("should return last assistant message")
        void shouldReturnLastAssistantMessage() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Question");
            ChatMessage lastAssistant = session.addAssistantMessage("Last Response");
            session.addUserMessage("New Question");

            // Act
            ChatMessage result = session.getLastAssistantMessage();

            // Assert
            assertThat(result).isEqualTo(lastAssistant);
            assertThat(result.getText()).isEqualTo("Last Response");
        }
    }

    @Nested
    @DisplayName("Get Recent Messages")
    class GetRecentMessages {

        @Test
        @DisplayName("should return empty list when no messages")
        void shouldReturnEmptyListWhenNoMessages() {
            // Arrange
            ChatSession session = ChatSession.create("Test");

            // Act
            List<ChatMessage> recent = session.getRecentMessages(5);

            // Assert
            assertThat(recent).isEmpty();
        }

        @Test
        @DisplayName("should return all messages when count exceeds message count")
        void shouldReturnAllMessagesWhenCountExceedsMessageCount() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("One");
            session.addUserMessage("Two");
            session.addUserMessage("Three");

            // Act
            List<ChatMessage> recent = session.getRecentMessages(10);

            // Assert
            assertThat(recent).hasSize(3);
        }

        @Test
        @DisplayName("should return last N messages")
        void shouldReturnLastNMessages() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("First");
            session.addUserMessage("Second");
            session.addUserMessage("Third");
            session.addUserMessage("Fourth");
            session.addUserMessage("Fifth");

            // Act
            List<ChatMessage> recent = session.getRecentMessages(3);

            // Assert
            assertThat(recent).hasSize(3);
            assertThat(recent.get(0).getText()).isEqualTo("Third");
            assertThat(recent.get(1).getText()).isEqualTo("Fourth");
            assertThat(recent.get(2).getText()).isEqualTo("Fifth");
        }

        @Test
        @DisplayName("should return empty list when count is zero")
        void shouldReturnEmptyListWhenCountIsZero() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");

            // Act
            List<ChatMessage> recent = session.getRecentMessages(0);

            // Assert
            assertThat(recent).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when count is negative")
        void shouldReturnEmptyListWhenCountIsNegative() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");

            // Act
            List<ChatMessage> recent = session.getRecentMessages(-1);

            // Assert
            assertThat(recent).isEmpty();
        }
    }

    @Nested
    @DisplayName("Clear Messages")
    class ClearMessages {

        @Test
        @DisplayName("should clear all messages")
        void shouldClearAllMessages() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");
            session.addAssistantMessage("Hi");
            session.addUserMessage("How are you?");

            // Act
            session.clearMessages();

            // Assert
            assertThat(session.getMessages()).isEmpty();
            assertThat(session.getMessageCount()).isZero();
            assertThat(session.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("should update lastActivityAt when clearing messages")
        void shouldUpdateLastActivityAtWhenClearingMessages() throws InterruptedException {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");
            Instant beforeClear = session.getLastActivityAt();
            Thread.sleep(10);

            // Act
            session.clearMessages();

            // Assert
            assertThat(session.getLastActivityAt()).isAfter(beforeClear);
        }
    }

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityAndHashCode {

        @Test
        @DisplayName("should be equal when same ID")
        void shouldBeEqualWhenSameId() {
            // Arrange
            ChatSessionId id = ChatSessionId.of("test-id");
            ChatSession session1 = ChatSession.of(id, "Title 1", Instant.now());
            ChatSession session2 = ChatSession.of(id, "Title 2", Instant.now().plusSeconds(10));

            // Assert
            assertThat(session1).isEqualTo(session2);
            assertThat(session1.hashCode()).isEqualTo(session2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when different ID")
        void shouldNotBeEqualWhenDifferentId() {
            // Arrange
            ChatSession session1 = ChatSession.of(ChatSessionId.generate(), "Title", Instant.now());
            ChatSession session2 = ChatSession.of(ChatSessionId.generate(), "Title", Instant.now());

            // Assert
            assertThat(session1).isNotEqualTo(session2);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Arrange
            ChatSession session = ChatSession.create("Test");

            // Assert
            assertThat(session).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            // Arrange
            ChatSession session = ChatSession.create("Test");

            // Assert
            assertThat(session).isNotEqualTo("Not a session");
        }
    }

    @Nested
    @DisplayName("validateTitle")
    class ValidateTitle {

        @Test
        @DisplayName("should return New Chat when null")
        void shouldReturnNewChatWhenNull() {
            // Arrange
            String nullTitle = null;

            // Act
            ChatSession session = ChatSession.create(nullTitle);

            // Assert
            assertThat(session.getTitle()).isEqualTo("New Chat");
        }

        @Test
        @DisplayName("should trim whitespace")
        void shouldTrimWhitespace() {
            // Arrange
            String whitespaceTitle = "  Whitespace Title  ";

            // Act
            ChatSession session = ChatSession.create(whitespaceTitle);

            // Assert
            assertThat(session.getTitle()).isEqualTo("Whitespace Title");
        }

        @Test
        @DisplayName("should truncate over 100 chars")
        void shouldTruncateOver100Chars() {
            // Arrange
            String longTitle = "A".repeat(150);

            // Act
            ChatSession session = ChatSession.create(longTitle);

            // Assert
            assertThat(session.getTitle()).hasSize(100);
        }

        @Test
        @DisplayName("should return New Chat when blank")
        void shouldReturnNewChatWhenBlank() {
            // Act
            ChatSession session = ChatSession.create("   ");

            // Assert
            assertThat(session.getTitle()).isEqualTo("New Chat");
        }
    }

    @Nested
    @DisplayName("message counting")
    class MessageCounting {

        @Test
        @DisplayName("should count user messages")
        void shouldCountUserMessages() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");
            session.addAssistantMessage("Hi");
            session.addUserMessage("How are you?");
            session.addAssistantMessage("Fine");

            // Act
            int count = session.getUserMessageCount();

            // Assert
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("should count assistant messages")
        void shouldCountAssistantMessages() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");
            session.addAssistantMessage("Hi");
            session.addAssistantMessage("How can I help?");

            // Act
            int count = session.getAssistantMessageCount();

            // Assert
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("should return zero when no messages")
        void shouldReturnZeroWhenNoMessages() {
            // Arrange
            ChatSession session = ChatSession.create("Test");

            // Act & Assert
            assertThat(session.getUserMessageCount()).isZero();
            assertThat(session.getAssistantMessageCount()).isZero();
            assertThat(session.getMessageCount()).isZero();
        }
    }

    @Nested
    @DisplayName("getLastMessage")
    class GetLastMessageTests {

        @Test
        @DisplayName("should return last user message")
        void shouldReturnLastUserMessage() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("First");
            session.addAssistantMessage("Response");
            ChatMessage lastUserMsg = session.addUserMessage("Second Question");

            // Act
            ChatMessage result = session.getLastUserMessage();

            // Assert
            assertThat(result).isEqualTo(lastUserMsg);
        }

        @Test
        @DisplayName("should return last assistant message")
        void shouldReturnLastAssistantMessage() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Question");
            ChatMessage lastAssistantMsg = session.addAssistantMessage("Last Response");
            session.addUserMessage("Another Question");

            // Act
            ChatMessage result = session.getLastAssistantMessage();

            // Assert
            assertThat(result).isEqualTo(lastAssistantMsg);
        }

        @Test
        @DisplayName("should return null when no messages")
        void shouldReturnNullWhenNoMessages() {
            // Arrange
            ChatSession session = ChatSession.create("Test");

            // Act & Assert
            assertThat(session.getLastUserMessage()).isNull();
            assertThat(session.getLastAssistantMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("getRecentMessages")
    class GetRecentMessagesTests {

        @Test
        @DisplayName("should return recent messages")
        void shouldReturnRecentMessages() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("First");
            session.addUserMessage("Second");
            session.addUserMessage("Third");

            // Act
            List<ChatMessage> recent = session.getRecentMessages(2);

            // Assert
            assertThat(recent).hasSize(2);
            assertThat(recent.get(0).getText()).isEqualTo("Second");
            assertThat(recent.get(1).getText()).isEqualTo("Third");
        }

        @Test
        @DisplayName("should return all when count exceeds size")
        void shouldReturnAllWhenCountExceedsSize() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("One");
            session.addUserMessage("Two");

            // Act
            List<ChatMessage> recent = session.getRecentMessages(10);

            // Assert
            assertThat(recent).hasSize(2);
        }

        @Test
        @DisplayName("should return empty when count is zero or negative")
        void shouldReturnEmptyWhenCountIsZeroOrNegative() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");

            // Act
            List<ChatMessage> zeroResult = session.getRecentMessages(0);
            List<ChatMessage> negativeResult = session.getRecentMessages(-1);

            // Assert
            assertThat(zeroResult).isEmpty();
            assertThat(negativeResult).isEmpty();
        }
    }

    @Nested
    @DisplayName("isEmpty and clearMessages")
    class IsEmptyAndClear {

        @Test
        @DisplayName("should return true when empty")
        void shouldReturnTrueWhenEmpty() {
            // Arrange
            ChatSession session = ChatSession.create("Test");

            // Act & Assert
            assertThat(session.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("should return false when not empty")
        void shouldReturnFalseWhenNotEmpty() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");

            // Act & Assert
            assertThat(session.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("should clear all messages")
        void shouldClearAllMessages() {
            // Arrange
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");
            session.addAssistantMessage("Hi");
            session.addUserMessage("How are you?");

            // Act
            session.clearMessages();

            // Assert
            assertThat(session.getMessages()).isEmpty();
            assertThat(session.getMessageCount()).isZero();
            assertThat(session.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("ChatMessage")
    class ChatMessageTests {

        @Test
        @DisplayName("should create user message with timestamp")
        void shouldCreateUserMessageWithTimestamp() {
            // Arrange
            Instant before = Instant.now();

            // Act
            ChatMessage message = ChatMessage.createUserMessage("Hello");

            // Assert
            Instant after = Instant.now();
            assertThat(message.getTimestamp()).isAfterOrEqualTo(before);
            assertThat(message.getTimestamp()).isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("should create assistant message with timestamp")
        void shouldCreateAssistantMessageWithTimestamp() {
            // Arrange
            Instant before = Instant.now();

            // Act
            ChatMessage message = ChatMessage.createAssistantMessage("Hello");

            // Assert
            Instant after = Instant.now();
            assertThat(message.getTimestamp()).isAfterOrEqualTo(before);
            assertThat(message.getTimestamp()).isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("should create unique message IDs")
        void shouldCreateUniqueMessageIds() {
            // Act
            ChatMessage msg1 = ChatMessage.createUserMessage("Hello");
            ChatMessage msg2 = ChatMessage.createUserMessage("Hello");

            // Assert
            assertThat(msg1.getId()).isNotEqualTo(msg2.getId());
        }

        @Test
        @DisplayName("should update message text")
        void shouldUpdateMessageText() {
            // Arrange
            ChatMessage original = ChatMessage.createUserMessage("Original");

            // Act
            ChatMessage updated = original.withText("Updated");

            // Assert
            assertThat(updated.getText()).isEqualTo("Updated");
            assertThat(updated.getId()).isEqualTo(original.getId());
            assertThat(updated.getRole()).isEqualTo(original.getRole());
        }

        @Test
        @DisplayName("should have correct equals and hashCode")
        void shouldHaveCorrectEqualsAndHashCode() {
            // Arrange
            var id = MessageId.generate();
            Instant timestamp = Instant.now();
            ChatMessage msg1 = ChatMessage.of(id, "Hello", MessageRole.USER, timestamp);
            ChatMessage msg2 = ChatMessage.of(id, "Hello", MessageRole.USER, timestamp);

            // Assert
            assertThat(msg1).isEqualTo(msg2);
            assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
        }
    }
}
