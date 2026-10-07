package com.ai.chat.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ChatSession")
class ChatSessionTest {

  @Test
  @DisplayName("should create session with title")
  void shouldCreateSessionWithTitle() {
    ChatSession session = ChatSession.createSession("My Chat", "c:client-a");

    assertThat(session.getTitle()).isEqualTo("My Chat");
    assertThat(session.getId()).isNotNull();
    assertThat(session.getCreatedAt()).isNotNull();
    assertThat(session.isEmpty()).isTrue();
  }

  @Test
  @DisplayName("should add user message")
  void shouldAddUserMessage() {
    ChatSession session = ChatSession.createSession("Test", "c:client-a");

    ChatMessage message = session.addUserMessage("Hello");

    assertThat(message.isFromUser()).isTrue();
    assertThat(message.getText()).isEqualTo("Hello");
    assertThat(session.getMessages()).hasSize(1);
  }

  @Test
  @DisplayName("should move last activity forward when a user message is added")
  void shouldMoveLastActivityForwardWhenAUserMessageIsAdded() {
    Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
    ChatSession session =
        ChatSession.restoreSession(ChatSessionId.generateId(), "Test", createdAt, "c:client-a");

    session.addUserMessage("Hello");

    assertThat(session.getLastActivityAt()).isAfter(createdAt);
  }

  @Test
  @DisplayName("should add assistant message")
  void shouldAddAssistantMessage() {
    ChatSession session = ChatSession.createSession("Test", "c:client-a");

    ChatMessage message = session.addAssistantMessage("Hello");

    assertThat(message.isFromAssistant()).isTrue();
    assertThat(message.getText()).isEqualTo("Hello");
    assertThat(session.getMessages()).hasSize(1);
  }

  @Test
  @DisplayName("should return the message that opened the conversation")
  void shouldReturnTheMessageThatOpenedTheConversation() {
    ChatSession session = ChatSession.createSession("Test", "c:client-a");
    session.addUserMessage("First");
    session.addAssistantMessage("Response");
    session.addUserMessage("Second");

    assertThat(session.findFirstUserMessage()).map(ChatMessage::getText).contains("First");
  }

  @Test
  @DisplayName("should be empty when the user has not written yet")
  void shouldBeEmptyWhenTheUserHasNotWrittenYet() {
    assertThat(ChatSession.createSession("Test", "c:client-a").findFirstUserMessage()).isEmpty();
  }

  @Test
  @DisplayName("should return the newest assistant reply")
  void shouldReturnTheNewestAssistantReply() {
    ChatSession session = ChatSession.createSession("Test", "c:client-a");
    session.addUserMessage("Question");
    session.addAssistantMessage("First Response");
    session.addAssistantMessage("Second Response");

    assertThat(session.findLastAssistantMessage())
        .map(ChatMessage::getText)
        .contains("Second Response");
  }

  @Test
  @DisplayName("should be empty when the assistant has not replied yet")
  void shouldBeEmptyWhenTheAssistantHasNotRepliedYet() {
    assertThat(ChatSession.createSession("Test", "c:client-a").findLastAssistantMessage())
        .isEmpty();
  }

  @Test
  @DisplayName("should return false for session with messages")
  void shouldReturnFalseForSessionWithMessages() {
    ChatSession session = ChatSession.createSession("Test", "c:client-a");
    session.addUserMessage("Hello");

    assertThat(session.isEmpty()).isFalse();
  }

  @Nested
  @DisplayName("restoreMessages() and recordExchange()")
  class RestoreMessages {

    private final Instant lastActive = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("should keep last activity when stored messages are restored")
    void shouldKeepLastActivityWhenStoredMessagesAreRestored() {
      ChatSession session =
          ChatSession.restoreSession(
              com.ai.chat.domain.model.ChatSessionId.generateId(),
              "Test",
              lastActive,
              "c:client-a");

      session.restoreMessages(List.of(ChatMessage.createUserMessage("Hello")));

      assertThat(session.getMessageCount()).isEqualTo(1);
      assertThat(session.getLastActivityAt()).isEqualTo(lastActive);
    }

    @Test
    @DisplayName("should move last activity forward when an exchange is recorded")
    void shouldMoveLastActivityForwardWhenAnExchangeIsRecorded() {
      ChatSession session =
          ChatSession.restoreSession(
              com.ai.chat.domain.model.ChatSessionId.generateId(),
              "Test",
              lastActive,
              "c:client-a");

      session.recordExchange(
          List.of(ChatMessage.createUserMessage("Hi"), ChatMessage.createAssistantMessage("Yo")));

      assertThat(session.getMessageCount()).isEqualTo(2);
      assertThat(session.getLastActivityAt()).isAfter(lastActive);
    }

