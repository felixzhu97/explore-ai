package com.ai.agents.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Conversation Tests")
class ConversationTest {

    @Nested
    @DisplayName("start factory method")
    class StartFactoryMethodTests {

        @Test
        @DisplayName("should create conversation with initial message")
        void shouldCreateConversationWithInitialMessage() {
            AgentId agentId = AgentId.of("chat-1");
            Message message = Message.fromUser("Hello");

            Conversation conversation = Conversation.start(agentId, message);

            assertThat(conversation.primaryAgentId()).isEqualTo(agentId);
            assertThat(conversation.messages()).hasSize(1);
            assertThat(conversation.messages().get(0)).isEqualTo(message);
        }

        @Test
        @DisplayName("should throw exception when initial message is null")
        void shouldThrowExceptionWhenInitialMessageIsNull() {
            AgentId agentId = AgentId.of("chat-1");

            assertThatThrownBy(() -> Conversation.start(agentId, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Initial message cannot be null");
        }

        @Test
        @DisplayName("should generate unique conversation id")
        void shouldGenerateUniqueConversationId() {
            AgentId agentId = AgentId.of("chat-1");
            Message message = Message.fromUser("Hello");

            Conversation conversation1 = Conversation.start(agentId, message);
            Conversation conversation2 = Conversation.start(agentId, message);

            assertThat(conversation1.id()).isNotEqualTo(conversation2.id());
        }

        @Test
        @DisplayName("should initialize with empty metadata")
        void shouldInitializeWithEmptyMetadata() {
            AgentId agentId = AgentId.of("chat-1");
            Message message = Message.fromUser("Hello");

            Conversation conversation = Conversation.start(agentId, message);

            assertThat(conversation.getMetadata()).isEmpty();
        }
    }

    @Nested
    @DisplayName("create factory method")
    class CreateFactoryMethodTests {

        @Test
        @DisplayName("should create empty conversation")
        void shouldCreateEmptyConversation() {
            AgentId agentId = AgentId.of("chat-1");

            Conversation conversation = Conversation.create(agentId);

            assertThat(conversation.primaryAgentId()).isEqualTo(agentId);
            assertThat(conversation.messages()).isEmpty();
            assertThat(conversation.getMetadata()).isEmpty();
        }
    }

    @Nested
    @DisplayName("addMessage method")
    class AddMessageMethodTests {

        @Test
        @DisplayName("should add message to conversation")
        void shouldAddMessageToConversation() {
            AgentId agentId = AgentId.of("chat-1");
            Conversation conversation = Conversation.create(agentId);
            Message message = Message.fromUser("Hello");

            Conversation updatedConversation = conversation.addMessage(message);

            assertThat(updatedConversation.messages()).hasSize(1);
            assertThat(updatedConversation.messages().get(0)).isEqualTo(message);
        }

        @Test
        @DisplayName("should throw exception when adding null message")
        void shouldThrowExceptionWhenAddingNullMessage() {
            AgentId agentId = AgentId.of("chat-1");
            Conversation conversation = Conversation.create(agentId);

            assertThatThrownBy(() -> conversation.addMessage(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Message cannot be null");
        }

        @Test
        @DisplayName("should not modify original conversation when adding message")
        void shouldNotModifyOriginalConversationWhenAddingMessage() {
            AgentId agentId = AgentId.of("chat-1");
            Conversation conversation = Conversation.create(agentId);
            Message message = Message.fromUser("Hello");

            conversation.addMessage(message);

            assertThat(conversation.messages()).isEmpty();
        }

        @Test
        @DisplayName("should add multiple messages sequentially")
        void shouldAddMultipleMessagesSequentially() {
            AgentId agentId = AgentId.of("chat-1");
            Conversation conversation = Conversation.create(agentId);
            Message message1 = Message.fromUser("Hello");
            Message message2 = Message.fromAssistant(agentId, "Hi!");

            Conversation updated1 = conversation.addMessage(message1);
            Conversation updated2 = updated1.addMessage(message2);

            assertThat(updated2.messages()).hasSize(2);
            assertThat(updated2.messages().get(0)).isEqualTo(message1);
            assertThat(updated2.messages().get(1)).isEqualTo(message2);
        }
    }

    @Nested
    @DisplayName("canContinue method")
    class CanContinueMethodTests {

        @Test
        @DisplayName("should return true when messages are less than 100")
        void shouldReturnTrueWhenMessagesAreLessThan100() {
            AgentId agentId = AgentId.of("chat-1");
            Conversation conversation = Conversation.create(agentId);

            assertThat(conversation.canContinue()).isTrue();
        }

        @Test
        @DisplayName("should return true when messages reach 99")
        void shouldReturnTrueWhenMessagesReach99() {
            AgentId agentId = AgentId.of("chat-1");
            Conversation conversation = Conversation.create(agentId);

            for (int i = 0; i < 99; i++) {
                conversation = conversation.addMessage(Message.fromUser("Message " + i));
            }

            assertThat(conversation.canContinue()).isTrue();
        }
    }

    @Nested
    @DisplayName("getContext method")
    class GetContextMethodTests {

        @Test
        @DisplayName("should return unmodifiable list of messages")
        void shouldReturnUnmodifiableListOfMessages() {
            AgentId agentId = AgentId.of("chat-1");
            Conversation conversation = Conversation.create(agentId);
            Message message = Message.fromUser("Hello");
            conversation = conversation.addMessage(message);

            assertThat(conversation.getContext()).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("getMetadata method")
    class GetMetadataMethodTests {

        @Test
        @DisplayName("should return unmodifiable map of metadata")
        void shouldReturnUnmodifiableMapOfMetadata() {
            AgentId agentId = AgentId.of("chat-1");
            Conversation conversation = Conversation.create(agentId);

            assertThat(conversation.getMetadata()).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("withMetadata method")
    class WithMetadataMethodTests {

        @Test
        @DisplayName("should add metadata to conversation")
        void shouldAddMetadataToConversation() {
            AgentId agentId = AgentId.of("chat-1");
            Conversation conversation = Conversation.create(agentId);

            Conversation updatedConversation = conversation.withMetadata("key", "value");

            assertThat(updatedConversation.getMetadata()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("should not modify original conversation")
        void shouldNotModifyOriginalConversation() {
            AgentId agentId = AgentId.of("chat-1");
            Conversation conversation = Conversation.create(agentId);

            conversation.withMetadata("key", "value");

            assertThat(conversation.getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should update existing metadata key")
        void shouldUpdateExistingMetadataKey() {
            AgentId agentId = AgentId.of("chat-1");
            Conversation conversation = Conversation.create(agentId)
                    .withMetadata("key", "oldValue");

            Conversation updatedConversation = conversation.withMetadata("key", "newValue");

            assertThat(updatedConversation.getMetadata()).containsEntry("key", "newValue");
        }
    }

    @Nested
    @DisplayName("messageCount method")
    class MessageCountMethodTests {

        @Test
        @DisplayName("should return zero for empty conversation")
        void shouldReturnZeroForEmptyConversation() {
            AgentId agentId = AgentId.of("chat-1");
            Conversation conversation = Conversation.create(agentId);

            assertThat(conversation.messageCount()).isZero();
        }

        @Test
        @DisplayName("should return correct count after adding messages")
        void shouldReturnCorrectCountAfterAddingMessages() {
            AgentId agentId = AgentId.of("chat-1");
            Conversation conversation = Conversation.create(agentId)
                    .addMessage(Message.fromUser("Hello"))
                    .addMessage(Message.fromUser("World"));

            assertThat(conversation.messageCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("timestamps")
    class TimestampsTests {

        @Test
        @DisplayName("should set createdAt and lastUpdatedAt")
        void shouldSetCreatedAtAndLastUpdatedAt() {
            AgentId agentId = AgentId.of("chat-1");

            Conversation conversation = Conversation.create(agentId);

            assertThat(conversation.createdAt()).isNotNull();
            assertThat(conversation.lastUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("ConversationId inner class")
    class ConversationIdTests {

        @Test
        @DisplayName("should generate unique conversation ids")
        void shouldGenerateUniqueConversationIds() {
            var id1 = Conversation.create(AgentId.of("c1")).id();
            var id2 = Conversation.create(AgentId.of("c1")).id();

            assertThat(id1).isNotEqualTo(id2);
        }
    }
}
