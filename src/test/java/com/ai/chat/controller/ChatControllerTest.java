package com.ai.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.chat.domain.exception.ChatSessionNotFoundException;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.chat.domain.vo.ContentHash;
import com.ai.chat.domain.vo.WebSource;
import com.ai.chat.service.usecase.ChatUseCase;
import com.ai.testsupport.AbstractOwnerScopedControllerTest;
import com.ai.testsupport.ClientIdentityRequestPostProcessor;
import com.ai.testsupport.OwnerKeyFixtures;
import com.ai.testsupport.SliceWebMvcTest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SliceWebMvcTest(controllers = ChatController.class)
@DisplayName("ChatController")
class ChatControllerTest extends AbstractOwnerScopedControllerTest {

  @MockitoBean private ChatUseCase chatUseCase;

  @MockitoBean private ChatWebSourcesRepository chatWebSourcesRepository;

  @Nested
  @DisplayName("GET /api/health")
  class HealthEndpoint {

    @Test
    @DisplayName("should return UP status")
    void shouldReturnUpStatus() {
      assertThat(mvc.get().uri("/api/health"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.status")
          .asString()
          .isEqualTo("UP");
    }
  }

  @Nested
  @DisplayName("POST /api/chat")
  class ChatEndpoint {

    @Test
    @DisplayName("should return response for valid message")
    void shouldReturnResponseForValidMessage() {
      when(chatUseCase.chatWithSession("Hello", ownerClientId())).thenReturn("Hi there!");

      assertThat(
              mvc.post()
                  .uri("/api/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"message\":\"Hello\"}")
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerClientId())))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.response")
          .asString()
          .isEqualTo("Hi there!");
      verify(chatUseCase).chatWithSession("Hello", ownerClientId());
    }

    @Test
    @DisplayName("should return 400 for null message")
    void shouldReturn400ForNullMessage() {
      assertThat(
              mvc.post()
                  .uri("/api/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"message\":null}"))
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyJson()
          .extractingPath("$.response")
          .asString()
          .isEqualTo("Please provide a message.");
    }

