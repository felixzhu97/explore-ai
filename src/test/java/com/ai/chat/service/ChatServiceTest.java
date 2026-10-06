package com.ai.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.chat.domain.exception.ChatSessionNotFoundException;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.chat.domain.vo.SessionTitle;
import com.ai.common.infra.prompt.PromptTemplates;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import com.ai.metrics.service.AiInvocationRecorder;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.retry.support.RetryTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService")
class ChatServiceTest {

  private static final String CLIENT_A = "c:11111111-1111-1111-1111-111111111111";
  private static final String CLIENT_B = "22222222-2222-2222-2222-222222222222";

  @Mock private ChatClientProvider chatClientProvider;

  @Mock private ChatSessionRepository repository;

  @Mock private ChatMemory chatMemory;

  @Mock private ConversationMemoryRepository conversationMemoryRepository;

  @Mock private SessionTitleGenerator sessionTitleGenerator;

  @Mock private ChatWebSourcesRepository chatWebSourcesRepository;

  @Mock private AiInvocationRecorder invocationRecorder;

  @Mock private AiInvocationEventRepository invocationEventRepository;

  private ChatService useCase;
  private RetryTemplate retryTemplate;

  @BeforeEach
  void setUp() {
    retryTemplate = RetryTemplate.builder().maxAttempts(1).build();

    useCase =
        new ChatService(
            chatClientProvider,
            repository,
            retryTemplate,
            chatMemory,
            conversationMemoryRepository,
            sessionTitleGenerator,
            chatWebSourcesRepository,
            new PromptTemplates(),
            invocationRecorder,
            invocationEventRepository);
  }

  @Nested
  @DisplayName("createSession()")
  class CreateSession {

    @Test
    @DisplayName("should create and save session with title and client")
    void shouldCreateAndSaveSessionWhenTitleAndClientProvided() {
      doNothing().when(repository).save(any(ChatSession.class));

      ChatSession result = useCase.createSession("My Chat", CLIENT_A);

      assertThat(result.getTitle()).isEqualTo("My Chat");
      assertThat(result.getOwnerKeyValue()).isEqualTo(CLIENT_A);
      verify(repository).save(any(ChatSession.class));
    }
  }

  @Nested
  @DisplayName("chatWithSession() without session id")
  class ChatInDefaultSession {

    @Test
    @DisplayName("should create the default session with the default title when owner has none")
    void shouldCreateTheDefaultSessionWithTheDefaultTitleWhenOwnerHasNone() {
      when(repository.findByOwnerKey(CLIENT_A)).thenReturn(List.of());
      ArgumentCaptor<ChatSession> saved = ArgumentCaptor.forClass(ChatSession.class);

      catchThrowable(() -> useCase.chatWithSession("Hello", CLIENT_A));

      verify(repository, atLeastOnce()).save(saved.capture());
      assertThat(saved.getAllValues().getFirst().getTitle())
          .isEqualTo(SessionTitle.DEFAULT.value());
    }
  }

  @Nested
  @DisplayName("getSession()")
  class GetSession {

    @Test
    @DisplayName("should return session when owned by client")
    void shouldReturnSessionWhenOwnedByClient() {
      ChatSession session = ChatSession.create("Test", CLIENT_A);
      when(repository.findByIdAndOwnerKey(
              ChatSessionId.of("22222222-2222-2222-2222-222222222222"), CLIENT_A))
          .thenReturn(Optional.of(session));

      Optional<ChatSession> result =
          useCase.getSession("22222222-2222-2222-2222-222222222222", CLIENT_A);

      assertThat(result).isPresent().contains(session);
      verify(conversationMemoryRepository)
          .syncToSession(eq("22222222-2222-2222-2222-222222222222"), eq(session));
    }

