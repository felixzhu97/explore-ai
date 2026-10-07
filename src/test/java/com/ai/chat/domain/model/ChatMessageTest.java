package com.ai.chat.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
  @DisplayName("restore()")
  class Restore {

    @Test
    @DisplayName("should keep every stored field when a message is restored")
    void shouldKeepEveryStoredFieldWhenAMessageIsRestored() {
      MessageId id = MessageId.generate();
      Instant timestamp = Instant.now();

      ChatMessage message = ChatMessage.restore(id, "Test", ChatMessageType.ASSISTANT, timestamp);

      assertThat(message.getId()).isEqualTo(id);
      assertThat(message.getText()).isEqualTo("Test");
      assertThat(message.getMessageType()).isEqualTo(ChatMessageType.ASSISTANT);
      assertThat(message.isFromAssistant()).isTrue();
      assertThat(message.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("should reject a message when its type is missing")
    void shouldRejectAMessageWhenItsTypeIsMissing() {
      assertThatThrownBy(
              () -> ChatMessage.restore(MessageId.generate(), "Text", null, Instant.now()))
          .isInstanceOf(NullPointerException.class);
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
      MessageId id = MessageId.of("11111111-1111-1111-1111-111111111111");
      Instant now = Instant.now();
      ChatMessage msg1 = ChatMessage.restore(id, "Text 1", ChatMessageType.USER, now);
      ChatMessage msg2 = ChatMessage.restore(id, "Text 2", ChatMessageType.ASSISTANT, now);

      assertThat(msg1).isEqualTo(msg2);
      assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when id is different")
    void shouldNotBeEqualWhenIdIsDifferent() {
      Instant now = Instant.now();
      ChatMessage msg1 =
          ChatMessage.restore(
              MessageId.of("11111111-1111-1111-1111-111111111111"),
              "Text",
              ChatMessageType.USER,
              now);
      ChatMessage msg2 =
          ChatMessage.restore(
              MessageId.of("22222222-2222-2222-2222-222222222222"),
              "Text",
              ChatMessageType.USER,
              now);

      assertThat(msg1).isNotEqualTo(msg2);
    }
  }

  @Nested
  @DisplayName("toString()")
  class ToString {

    @Test
    @DisplayName("should contain id, type and timestamp")
    void shouldContainIdTypeAndTimestamp() {
      ChatMessage message = ChatMessage.createUserMessage("Test");

      String str = message.toString();

      assertThat(str).contains("type=USER");
    }
  }
}
