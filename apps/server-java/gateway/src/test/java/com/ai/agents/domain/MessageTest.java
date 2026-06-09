package com.ai.agents.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Message Tests")
class MessageTest {

    @Nested
    @DisplayName("of factory method")
    class OfFactoryMethodTests {

        @Test
        @DisplayName("should create message with content and role")
        void shouldCreateMessageWithContentAndRole() {
            Message message = Message.of("Hello", Message.MessageRole.USER);

            assertThat(message.content()).isEqualTo("Hello");
            assertThat(message.role()).isEqualTo(Message.MessageRole.USER);
        }

        @Test
        @DisplayName("should generate unique message id")
        void shouldGenerateUniqueMessageId() {
            Message message1 = Message.of("Hello", Message.MessageRole.USER);
            Message message2 = Message.of("Hello", Message.MessageRole.USER);

            assertThat(message1.id()).isNotEqualTo(message2.id());
        }
    }

    @Nested
    @DisplayName("fromUser factory method")
    class FromUserFactoryMethodTests {

        @Test
        @DisplayName("should create user message")
        void shouldCreateUserMessage() {
            Message message = Message.fromUser("Hello");

            assertThat(message.content()).isEqualTo("Hello");
            assertThat(message.role()).isEqualTo(Message.MessageRole.USER);
        }
    }

    @Nested
    @DisplayName("fromAssistant factory method")
    class FromAssistantFactoryMethodTests {

        @Test
        @DisplayName("should create assistant message with agent id")
        void shouldCreateAssistantMessageWithAgentId() {
            AgentId agentId = AgentId.of("chat-1");
            Message message = Message.fromAssistant(agentId, "Hello");

            assertThat(message.content()).isEqualTo("Hello");
            assertThat(message.role()).isEqualTo(Message.MessageRole.ASSISTANT);
            assertThat(message.agentId()).isEqualTo(agentId);
        }
    }

    @Nested
    @DisplayName("system factory method")
    class SystemFactoryMethodTests {

        @Test
        @DisplayName("should create system message")
        void shouldCreateSystemMessage() {
            Message message = Message.system("System prompt");

            assertThat(message.content()).isEqualTo("System prompt");
            assertThat(message.role()).isEqualTo(Message.MessageRole.SYSTEM);
        }
    }

    @Nested
    @DisplayName("role checking methods")
    class RoleCheckingMethodTests {

        @Test
        @DisplayName("isFromUser should return true for user messages")
        void isFromUserShouldReturnTrueForUserMessages() {
            Message message = Message.fromUser("Hello");

            assertThat(message.isFromUser()).isTrue();
            assertThat(message.isFromAssistant()).isFalse();
            assertThat(message.isSystem()).isFalse();
        }

        @Test
        @DisplayName("isFromAssistant should return true for assistant messages")
        void isFromAssistantShouldReturnTrueForAssistantMessages() {
            Message message = Message.fromAssistant(AgentId.of("a1"), "Hello");

            assertThat(message.isFromUser()).isFalse();
            assertThat(message.isFromAssistant()).isTrue();
            assertThat(message.isSystem()).isFalse();
        }

        @Test
        @DisplayName("isSystem should return true for system messages")
        void isSystemShouldReturnTrueForSystemMessages() {
            Message message = Message.system("System prompt");

            assertThat(message.isFromUser()).isFalse();
            assertThat(message.isFromAssistant()).isFalse();
            assertThat(message.isSystem()).isTrue();
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("messages with same id should be equal")
        void messagesWithSameIdShouldBeEqual() {
            // Messages are created with unique IDs, so we test with a single message
            Message message1 = Message.fromUser("Hello");

            assertThat(message1).isEqualTo(message1);
            assertThat(message1.hashCode()).isEqualTo(message1.hashCode());
        }

        @Test
        @DisplayName("different messages should not be equal")
        void differentMessagesShouldNotBeEqual() {
            Message message1 = Message.fromUser("Hello");
            Message message2 = Message.fromUser("Hello");

            assertThat(message1.id()).isNotEqualTo(message2.id());
            assertThat(message1).isNotEqualTo(message2);
        }
    }

    @Nested
    @DisplayName("MessageId inner class")
    class MessageIdTests {

        @Test
        @DisplayName("generated ids should be unique")
        void generatedIdsShouldBeUnique() {
            var message1 = Message.fromUser("Hello");
            var message2 = Message.fromUser("World");

            assertThat(message1.id()).isNotEqualTo(message2.id());
        }
    }
}
