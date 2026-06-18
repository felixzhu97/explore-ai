package com.ai.adapter.in.controller;

import com.ai.domain.service.AiChatService;
import com.ai.domain.model.ChatMessage;
import com.ai.domain.model.ChatSession;
import com.ai.domain.vo.ChatSessionId;
import com.ai.domain.vo.MessageId;
import com.ai.adapter.in.controller.AiController;
import com.ai.adapter.in.controller.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("AiController Session Management Tests")
class ChatControllerSessionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiChatService chatService;

    @Nested
    @DisplayName("shouldCreateSession_WhenValidRequest")
    class CreateSessionTests {

        @Test
        @DisplayName("should create session with custom title and return 200")
        void shouldCreateSessionWithCustomTitleAndReturn200() throws Exception {
            ChatSession session = createTestSession("session-123", "My Custom Chat");
            when(chatService.createSession("My Custom Chat")).thenReturn(session);

            String requestBody = objectMapper.writeValueAsString(Map.of("title", "My Custom Chat"));

            mockMvc.perform(post("/api/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value("session-123"))
                    .andExpect(jsonPath("$.title").value("My Custom Chat"))
                    .andExpect(jsonPath("$.messageCount").value(0));

            verify(chatService).createSession("My Custom Chat");
        }

        @Test
        @DisplayName("should create session with default title when title is not provided")
        void shouldCreateSessionWithDefaultTitleWhenTitleNotProvided() throws Exception {
            ChatSession session = createTestSession("session-456", "New Chat");
            when(chatService.createSession("New Chat")).thenReturn(session);

            mockMvc.perform(post("/api/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value("session-456"))
                    .andExpect(jsonPath("$.title").value("New Chat"));

            verify(chatService).createSession("New Chat");
        }

        @Test
        @DisplayName("should create session when request body is empty")
        void shouldCreateSessionWhenRequestBodyIsEmpty() throws Exception {
            ChatSession session = createTestSession("session-789", "New Chat");
            when(chatService.createSession("New Chat")).thenReturn(session);

            mockMvc.perform(post("/api/sessions")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value("session-789"));

            verify(chatService).createSession("New Chat");
        }
    }

    @Nested
    @DisplayName("shouldReturnSessions_WhenGetAllSessions")
    class GetAllSessionsTests {

        @Test
        @DisplayName("should return all sessions when getAllSessions is called")
        void shouldReturnAllSessionsWhenGetAllSessionsCalled() throws Exception {
            List<ChatSession> sessions = List.of(
                createTestSession("session-1", "Chat 1"),
                createTestSession("session-2", "Chat 2"),
                createTestSession("session-3", "Chat 3")
            );
            when(chatService.getAllSessions()).thenReturn(sessions);

            mockMvc.perform(get("/api/sessions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].sessionId").value("session-1"))
                    .andExpect(jsonPath("$[1].sessionId").value("session-2"))
                    .andExpect(jsonPath("$[2].sessionId").value("session-3"));
        }

        @Test
        @DisplayName("should return empty list when no sessions exist")
        void shouldReturnEmptyListWhenNoSessionsExist() throws Exception {
            when(chatService.getAllSessions()).thenReturn(List.of());

            mockMvc.perform(get("/api/sessions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("shouldReturnMessages_WhenSessionExists")
    class GetSessionMessagesTests {

        @Test
        @DisplayName("should return messages when session exists")
        void shouldReturnMessagesWhenSessionExists() throws Exception {
            String sessionId = "existing-session";
            ChatSession session = createTestSessionWithMessages(
                sessionId,
                "Test Session",
                List.of(
                    createTestMessage("Hello", "user"),
                    createTestMessage("Hi there!", "assistant")
                )
            );
            when(chatService.getSession(sessionId)).thenReturn(Optional.of(session));

            mockMvc.perform(get("/api/sessions/{sessionId}/messages", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId))
                    .andExpect(jsonPath("$.messages").isArray())
                    .andExpect(jsonPath("$.messages.length()").value(2))
                    .andExpect(jsonPath("$.messages[0].text").value("Hello"))
                    .andExpect(jsonPath("$.messages[0].role").value("user"))
                    .andExpect(jsonPath("$.messages[1].text").value("Hi there!"))
                    .andExpect(jsonPath("$.messages[1].role").value("assistant"))
                    .andExpect(jsonPath("$.totalCount").value(2));
        }

        @Test
        @DisplayName("should return empty messages when session has no messages")
        void shouldReturnEmptyMessagesWhenSessionHasNoMessages() throws Exception {
            String sessionId = "empty-session";
            ChatSession session = createTestSession(sessionId, "Empty Session");
            when(chatService.getSession(sessionId)).thenReturn(Optional.of(session));

            mockMvc.perform(get("/api/sessions/{sessionId}/messages", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId))
                    .andExpect(jsonPath("$.messages").isArray())
                    .andExpect(jsonPath("$.messages").isEmpty())
                    .andExpect(jsonPath("$.totalCount").value(0));
        }
    }

    @Nested
    @DisplayName("shouldReturn404_WhenSessionNotFound")
    class SessionNotFoundTests {

        @Test
        @DisplayName("should return 404 when session does not exist")
        void shouldReturn404WhenSessionDoesNotExist() throws Exception {
            String sessionId = "non-existent-session";
            when(chatService.getSession(sessionId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/sessions/{sessionId}/messages", sessionId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("shouldDeleteSession_WhenSessionExists")
    class DeleteSessionTests {

        @Test
        @DisplayName("should delete session and return 204")
        void shouldDeleteSessionAndReturn204() throws Exception {
            String sessionId = "session-to-delete";
            doNothing().when(chatService).deleteSession(sessionId);

            mockMvc.perform(delete("/api/sessions/{sessionId}", sessionId))
                    .andExpect(status().isNoContent());

            verify(chatService).deleteSession(sessionId);
        }
    }

    private ChatSession createTestSession(String id, String title) {
        return createTestSessionWithMessages(id, title, List.of());
    }

    private ChatSession createTestSessionWithMessages(String id, String title, List<ChatMessage> messages) {
        ChatSession session = ChatSession.create(title);
        try {
            java.lang.reflect.Field idField = ChatSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(session, ChatSessionId.of(id));
        } catch (Exception e) {
            // Ignore for testing
        }
        try {
            java.lang.reflect.Field messagesField = ChatSession.class.getDeclaredField("messages");
            messagesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<ChatMessage> list = (java.util.List<ChatMessage>) messagesField.get(session);
            list.addAll(messages);
        } catch (Exception e) {
            // Ignore for testing
        }
        return session;
    }

    private ChatMessage createTestMessage(String text, String role) {
        return ChatMessage.of(
            MessageId.generate(),
            text,
            role,
            Instant.now()
        );
    }
}