    @Test
    @DisplayName("should return 400 for blank message")
    void shouldReturn400ForBlankMessage() {
      assertThat(
              mvc.post()
                  .uri("/api/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"message\":\"   \"}"))
          .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should use session when sessionId provided")
    void shouldUseSessionWhenSessionIdProvided() {
      when(chatUseCase.chatWithSession(
              "22222222-2222-2222-2222-222222222222", "Hello", ownerClientId()))
          .thenReturn("Response with context");

      String sessionId = "22222222-2222-2222-2222-222222222222";
      assertThat(
              mvc.post()
                  .uri("/api/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"message\":\"Hello\",\"sessionId\":\"" + sessionId + "\"}"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.response")
          .asString()
          .isEqualTo("Response with context");
      verify(chatUseCase)
          .chatWithSession("22222222-2222-2222-2222-222222222222", "Hello", ownerClientId());
    }

    @Test
    @DisplayName("should handle long message without error")
    void shouldHandleLongMessageWithoutError() {
      String longMessage = "A".repeat(100);
      when(chatUseCase.chatWithSession(longMessage, ownerClientId()))
          .thenReturn("Response to long message");

      assertThat(
              mvc.post()
                  .uri("/api/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"message\":\"" + longMessage + "\"}"))
          .hasStatusOk();
    }
  }

  @Nested
  @DisplayName("POST /api/sessions")
  class CreateSession {

    @Test
    @DisplayName("should create session with custom title")
    void shouldCreateSessionWithCustomTitle() {
      ChatSession session =
          createTestSession("33333333-3333-3333-3333-333333333333", "Custom Title");
      when(chatUseCase.createSession("Custom Title", ownerClientId())).thenReturn(session);

      assertThat(
              mvc.post()
                  .uri("/api/sessions")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"title\":\"Custom Title\"}"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.title")
          .asString()
          .isEqualTo("Custom Title");
    }

    @Test
    @DisplayName("should create session with default title when not provided")
    void shouldCreateSessionWithDefaultTitleWhenNotProvided() {
      ChatSession session = createTestSession("33333333-3333-3333-3333-333333333333", "New Chat");
      when(chatUseCase.createSession("New Chat", ownerClientId())).thenReturn(session);

      assertThat(
              mvc.post().uri("/api/sessions").contentType(MediaType.APPLICATION_JSON).content("{}"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.title")
          .asString()
          .isEqualTo("New Chat");
    }

    @Test
    @DisplayName("should create session with default title when body is null")
    void shouldCreateSessionWithDefaultTitleWhenBodyIsNull() {
      ChatSession session = createTestSession("33333333-3333-3333-3333-333333333333", "New Chat");
      when(chatUseCase.createSession("New Chat", ownerClientId())).thenReturn(session);

      assertThat(mvc.post().uri("/api/sessions").contentType(MediaType.APPLICATION_JSON))
          .hasStatusOk();
    }
  }

  @Nested
  @DisplayName("GET /api/sessions")
  class GetAllSessions {

    @Test
    @DisplayName("should return client sessions")
    void shouldReturnClientSessions() {
      List<ChatSession> sessions =
          List.of(
              createTestSession("22222222-2222-2222-2222-222222222222", "Chat 1"),
              createTestSession("44444444-4444-4444-4444-444444444444", "Chat 2"));
      when(chatUseCase.getSessionsForClient(ownerClientId())).thenReturn(sessions);

      assertThat(mvc.get().uri("/api/sessions"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$")
          .asArray()
          .hasSize(2);
    }

    @Test
    @DisplayName("should return empty list when no sessions")
    void shouldReturnEmptyListWhenNoSessions() {
      when(chatUseCase.getSessionsForClient(ownerClientId())).thenReturn(List.of());

      assertThat(mvc.get().uri("/api/sessions"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$")
          .asArray()
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("GET /api/sessions/{sessionId}")
  class GetSession {

    @Test
    @DisplayName("should return session when found")
    void shouldReturnSessionWhenFound() {
      ChatSession session = createTestSession("22222222-2222-2222-2222-222222222222", "My Chat");
      when(chatUseCase.getSession("22222222-2222-2222-2222-222222222222", ownerClientId()))
          .thenReturn(Optional.of(session));

      assertThat(mvc.get().uri("/api/sessions/22222222-2222-2222-2222-222222222222"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.title")
          .asString()
          .isEqualTo("My Chat");
    }

    @Test
    @DisplayName("should return 404 when session not found")
    void shouldReturn404WhenSessionNotFound() {
      when(chatUseCase.getSession("missing", ownerClientId())).thenReturn(Optional.empty());

      assertThat(mvc.get().uri("/api/sessions/missing")).hasStatus(HttpStatus.NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("GET /api/sessions/{sessionId}/messages")
  class GetSessionMessages {

    @Test
    @DisplayName("should return messages for session")
    void shouldReturnMessagesForSession() {
      when(chatUseCase.getSessionHistory("22222222-2222-2222-2222-222222222222", ownerClientId()))
          .thenReturn(
              List.of(
                  ChatMessage.createUserMessage("Hello"),
                  ChatMessage.createAssistantMessage("Hi")));
      when(chatWebSourcesRepository.findByConversationId("22222222-2222-2222-2222-222222222222"))
          .thenReturn(Map.of());

      assertThat(mvc.get().uri("/api/sessions/22222222-2222-2222-2222-222222222222/messages"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$")
          .asArray()
          .hasSize(2);

      assertThat(mvc.get().uri("/api/sessions/22222222-2222-2222-2222-222222222222/messages"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$[0].role")
          .asString()
          .isEqualTo("user");
    }

    @Test
    @DisplayName("should attach persisted sources to assistant messages")
    void shouldAttachPersistedSourcesToAssistantMessages() {
      String reply = "Paris is the capital.";
      when(chatUseCase.getSessionHistory("22222222-2222-2222-2222-222222222222", ownerClientId()))
          .thenReturn(
              List.of(
                  ChatMessage.createUserMessage("Where is Paris?"),
                  ChatMessage.createAssistantMessage(reply)));
      when(chatWebSourcesRepository.findByConversationId("22222222-2222-2222-2222-222222222222"))
          .thenReturn(
              Map.of(
                  ContentHash.sha256(reply),
                  List.of(
                      new WebSource("Wiki", "https://en.wikipedia.org/wiki/Paris", "Capital"))));

      assertThat(mvc.get().uri("/api/sessions/22222222-2222-2222-2222-222222222222/messages"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$[1].sources")
          .asArray()
          .hasSize(1);

      assertThat(mvc.get().uri("/api/sessions/22222222-2222-2222-2222-222222222222/messages"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$[1].sources[0].url")
          .asString()
          .isEqualTo("https://en.wikipedia.org/wiki/Paris");
    }

    @Test
    @DisplayName("should return 404 when session not found")
    void shouldReturn404WhenSessionNotFound() {
      when(chatUseCase.getSessionHistory("missing", ownerClientId()))
          .thenThrow(new ChatSessionNotFoundException("missing"));

      assertThat(mvc.get().uri("/api/sessions/missing/messages")).hasStatus(HttpStatus.NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("DELETE /api/sessions/{sessionId}")
  class DeleteSession {

    @Test
    @DisplayName("should delete session and return 204")
    void shouldDeleteSessionAndReturn204() {
      doNothing().when(chatUseCase).deleteSession("session-to-delete", ownerClientId());

      assertThat(mvc.delete().uri("/api/sessions/session-to-delete"))
          .hasStatus(HttpStatus.NO_CONTENT);
      verify(chatUseCase).deleteSession("session-to-delete", ownerClientId());
    }
  }

  private static ChatSession createTestSession(String id, String title) {
    return ChatSession.createWithId(ChatSessionId.of(id), title, OwnerKeyFixtures.CLIENT_FULL_KEY);
  }
}
