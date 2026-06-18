package com.ai.adapter.in.controller;

import com.ai.adapter.in.dto.ChatRequest;
import com.ai.domain.service.AiChatService;
import com.ai.domain.model.ChatSession;
import com.ai.domain.model.ChatMessage;
import com.ai.domain.vo.ChatSessionId;
import com.ai.domain.vo.MessageId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiController")
class AiControllerTest {

    @Mock
    private AiChatService chatService;

    private AiController controller;

    @BeforeEach
    void setUp() {
        controller = new AiController(chatService);
    }

    @Nested
    @DisplayName("POST /api/chat")
    class ChatEndpoint {

        @Test
        @DisplayName("should return response for valid message")
        void shouldReturnResponseForValidMessage() {
            when(chatService.processChatMessage("Hello")).thenReturn("Hi there!");

            var response = controller.chat(new ChatRequest("Hello", null));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().response()).isEqualTo("Hi there!");
            verify(chatService).processChatMessage("Hello");
        }

        @Test
        @DisplayName("should return 400 for null message")
        void shouldReturn400ForNullMessage() {
            var response = controller.chat(new ChatRequest(null, null));

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().response()).isEqualTo("Please provide a message.");
        }

        @Test
        @DisplayName("should return 400 for blank message")
        void shouldReturn400ForBlankMessage() {
            var response = controller.chat(new ChatRequest("   ", null));

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("should use session when sessionId provided")
        void shouldUseSessionWhenSessionIdProvided() {
            when(chatService.processChatMessage("session-123", "Hello"))
                    .thenReturn("Response with context");

            var response = controller.chat(new ChatRequest("Hello", "session-123"));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().response()).isEqualTo("Response with context");
            verify(chatService).processChatMessage("session-123", "Hello");
        }
    }

    @Nested
    @DisplayName("POST /api/chat/simple")
    class SimpleChatEndpoint {

        @Test
        @DisplayName("should return response for valid request")
        void shouldReturnResponseForValidRequest() {
            when(chatService.processChatMessage("Simple message")).thenReturn("Simple response");

            var response = controller.chatSimple(Map.of("message", "Simple message"));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().get("response")).isEqualTo("Simple response");
        }

        @Test
        @DisplayName("should return 400 for missing message")
        void shouldReturn400ForMissingMessage() {
            var response = controller.chatSimple(Map.of());

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("GET /api/sessions/{sessionId}/messages")
    class GetMessages {

        @Test
        @DisplayName("should return messages for existing session")
        void shouldReturnMessagesForExistingSession() {
            ChatSession session = createTestSession("session-123", "Test Session");
            when(chatService.getSession("session-123")).thenReturn(Optional.of(session));

            var response = controller.getMessages("session-123");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("should return 404 for non-existent session")
        void shouldReturn404ForNonExistentSession() {
            when(chatService.getSession("non-existent")).thenReturn(Optional.empty());

            var response = controller.getMessages("non-existent");

            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("POST /api/sessions")
    class CreateSession {

        @Test
        @DisplayName("should create session with custom title")
        void shouldCreateSessionWithCustomTitle() {
            ChatSession session = createTestSession("new-session", "Custom Title");
            when(chatService.createSession("Custom Title")).thenReturn(session);

            var response = controller.createSession(Map.of("title", "Custom Title"));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().title()).isEqualTo("Custom Title");
        }

        @Test
        @DisplayName("should create session with default title when not provided")
        void shouldCreateSessionWithDefaultTitle() {
            ChatSession session = createTestSession("new-session", "New Chat");
            when(chatService.createSession("New Chat")).thenReturn(session);

            var response = controller.createSession(Map.of());

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().title()).isEqualTo("New Chat");
        }

        @Test
        @DisplayName("should create session with default title when body is null")
        void shouldCreateSessionWithDefaultTitleWhenBodyIsNull() {
            ChatSession session = createTestSession("new-session", "New Chat");
            when(chatService.createSession("New Chat")).thenReturn(session);

            var response = controller.createSession(null);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("GET /api/sessions")
    class GetAllSessions {

        @Test
        @DisplayName("should return all sessions")
        void shouldReturnAllSessions() {
            List<ChatSession> sessions = List.of(
                    createTestSession("session-1", "Chat 1"),
                    createTestSession("session-2", "Chat 2")
            );
            when(chatService.getAllSessions()).thenReturn(sessions);

            var response = controller.getAllSessions();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no sessions")
        void shouldReturnEmptyListWhenNoSessions() {
            when(chatService.getAllSessions()).thenReturn(List.of());

            var response = controller.getAllSessions();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("DELETE /api/sessions/{sessionId}")
    class DeleteSession {

        @Test
        @DisplayName("should delete session and return 204")
        void shouldDeleteSessionAndReturn204() {
            doNothing().when(chatService).deleteSession("session-to-delete");

            var response = controller.deleteSession("session-to-delete");

            assertThat(response.getStatusCode().value()).isEqualTo(204);
            verify(chatService).deleteSession("session-to-delete");
        }
    }

    @Nested
    @DisplayName("GET /api/health")
    class HealthEndpoint {

        @Test
        @DisplayName("should return UP status")
        void shouldReturnUpStatus() {
            var response = controller.health();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().get("status")).isEqualTo("UP");
        }
    }

    private ChatSession createTestSession(String id, String title) {
        return createTestSession(id, title, List.of());
    }

    private ChatSession createTestSession(String id, String title, List<ChatMessage> messages) {
        ChatSession session = ChatSession.create(title);
        try {
            java.lang.reflect.Field idField = ChatSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(session, ChatSessionId.of(id));
        } catch (Exception e) {
            // Ignore for testing
        }
        return session;
    }

    private ChatMessage createMessage(String text, String role) {
        return ChatMessage.of(MessageId.generate(), text, role, Instant.now());
    }
}
