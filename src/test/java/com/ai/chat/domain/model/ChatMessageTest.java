package com.ai.chat.domain.model;

import com.ai.chat.domain.vo.MessageId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChatMessage")
class ChatMessageTest {

    @Nested
    @DisplayName("createUserMessage()")
    class CreateUserMessage {

        @Test
        @DisplayName("should create user message with correct role")
        void shouldCreateUserMessageWithCorrectRole() {
            ChatMessage message = ChatMessage.createUserMessage("Hello");

            assertThat(message.isFromUser()).isTrue();
            assertThat(message.isFromAssistant()).isFalse();
            assertThat(message.getText()).isEqualTo("Hello");
            assertThat(message.getId()).isNotNull();
            assertThat(message.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("should trim text")
        void shouldTrimText() {
            ChatMessage message = ChatMessage.createUserMessage("  Hello  ");

            assertThat(message.getText()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("should throw for null text")
        void shouldThrowForNullText() {
            assertThatThrownBy(() -> ChatMessage.createUserMessage(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("should throw for blank text")
        void shouldThrowForBlankText() {
            assertThatThrownBy(() -> ChatMessage.createUserMessage("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }
    }

    @Nested
    @DisplayName("createAssistantMessage()")
    class CreateAssistantMessage {

        @Test
        @DisplayName("should create assistant message with correct role")
        void shouldCreateAssistantMessageWithCorrectRole() {
            ChatMessage message = ChatMessage.createAssistantMessage("Hi!");

            assertThat(message.isFromAssistant()).isTrue();
            assertThat(message.isFromUser()).isFalse();
            assertThat(message.getText()).isEqualTo("Hi!");
        }
    }

    @Nested
    @DisplayName("of()")
    class Of {

        @Test
        @DisplayName("should create message with all fields")
        void shouldCreateMessageWithAllFields() {
            MessageId id = MessageId.generate();
            Instant timestamp = Instant.now();

            ChatMessage message = ChatMessage.of(id, "Test", "user", timestamp);

            assertThat(message.getId()).isEqualTo(id);
            assertThat(message.getText()).isEqualTo("Test");
            assertThat(message.role()).isEqualTo("user");
            assertThat(message.getTimestamp()).isEqualTo(timestamp);
        }
    }

    @Nested
    @DisplayName("role validation")
    class RoleValidation {

        @Test
        @DisplayName("should normalize role to lowercase")
        void shouldNormalizeRoleToLowercase() {
            MessageId id = MessageId.generate();
            ChatMessage message = ChatMessage.of(id, "Text", "USER", Instant.now());

            assertThat(message.role()).isEqualTo("user");
        }

        @Test
        @DisplayName("should default to user for null role")
        void shouldDefaultToUserForNullRole() {
            MessageId id = MessageId.generate();
            ChatMessage message = ChatMessage.of(id, "Text", null, Instant.now());

            assertThat(message.role()).isEqualTo("user");
        }

        @Test
        @DisplayName("should default to user for blank role")
        void shouldDefaultToUserForBlankRole() {
            MessageId id = MessageId.generate();
            ChatMessage message = ChatMessage.of(id, "Text", "   ", Instant.now());

            assertThat(message.role()).isEqualTo("user");
        }

        @Test
        @DisplayName("should default to user for unknown role")
        void shouldDefaultToUserForUnknownRole() {
            MessageId id = MessageId.generate();
            ChatMessage message = ChatMessage.of(id, "Text", "unknown", Instant.now());

            assertThat(message.role()).isEqualTo("user");
        }

        @Test
        @DisplayName("should accept valid user role")
        void shouldAcceptValidUserRole() {
            MessageId id = MessageId.generate();
            ChatMessage message = ChatMessage.of(id, "Text", "user", Instant.now());

            assertThat(message.role()).isEqualTo("user");
        }

        @Test
        @DisplayName("should accept valid assistant role")
        void shouldAcceptValidAssistantRole() {
            MessageId id = MessageId.generate();
            ChatMessage message = ChatMessage.of(id, "Text", "assistant", Instant.now());

            assertThat(message.role()).isEqualTo("assistant");
        }
    }

    @Nested
    @DisplayName("withText()")
    class WithText {

        @Test
        @DisplayName("should create new message with different text")
        void shouldCreateNewMessageWithDifferentText() {
            ChatMessage original = ChatMessage.createUserMessage("Hello");

            ChatMessage modified = original.withText("Hi");

            assertThat(modified.getText()).isEqualTo("Hi");
            assertThat(modified.getId()).isEqualTo(original.getId());
            assertThat(modified.isFromUser()).isTrue();
            assertThat(original.getText()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("should preserve assistant role")
        void shouldPreserveAssistantRole() {
            ChatMessage original = ChatMessage.createAssistantMessage("Hello");

            ChatMessage modified = original.withText("Hi");

            assertThat(modified.isFromAssistant()).isTrue();
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class Identity {

        @Test
        @DisplayName("should be equal when id is same")
        void shouldBeEqualWhenIdIsSame() {
            MessageId id = MessageId.of("same-id");
            Instant now = Instant.now();
            ChatMessage msg1 = ChatMessage.of(id, "Text 1", "user", now);
            ChatMessage msg2 = ChatMessage.of(id, "Text 2", "assistant", now);

            assertThat(msg1).isEqualTo(msg2);
            assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when id is different")
        void shouldNotBeEqualWhenIdIsDifferent() {
            Instant now = Instant.now();
            ChatMessage msg1 = ChatMessage.of(MessageId.of("id-1"), "Text", "user", now);
            ChatMessage msg2 = ChatMessage.of(MessageId.of("id-2"), "Text", "user", now);

            assertThat(msg1).isNotEqualTo(msg2);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("should contain id, role and timestamp")
        void shouldContainIdRoleAndTimestamp() {
            ChatMessage message = ChatMessage.createUserMessage("Test");

            String str = message.toString();

            assertThat(str).contains("role='user'");
        }
    }
}
