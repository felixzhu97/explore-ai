package com.ai.chat.web;

import com.ai.chat.application.usecase.*;
import com.ai.chat.web.dto.*;
import com.ai.chat.domain.model.ChatSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController")
class ChatControllerTest {

    @Mock
    private ChatFacade chatFacade;

    private ChatController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatController(chatFacade);
    }

    @Nested
    @DisplayName("POST /api/chat")
    class ChatEndpoint {

        @Test
        @DisplayName("should return response for valid message")
        void shouldReturnResponseForValidMessage() {
            when(chatFacade.chatWithSession("Hello")).thenReturn("Hi there!");

            var response = controller.chat(new ChatRequest("Hello", null));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().response()).isEqualTo("Hi there!");
            verify(chatFacade).chatWithSession("Hello");
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
            when(chatFacade.chatWithSession("session-123", "Hello"))
                    .thenReturn("Response with context");

            var response = controller.chat(new ChatRequest("Hello", "session-123"));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().response()).isEqualTo("Response with context");
            verify(chatFacade).chatWithSession("session-123", "Hello");
        }

        @Test
        @DisplayName("should handle long message without error")
        void shouldHandleLongMessageWithoutError() {
            String longMessage = "A".repeat(100);
            when(chatFacade.chatWithSession(longMessage)).thenReturn("Response to long message");

            var response = controller.chat(new ChatRequest(longMessage, null));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("POST /api/sessions")
    class CreateSession {

        @Test
        @DisplayName("should create session with custom title")
        void shouldCreateSessionWithCustomTitle() {
            ChatSession session = createTestSession("new-session", "Custom Title");
            when(chatFacade.createSession("Custom Title")).thenReturn(session);

            var response = controller.createSession(new CreateSessionRequest("Custom Title"));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().title()).isEqualTo("Custom Title");
        }

        @Test
        @DisplayName("should create session with default title when not provided")
        void shouldCreateSessionWithDefaultTitleWhenNotProvided() {
            ChatSession session = createTestSession("new-session", "New Chat");
            when(chatFacade.createSession("New Chat")).thenReturn(session);

            var response = controller.createSession(new CreateSessionRequest(null));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().title()).isEqualTo("New Chat");
        }

        @Test
        @DisplayName("should create session with default title when body is null")
        void shouldCreateSessionWithDefaultTitleWhenBodyIsNull() {
            ChatSession session = createTestSession("new-session", "New Chat");
            when(chatFacade.createSession("New Chat")).thenReturn(session);

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
            when(chatFacade.getAllSessions()).thenReturn(sessions);

            var response = controller.getAllSessions();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no sessions")
        void shouldReturnEmptyListWhenNoSessions() {
            when(chatFacade.getAllSessions()).thenReturn(List.of());

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
            doNothing().when(chatFacade).deleteSession("session-to-delete");

            var response = controller.deleteSession("session-to-delete");

            assertThat(response.getStatusCode().value()).isEqualTo(204);
            verify(chatFacade).deleteSession("session-to-delete");
        }
    }

    private ChatSession createTestSession(String id, String title) {
        ChatSession session = ChatSession.create(title);
        try {
            java.lang.reflect.Field idField = ChatSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(session, com.ai.chat.domain.vo.ChatSessionId.of(id));
        } catch (Exception e) {
            // Ignore for testing
        }
        return session;
    }
}
