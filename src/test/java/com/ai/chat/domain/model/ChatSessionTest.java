package com.ai.chat.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ChatSession")
class ChatSessionTest {

  @Nested
  @DisplayName("create()")
  class Create {

    @Test
    @DisplayName("should create session with title")
    void shouldCreateSessionWithTitle() {
      ChatSession session = ChatSession.create("My Chat", "c:client-a");

      assertThat(session.getTitle()).isEqualTo("My Chat");
      assertThat(session.getId()).isNotNull();
      assertThat(session.getCreatedAt()).isNotNull();
      assertThat(session.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("should create session with null title")
    void shouldCreateSessionWithNullTitle() {
      ChatSession session = ChatSession.create(null, "c:client-a");

      assertThat(session.getTitle()).isEqualTo("New Chat");
    }

    @Test
    @DisplayName("should create session with blank title")
    void shouldCreateSessionWithBlankTitle() {
      ChatSession session = ChatSession.create("   ", "c:client-a");

      assertThat(session.getTitle()).isEqualTo("New Chat");
    }

    @Test
    @DisplayName("should truncate long title")
    void shouldTruncateLongTitle() {
      String longTitle = "A".repeat(150);
      ChatSession session = ChatSession.create(longTitle, "c:client-a");

      assertThat(session.getTitle()).hasSize(100);
      assertThat(session.getTitle()).isEqualTo(longTitle.substring(0, 100));
    }

    @Test
    @DisplayName("should trim title")
    void shouldTrimTitle() {
      ChatSession session = ChatSession.create("  My Chat  ", "c:client-a");

      assertThat(session.getTitle()).isEqualTo("My Chat");
    }
  }

  @Nested
  @DisplayName("of()")
  class Of {

    @Test
    @DisplayName("should create session with id and title")
    void shouldCreateSessionWithIdAndTitle() {
      var id = com.ai.chat.domain.vo.ChatSessionId.of("11111111-1111-1111-1111-111111111111");
      Instant createdAt = Instant.now();

      ChatSession session = ChatSession.of(id, "Title", createdAt, "c:client-a");

      assertThat(session.getId()).isEqualTo(id);
      assertThat(session.getTitle()).isEqualTo("Title");
      assertThat(session.getCreatedAt()).isEqualTo(createdAt);
    }
  }

  @Nested
  @DisplayName("reconstitute()")
  class Reconstitute {

    @Test
    @DisplayName("should reconstitute session with messages")
    void shouldReconstituteSessionWithMessages() {
      var id = com.ai.chat.domain.vo.ChatSessionId.of("11111111-1111-1111-1111-111111111111");
      Instant createdAt = Instant.now().minusSeconds(3600);
      Instant lastActivity = Instant.now();

      var messages =
          java.util.List.of(
              ChatMessage.createUserMessage("Hello"), ChatMessage.createAssistantMessage("Hi!"));

      ChatSession session =
          ChatSession.reconstitute(id, "Title", createdAt, lastActivity, messages, "c:client-a");

      assertThat(session.getId()).isEqualTo(id);
      assertThat(session.getLastActivityAt()).isEqualTo(lastActivity);
      assertThat(session.getMessages()).hasSize(2);
    }

    @Test
    @DisplayName("should handle null lastActivityAt")
    void shouldHandleNullLastActivityAt() {
      var id = com.ai.chat.domain.vo.ChatSessionId.of("11111111-1111-1111-1111-111111111111");
      Instant createdAt = Instant.now();

      ChatSession session =
          ChatSession.reconstitute(id, "Title", createdAt, null, java.util.List.of(), "c:client-a");

      assertThat(session.getLastActivityAt()).isEqualTo(createdAt);
    }
  }

  @Nested
  @DisplayName("addUserMessage()")
  class AddUserMessage {

    @Test
    @DisplayName("should add user message")
    void shouldAddUserMessage() {
      ChatSession session = ChatSession.create("Test", "c:client-a");

      ChatMessage message = session.addUserMessage("Hello");

      assertThat(message.isFromUser()).isTrue();
      assertThat(message.getText()).isEqualTo("Hello");
      assertThat(session.getMessages()).hasSize(1);
      assertThat(session.getUserMessageCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("should update lastActivityAt")
    void shouldUpdateLastActivityAt() throws InterruptedException {
      ChatSession session = ChatSession.create("Test", "c:client-a");
      Instant beforeAdd = session.getLastActivityAt();

      Thread.sleep(10);
      session.addUserMessage("Hello");

      assertThat(session.getLastActivityAt()).isAfter(beforeAdd);
    }
  }

  @Nested
  @DisplayName("addAssistantMessage()")
  class AddAssistantMessage {

    @Test
    @DisplayName("should add assistant message")
    void shouldAddAssistantMessage() {
      ChatSession session = ChatSession.create("Test", "c:client-a");

      ChatMessage message = session.addAssistantMessage("Hello");

      assertThat(message.isFromAssistant()).isTrue();
      assertThat(message.getText()).isEqualTo("Hello");
      assertThat(session.getMessages()).hasSize(1);
      assertThat(session.getAssistantMessageCount()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("getLastUserMessage()")
  class GetLastUserMessage {

    @Test
    @DisplayName("should return last user message")
    void shouldReturnLastUserMessage() {
      ChatSession session = ChatSession.create("Test", "c:client-a");
      session.addUserMessage("First");
      session.addAssistantMessage("Response");
      session.addUserMessage("Second");

      ChatMessage lastUser = session.getLastUserMessage();

      assertThat(lastUser.getText()).isEqualTo("Second");
    }

    @Test
    @DisplayName("should return null when no user messages")
    void shouldReturnNullWhenNoUserMessages() {
      ChatSession session = ChatSession.create("Test", "c:client-a");

      ChatMessage lastUser = session.getLastUserMessage();

      assertThat(lastUser).isNull();
    }
  }

  @Nested
  @DisplayName("getLastAssistantMessage()")
  class GetLastAssistantMessage {

    @Test
    @DisplayName("should return last assistant message")
    void shouldReturnLastAssistantMessage() {
      ChatSession session = ChatSession.create("Test", "c:client-a");
      session.addUserMessage("Question");
      session.addAssistantMessage("First Response");
      session.addAssistantMessage("Second Response");

      ChatMessage lastAssistant = session.getLastAssistantMessage();

      assertThat(lastAssistant.getText()).isEqualTo("Second Response");
    }

    @Test
    @DisplayName("should return null when no assistant messages")
    void shouldReturnNullWhenNoAssistantMessages() {
      ChatSession session = ChatSession.create("Test", "c:client-a");

      ChatMessage lastAssistant = session.getLastAssistantMessage();

      assertThat(lastAssistant).isNull();
    }
  }

  @Nested
  @DisplayName("getRecentMessages()")
  class GetRecentMessages {

    @Test
    @DisplayName("should return recent messages")
    void shouldReturnRecentMessages() {
      ChatSession session = ChatSession.create("Test", "c:client-a");
      for (int i = 1; i <= 5; i++) {
        session.addUserMessage("Message " + i);
      }

      var recent = session.getRecentMessages(3);

      assertThat(recent).hasSize(3);
      assertThat(recent.get(0).getText()).isEqualTo("Message 3");
      assertThat(recent.get(2).getText()).isEqualTo("Message 5");
    }

    @Test
    @DisplayName("should return all messages when count exceeds size")
    void shouldReturnAllMessagesWhenCountExceedsSize() {
      ChatSession session = ChatSession.create("Test", "c:client-a");
      session.addUserMessage("One");
      session.addUserMessage("Two");

      var recent = session.getRecentMessages(10);

      assertThat(recent).hasSize(2);
    }

    @Test
    @DisplayName("should return empty list for zero count")
    void shouldReturnEmptyListForZeroCount() {
      ChatSession session = ChatSession.create("Test", "c:client-a");
      session.addUserMessage("Hello");

      var recent = session.getRecentMessages(0);

      assertThat(recent).isEmpty();
    }

    @Test
    @DisplayName("should return empty list for negative count")
    void shouldReturnEmptyListForNegativeCount() {
      ChatSession session = ChatSession.create("Test", "c:client-a");

      var recent = session.getRecentMessages(-1);

      assertThat(recent).isEmpty();
    }

    @Test
    @DisplayName("should return unmodifiable list")
    void shouldReturnUnmodifiableList() {
      ChatSession session = ChatSession.create("Test", "c:client-a");
      session.addUserMessage("Hello");

      var recent = session.getRecentMessages(10);

      assertThatThrownBy(() -> recent.add(ChatMessage.createUserMessage("New")))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  @DisplayName("isEmpty()")
  class IsEmpty {

    @Test
    @DisplayName("should return true for empty session")
    void shouldReturnTrueForEmptySession() {
      ChatSession session = ChatSession.create("Test", "c:client-a");

      assertThat(session.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("should return false for session with messages")
    void shouldReturnFalseForSessionWithMessages() {
      ChatSession session = ChatSession.create("Test", "c:client-a");
      session.addUserMessage("Hello");

      assertThat(session.isEmpty()).isFalse();
    }
  }

  @Nested
  @DisplayName("clearMessages()")
  class ClearMessages {

    @Test
    @DisplayName("should clear all messages")
    void shouldClearAllMessages() {
      ChatSession session = ChatSession.create("Test", "c:client-a");
      session.addUserMessage("Hello");
      session.addAssistantMessage("Hi");

      session.clearMessages();

      assertThat(session.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("should update lastActivityAt")
    void shouldUpdateLastActivityAt() throws InterruptedException {
      ChatSession session = ChatSession.create("Test", "c:client-a");
      session.addUserMessage("Hello");
      Instant beforeClear = session.getLastActivityAt();

      Thread.sleep(10);
      session.clearMessages();

      assertThat(session.getLastActivityAt()).isAfter(beforeClear);
    }
  }

  @Nested
  @DisplayName("getMessages()")
  class GetMessages {

    @Test
    @DisplayName("should return unmodifiable list")
    void shouldReturnUnmodifiableList() {
      ChatSession session = ChatSession.create("Test", "c:client-a");
      session.addUserMessage("Hello");

      assertThatThrownBy(() -> session.getMessages().add(ChatMessage.createUserMessage("New")))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  @DisplayName("equals() and hashCode()")
  class Identity {

    @Test
    @DisplayName("should be equal when id is same")
    void shouldBeEqualWhenIdIsSame() {
      var id = com.ai.chat.domain.vo.ChatSessionId.of("11111111-1111-1111-1111-111111111111");
      ChatSession session1 = ChatSession.of(id, "Title 1", Instant.now(), "c:client-a");
      ChatSession session2 = ChatSession.of(id, "Title 2", Instant.now(), "c:client-a");

      assertThat(session1).isEqualTo(session2);
      assertThat(session1.hashCode()).isEqualTo(session2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when id is different")
    void shouldNotBeEqualWhenIdIsDifferent() {
      ChatSession session1 =
          ChatSession.of(
              com.ai.chat.domain.vo.ChatSessionId.of("11111111-1111-1111-1111-111111111111"),
              "Title",
              Instant.now(),
              "c:client-a");
      ChatSession session2 =
          ChatSession.of(
              com.ai.chat.domain.vo.ChatSessionId.of("22222222-2222-2222-2222-222222222222"),
              "Title",
              Instant.now(),
              "c:client-a");

      assertThat(session1).isNotEqualTo(session2);
    }
  }

  @Nested
  @DisplayName("rename()")
  class Rename {

    @Test
    @DisplayName("should rename session with valid title")
    void shouldRenameSessionWithValidTitle() {
      ChatSession session = ChatSession.create("New Chat", "c:client-a");

      session.rename("Kubernetes Guide");

      assertThat(session.getTitle()).isEqualTo("Kubernetes Guide");
    }

    @Test
    @DisplayName("should ignore blank rename")
    void shouldIgnoreBlankRename() {
      ChatSession session = ChatSession.create("New Chat", "c:client-a");

      session.rename("   ");

      assertThat(session.getTitle()).isEqualTo("New Chat");
    }

    @Test
    @DisplayName("should truncate long rename")
    void shouldTruncateLongRename() {
      ChatSession session = ChatSession.create("New Chat", "c:client-a");
      String longTitle = "A".repeat(150);

      session.rename(longTitle);

      assertThat(session.getTitle()).hasSize(100);
    }
  }

  @Nested
  @DisplayName("hasDefaultTitle()")
  class HasDefaultTitle {

    @Test
    @DisplayName("should return true for default title")
    void shouldReturnTrueForDefaultTitle() {
      ChatSession session = ChatSession.create(null, "c:client-a");

      assertThat(session.hasDefaultTitle()).isTrue();
    }

    @Test
    @DisplayName("should return false for custom title")
    void shouldReturnFalseForCustomTitle() {
      ChatSession session = ChatSession.create("Custom", "c:client-a");

      assertThat(session.hasDefaultTitle()).isFalse();
    }
  }

  @Nested
  @DisplayName("toString()")
  class ToString {

    @Test
    @DisplayName("should contain id, title and message count")
    void shouldContainIdTitleAndMessageCount() {
      ChatSession session = ChatSession.create("Test Session", "c:client-a");
      session.addUserMessage("Hello");

      String str = session.toString();

      assertThat(str).contains("Test Session");
      assertThat(str).contains("messageCount=1");
    }
  }
}
