package com.ai.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.account.controller.OwnerContext;
import com.ai.chat.controller.dto.ChatRequest;
import com.ai.chat.controller.dto.CreateSessionRequest;
import com.ai.chat.domain.exception.ChatSessionNotFoundException;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.vo.ContentHash;
import com.ai.chat.domain.vo.WebSource;
import com.ai.chat.service.usecase.ChatUseCase;
import com.ai.common.controller.ClientIdentity;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController")
class ChatControllerTest {
  private OwnerContext ownerContext;

  private static final String CLIENT_ID = "c:11111111-1111-1111-1111-111111111111";

  @Mock private ChatUseCase chatUseCase;

  @Mock private ChatWebSourcesRepository chatWebSourcesRepository;

  @Mock private HttpServletRequest httpRequest;

  private ChatController controller;

  @BeforeEach
  void setUp() {
    ownerContext = mock(OwnerContext.class);
    lenient().when(ownerContext.requireValue(any())).thenReturn(CLIENT_ID);
    controller = new ChatController(chatUseCase, chatWebSourcesRepository, ownerContext);
    lenient()
        .when(httpRequest.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE))
        .thenReturn(CLIENT_ID);
  }

  @Nested
  @DisplayName("GET /api/health")
  class HealthEndpoint {

    @Test
    @DisplayName("should return UP status")
    void shouldReturnUpStatus() {
      var response = controller.health();

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody().status()).isEqualTo("UP");
    }
  }

  @Nested
  @DisplayName("POST /api/chat")
  class ChatEndpoint {

    @Test
    @DisplayName("should return response for valid message")
    void shouldReturnResponseForValidMessage() {
      when(chatUseCase.chatWithSession("Hello", CLIENT_ID)).thenReturn("Hi there!");

      var response = controller.chat(new ChatRequest("Hello", null), httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody().response()).isEqualTo("Hi there!");
      verify(chatUseCase).chatWithSession("Hello", CLIENT_ID);
    }

    @Test
    @DisplayName("should return 400 for null message")
    void shouldReturn400ForNullMessage() {
      var response = controller.chat(new ChatRequest(null, null), httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(400);
      assertThat(response.getBody().response()).isEqualTo("Please provide a message.");
    }

    @Test
    @DisplayName("should return 400 for blank message")
    void shouldReturn400ForBlankMessage() {
      var response = controller.chat(new ChatRequest("   ", null), httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("should use session when sessionId provided")
    void shouldUseSessionWhenSessionIdProvided() {
      when(chatUseCase.chatWithSession("22222222-2222-2222-2222-222222222222", "Hello", CLIENT_ID))
          .thenReturn("Response with context");

      var response =
          controller.chat(
              new ChatRequest("Hello", "22222222-2222-2222-2222-222222222222"), httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody().response()).isEqualTo("Response with context");
      verify(chatUseCase)
          .chatWithSession("22222222-2222-2222-2222-222222222222", "Hello", CLIENT_ID);
    }

    @Test
    @DisplayName("should handle long message without error")
    void shouldHandleLongMessageWithoutError() {
      String longMessage = "A".repeat(100);
      when(chatUseCase.chatWithSession(longMessage, CLIENT_ID))
          .thenReturn("Response to long message");

      var response = controller.chat(new ChatRequest(longMessage, null), httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
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
      when(chatUseCase.createSession("Custom Title", CLIENT_ID)).thenReturn(session);

      var response =
          controller.createSession(new CreateSessionRequest("Custom Title"), httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody().title()).isEqualTo("Custom Title");
    }

    @Test
    @DisplayName("should create session with default title when not provided")
    void shouldCreateSessionWithDefaultTitleWhenNotProvided() {
      ChatSession session = createTestSession("33333333-3333-3333-3333-333333333333", "New Chat");
      when(chatUseCase.createSession("New Chat", CLIENT_ID)).thenReturn(session);

      var response = controller.createSession(new CreateSessionRequest(null), httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody().title()).isEqualTo("New Chat");
    }

    @Test
    @DisplayName("should create session with default title when body is null")
    void shouldCreateSessionWithDefaultTitleWhenBodyIsNull() {
      ChatSession session = createTestSession("33333333-3333-3333-3333-333333333333", "New Chat");
      when(chatUseCase.createSession("New Chat", CLIENT_ID)).thenReturn(session);

      var response = controller.createSession(null, httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
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
      when(chatUseCase.getSessionsForClient(CLIENT_ID)).thenReturn(sessions);

      var response = controller.getAllSessions(httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("should return empty list when no sessions")
    void shouldReturnEmptyListWhenNoSessions() {
      when(chatUseCase.getSessionsForClient(CLIENT_ID)).thenReturn(List.of());

      var response = controller.getAllSessions(httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isEmpty();
    }
  }

  @Nested
  @DisplayName("GET /api/sessions/{sessionId}")
  class GetSession {

    @Test
    @DisplayName("should return session when found")
    void shouldReturnSessionWhenFound() {
      ChatSession session = createTestSession("22222222-2222-2222-2222-222222222222", "My Chat");
      when(chatUseCase.getSession("22222222-2222-2222-2222-222222222222", CLIENT_ID))
          .thenReturn(java.util.Optional.of(session));

      var response = controller.getSession("22222222-2222-2222-2222-222222222222", httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody().title()).isEqualTo("My Chat");
    }

    @Test
    @DisplayName("should return 404 when session not found")
    void shouldReturn404WhenSessionNotFound() {
      when(chatUseCase.getSession("missing", CLIENT_ID)).thenReturn(java.util.Optional.empty());

      var response = controller.getSession("missing", httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(404);
    }
  }

  @Nested
  @DisplayName("GET /api/sessions/{sessionId}/messages")
  class GetSessionMessages {

    @Test
    @DisplayName("should return messages for session")
    void shouldReturnMessagesForSession() {
      when(chatUseCase.getSessionHistory("22222222-2222-2222-2222-222222222222", CLIENT_ID))
          .thenReturn(
              List.of(
                  ChatMessage.createUserMessage("Hello"),
                  ChatMessage.createAssistantMessage("Hi")));
      when(chatWebSourcesRepository.findByConversationId("22222222-2222-2222-2222-222222222222"))
          .thenReturn(Map.of());

      var response =
          controller.getSessionMessages("22222222-2222-2222-2222-222222222222", httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).hasSize(2);
      assertThat(response.getBody().getFirst().role()).isEqualTo("user");
    }

    @Test
    @DisplayName("should attach persisted sources to assistant messages")
    void shouldAttachPersistedSourcesToAssistantMessages() {
      String reply = "Paris is the capital.";
      when(chatUseCase.getSessionHistory("22222222-2222-2222-2222-222222222222", CLIENT_ID))
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

      var response =
          controller.getSessionMessages("22222222-2222-2222-2222-222222222222", httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody().get(1).sources()).hasSize(1);
      assertThat(response.getBody().get(1).sources().getFirst().url())
          .isEqualTo("https://en.wikipedia.org/wiki/Paris");
    }

    @Test
    @DisplayName("should return 404 when session not found")
    void shouldReturn404WhenSessionNotFound() {
      when(chatUseCase.getSessionHistory("missing", CLIENT_ID))
          .thenThrow(new ChatSessionNotFoundException("missing"));

      var response = controller.getSessionMessages("missing", httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(404);
    }
  }

  @Nested
  @DisplayName("DELETE /api/sessions/{sessionId}")
  class DeleteSession {

    @Test
    @DisplayName("should delete session and return 204")
    void shouldDeleteSessionAndReturn204() {
      doNothing().when(chatUseCase).deleteSession("session-to-delete", CLIENT_ID);

      var response = controller.deleteSession("session-to-delete", httpRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(204);
      verify(chatUseCase).deleteSession("session-to-delete", CLIENT_ID);
    }
  }

  private ChatSession createTestSession(String id, String title) {
    return ChatSession.createWithId(com.ai.chat.domain.vo.ChatSessionId.of(id), title, CLIENT_ID);
  }
}