    @Test
    @DisplayName("should return empty when owned by another client")
    void shouldReturnEmptyWhenOwnedByAnotherClient() {
      when(repository.findByIdAndOwnerKey(
              ChatSessionId.of("22222222-2222-2222-2222-222222222222"), CLIENT_B))
          .thenReturn(Optional.empty());

      Optional<ChatSession> result =
          useCase.getSession("22222222-2222-2222-2222-222222222222", CLIENT_B);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("findSessionHistory()")
  class GetSessionHistory {

    @Test
    @DisplayName("should return messages for owned session")
    void shouldReturnMessagesWhenSessionOwned() {
      ChatSession session = ChatSession.create("Test", CLIENT_A);
      session.addUserMessage("Hello");
      session.addAssistantMessage("Hi!");
      when(repository.findByIdAndOwnerKey(
              ChatSessionId.of("22222222-2222-2222-2222-222222222222"), CLIENT_A))
          .thenReturn(Optional.of(session));

      List<ChatMessage> history =
          useCase.findSessionHistory("22222222-2222-2222-2222-222222222222", CLIENT_A);

      assertThat(history).hasSize(2);
      verify(conversationMemoryRepository)
          .syncToSession(eq("22222222-2222-2222-2222-222222222222"), eq(session));
    }

    @Test
    @DisplayName("should throw when session not owned")
    void shouldThrowWhenSessionNotOwned() {
      when(repository.findByIdAndOwnerKey(
              ChatSessionId.of("44444444-4444-4444-4444-444444444444"), CLIENT_A))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> useCase.findSessionHistory("44444444-4444-4444-4444-444444444444", CLIENT_A))
          .isInstanceOf(ChatSessionNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("deleteSession()")
  class DeleteSession {

    @Test
    @DisplayName("should delete owned session")
    void shouldDeleteSessionWhenOwned() {
      ChatSession session =
          ChatSession.of(
              ChatSessionId.of("22222222-2222-2222-2222-222222222222"),
              "Test",
              java.time.Instant.now(),
              CLIENT_A);
      when(repository.findByIdAndOwnerKey(
              ChatSessionId.of("22222222-2222-2222-2222-222222222222"), CLIENT_A))
          .thenReturn(Optional.of(session));
      doNothing().when(repository).delete(ChatSessionId.of("22222222-2222-2222-2222-222222222222"));

      useCase.deleteSession("22222222-2222-2222-2222-222222222222", CLIENT_A);

      verify(conversationMemoryRepository).clear("22222222-2222-2222-2222-222222222222");
      verify(chatWebSourcesRepository)
          .deleteByConversationId("22222222-2222-2222-2222-222222222222");
      verify(repository).delete(ChatSessionId.of("22222222-2222-2222-2222-222222222222"));
    }

    @Test
    @DisplayName("should throw when deleting another client's session")
    void shouldThrowWhenDeletingAnotherClientsSession() {
      when(repository.findByIdAndOwnerKey(
              ChatSessionId.of("22222222-2222-2222-2222-222222222222"), CLIENT_B))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> useCase.deleteSession("22222222-2222-2222-2222-222222222222", CLIENT_B))
          .isInstanceOf(ChatSessionNotFoundException.class);
      verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("should erase all sessions for client")
    void shouldEraseAllSessionsWhenClientRequestsPrivacyDelete() {
      ChatSession owned =
          ChatSession.of(
              ChatSessionId.of("22222222-2222-2222-2222-222222222222"),
              "Test",
              java.time.Instant.now(),
              CLIENT_A);
      when(repository.findByOwnerKey(CLIENT_A)).thenReturn(List.of(owned));

      useCase.deleteAllSessions(CLIENT_A);

      verify(conversationMemoryRepository).clear("22222222-2222-2222-2222-222222222222");
      verify(chatWebSourcesRepository)
          .deleteByConversationId("22222222-2222-2222-2222-222222222222");
      verify(repository).delete(ChatSessionId.of("22222222-2222-2222-2222-222222222222"));
      verify(invocationEventRepository)
          .deleteBySessionIds(List.of("22222222-2222-2222-2222-222222222222"));
    }
  }

  @Nested
  @DisplayName("listSessions()")
  class ListSessions {

    @Test
    @DisplayName("should return sessions for client")
    void shouldReturnSessionsWhenClientHasSessions() {
      List<ChatSession> sessions =
          List.of(
              ChatSession.create("Session 1", CLIENT_A), ChatSession.create("Session 2", CLIENT_A));
      when(repository.findByOwnerKey(CLIENT_A)).thenReturn(sessions);

      List<ChatSession> result = useCase.listSessions(CLIENT_A);

      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("should return empty list when client has no sessions")
    void shouldReturnEmptyListWhenClientHasNoSessions() {
      when(repository.findByOwnerKey(CLIENT_A)).thenReturn(List.of());

      List<ChatSession> result = useCase.listSessions(CLIENT_A);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("clearConversationMemory()")
  class ClearConversationMemory {

    @Test
    @DisplayName("should clear chat memory")
    void shouldClearChatMemory() {
      doNothing().when(chatMemory).clear("conversation-123");

      useCase.clearConversationMemory("conversation-123");

      verify(chatMemory).clear("conversation-123");
    }
  }
}