    @Test
    @DisplayName("should be inactive only when last activity is before the cutoff")
    void shouldBeInactiveOnlyWhenLastActivityIsBeforeTheCutoff() {
      ChatSession session =
          ChatSession.restoreSession(
              com.ai.chat.domain.model.ChatSessionId.generateId(),
              "Test",
              lastActive,
              "c:client-a");

      assertThat(session.isInactiveSince(lastActive.plusSeconds(1))).isTrue();
      assertThat(session.isInactiveSince(lastActive)).isFalse();
    }
  }

  @Test
  @DisplayName("should return unmodifiable list")
  void shouldReturnUnmodifiableList() {
    ChatSession session = ChatSession.createSession("Test", "c:client-a");
    session.addUserMessage("Hello");

    assertThatThrownBy(() -> session.getMessages().add(ChatMessage.createUserMessage("New")))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("should be equal when id is same")
  void shouldBeEqualWhenIdIsSame() {
    var id = com.ai.chat.domain.model.ChatSessionId.parseId("11111111-1111-1111-1111-111111111111");
    ChatSession session1 = ChatSession.restoreSession(id, "Title 1", Instant.now(), "c:client-a");
    ChatSession session2 = ChatSession.restoreSession(id, "Title 2", Instant.now(), "c:client-a");

    assertThat(session1).isEqualTo(session2);
    assertThat(session1.hashCode()).isEqualTo(session2.hashCode());
  }

  @Test
  @DisplayName("should not be equal when id is different")
  void shouldNotBeEqualWhenIdIsDifferent() {
    ChatSession session1 =
        ChatSession.restoreSession(
            com.ai.chat.domain.model.ChatSessionId.parseId("11111111-1111-1111-1111-111111111111"),
            "Title",
            Instant.now(),
            "c:client-a");
    ChatSession session2 =
        ChatSession.restoreSession(
            com.ai.chat.domain.model.ChatSessionId.parseId("22222222-2222-2222-2222-222222222222"),
            "Title",
            Instant.now(),
            "c:client-a");

    assertThat(session1).isNotEqualTo(session2);
  }

  @Test
  @DisplayName("should rename session with valid title")
  void shouldRenameSessionWithValidTitle() {
    ChatSession session = ChatSession.createSession("New Chat", "c:client-a");

    session.rename("Kubernetes Guide");

    assertThat(session.getTitle()).isEqualTo("Kubernetes Guide");
  }

  @Test
  @DisplayName("should ignore blank rename")
  void shouldIgnoreBlankRename() {
    ChatSession session = ChatSession.createSession("New Chat", "c:client-a");

    session.rename("   ");

    assertThat(session.getTitle()).isEqualTo("New Chat");
  }

  @Nested
  @DisplayName("generated title")
  class GeneratedTitle {

    @Test
    @DisplayName("should need a generated title when an untitled session has its first exchange")
    void shouldNeedAGeneratedTitleWhenAnUntitledSessionHasItsFirstExchange() {
      ChatSession session = ChatSession.createDefaultSession("c:client-a");
      assertThat(session.needsGeneratedTitle()).isFalse();

      session.addUserMessage("How do I deploy K8s?");
      session.addAssistantMessage("Use kubectl apply.");

      assertThat(session.needsGeneratedTitle()).isTrue();
    }

    @Test
    @DisplayName("should not need a generated title when the user named the session")
    void shouldNotNeedAGeneratedTitleWhenTheUserNamedTheSession() {
      ChatSession session = ChatSession.createSession("Custom", "c:client-a");
      session.addUserMessage("Hi");
      session.addAssistantMessage("Hello");

      assertThat(session.needsGeneratedTitle()).isFalse();
    }

    @Test
    @DisplayName("should apply a generated title without recording activity")
    void shouldApplyAGeneratedTitleWithoutRecordingActivity() {
      Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
      ChatSession session =
          ChatSession.restoreSession(ChatSessionId.generateId(), null, createdAt, "c:client-a");

      boolean applied =
          session.applyGeneratedTitle(SessionTitle.createGeneratedTitle("\"K8s deploy\""));

      assertThat(applied).isTrue();
      assertThat(session.getTitle()).isEqualTo("K8s deploy");
      assertThat(session.getLastActivityAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("should keep the user's title when a generated title arrives late")
    void shouldKeepTheUsersTitleWhenAGeneratedTitleArrivesLate() {
      ChatSession session = ChatSession.createDefaultSession("c:client-a");
      session.rename("Mine");

      assertThat(session.applyGeneratedTitle(SessionTitle.createGeneratedTitle("Theirs")))
          .isFalse();
      assertThat(session.getTitle()).isEqualTo("Mine");
    }
  }
}
