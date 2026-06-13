package com.ai.interfaces;

import com.ai.application.service.ChatApplicationService;
import com.ai.domain.model.ChatMessage;
import com.ai.domain.model.ChatSession;
import com.ai.domain.vo.ChatSessionId;
import com.ai.domain.vo.MessageId;
import com.ai.interfaces.controller.ChatController;
import com.ai.interfaces.controller.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChatController Session Management Tests
 *
 * Tests using standalone MockMvc setup for session management endpoints:
 * - Create session (with/without title)
 * - Get all sessions
 * - Get specific session messages (exists/not exists)
 * - Delete session
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController Session Management Tests")
class ChatControllerSessionTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ChatApplicationService chatService;

    @InjectMocks
    private ChatController chatController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("shouldCreateSession_WhenValidRequest")
    class CreateSessionTests {

        @Test
        @DisplayName("should create session with custom title and return 200")
        void shouldCreateSessionWithCustomTitleAndReturn200() throws Exception {
            // Arrange
            ChatSession session = createTestSession("session-123", "My Custom Chat");
            when(chatService.createSession("My Custom Chat")).thenReturn(session);

            String requestBody = objectMapper.writeValueAsString(Map.of("title", "My Custom Chat"));

            // Act & Assert
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
            // Arrange
            ChatSession session = createTestSession("session-456", "New Chat");
            when(chatService.createSession("New Chat")).thenReturn(session);

            // Act & Assert
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
            // Arrange
            ChatSession session = createTestSession("session-789", "New Chat");
            when(chatService.createSession("New Chat")).thenReturn(session);

            // Act & Assert
            mockMvc.perform(post("/api/sessions")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value("session-789"));

            verify(chatService).createSession("New Chat");
        }

        @Test
        @DisplayName("should create session with null title handled gracefully")
        void shouldCreateSessionWithNullTitleHandledGracefully() throws Exception {
            // Arrange
            ChatSession session = createTestSession("session-abc", "New Chat");
            // When title is null in JSON, the controller uses "New Chat" as default
            when(chatService.createSession("New Chat")).thenReturn(session);

            // Act & Assert - send null explicitly in JSON
            mockMvc.perform(post("/api/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":null}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value("session-abc"));
        }
    }

    @Nested
    @DisplayName("shouldReturnSessions_WhenGetAllSessions")
    class GetAllSessionsTests {

        @Test
        @DisplayName("should return all sessions when getAllSessions is called")
        void shouldReturnAllSessionsWhenGetAllSessionsCalled() throws Exception {
            // Arrange
            List<ChatSession> sessions = List.of(
                createTestSession("session-1", "Chat 1"),
                createTestSession("session-2", "Chat 2"),
                createTestSession("session-3", "Chat 3")
            );
            when(chatService.getAllSessions()).thenReturn(sessions);

            // Act & Assert
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
            // Arrange
            when(chatService.getAllSessions()).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/sessions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("should include session details in response")
        void shouldIncludeSessionDetailsInResponse() throws Exception {
            // Arrange
            ChatSession session = createTestSession("session-detail", "Detailed Chat");
            when(chatService.getAllSessions()).thenReturn(List.of(session));

            // Act & Assert
            mockMvc.perform(get("/api/sessions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].sessionId").value("session-detail"))
                    .andExpect(jsonPath("$[0].title").value("Detailed Chat"))
                    .andExpect(jsonPath("$[0].messageCount").value(0))
                    .andExpect(jsonPath("$[0].createdAt").exists())
                    .andExpect(jsonPath("$[0].lastActivityAt").exists());
        }
    }

    @Nested
    @DisplayName("shouldReturnMessages_WhenSessionExists")
    class GetSessionMessagesTests {

        @Test
        @DisplayName("should return messages when session exists")
        void shouldReturnMessagesWhenSessionExists() throws Exception {
            // Arrange
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

            // Act & Assert
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
            // Arrange
            String sessionId = "empty-session";
            ChatSession session = createTestSession(sessionId, "Empty Session");
            when(chatService.getSession(sessionId)).thenReturn(Optional.of(session));

            // Act & Assert
            mockMvc.perform(get("/api/sessions/{sessionId}/messages", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId))
                    .andExpect(jsonPath("$.messages").isArray())
                    .andExpect(jsonPath("$.messages").isEmpty())
                    .andExpect(jsonPath("$.totalCount").value(0));
        }

        @Test
        @DisplayName("should include timestamp in message response")
        void shouldIncludeTimestampInMessageResponse() throws Exception {
            // Arrange
            String sessionId = "session-with-time";
            Instant fixedTime = Instant.parse("2024-01-15T10:30:00Z");
            ChatMessage message = ChatMessage.of(
                MessageId.generate(),
                "Test message",
                "user",
                fixedTime
            );
            ChatSession session = createTestSessionWithMessages(sessionId, "Time Test", List.of(message));
            when(chatService.getSession(sessionId)).thenReturn(Optional.of(session));

            // Act & Assert
            mockMvc.perform(get("/api/sessions/{sessionId}/messages", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages[0].id").exists())
                    .andExpect(jsonPath("$.messages[0].timestamp").exists());
        }
    }

    @Nested
    @DisplayName("shouldReturn404_WhenSessionNotFound")
    class SessionNotFoundTests {

        @Test
        @DisplayName("should return 404 when session does not exist")
        void shouldReturn404WhenSessionDoesNotExist() throws Exception {
            // Arrange
            String sessionId = "non-existent-session";
            when(chatService.getSession(sessionId)).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get("/api/sessions/{sessionId}/messages", sessionId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 when session ID is invalid format")
        void shouldReturn404WhenSessionIdIsInvalidFormat() throws Exception {
            // Arrange
            String invalidSessionId = "invalid-session-id";
            when(chatService.getSession(invalidSessionId)).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get("/api/sessions/{sessionId}/messages", invalidSessionId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 for non-existent session")
        void shouldReturn404ForNonExistentSession() throws Exception {
            // Arrange
            String sessionId = "non-existent-session-id-123";
            when(chatService.getSession(sessionId)).thenReturn(Optional.empty());

            // Act & Assert
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
            // Arrange
            String sessionId = "session-to-delete";
            doNothing().when(chatService).deleteSession(sessionId);

            // Act & Assert
            mockMvc.perform(delete("/api/sessions/{sessionId}", sessionId))
                    .andExpect(status().isNoContent());

            verify(chatService).deleteSession(sessionId);
        }

        @Test
        @DisplayName("should call deleteSession with correct session ID")
        void shouldCallDeleteSessionWithCorrectSessionId() throws Exception {
            // Arrange
            String sessionId = "specific-session-123";
            doNothing().when(chatService).deleteSession(sessionId);

            // Act
            mockMvc.perform(delete("/api/sessions/{sessionId}", sessionId))
                    .andExpect(status().isNoContent());

            // Assert
            verify(chatService).deleteSession("specific-session-123");
        }

        @Test
        @DisplayName("should handle delete for session with special characters in ID")
        void shouldHandleDeleteForSessionWithSpecialCharactersInId() throws Exception {
            // Arrange
            String sessionId = "session_with_underscores";
            doNothing().when(chatService).deleteSession(sessionId);

            // Act & Assert
            mockMvc.perform(delete("/api/sessions/{sessionId}", sessionId))
                    .andExpect(status().isNoContent());

            verify(chatService).deleteSession(sessionId);
        }
    }

    // Helper methods
    private ChatSession createTestSession(String id, String title) {
        return createTestSessionWithMessages(id, title, List.of());
    }

    private ChatSession createTestSessionWithMessages(String id, String title, List<ChatMessage> messages) {
        ChatSession session = ChatSession.create(title);
        // Use reflection to set the ID since there's no public setter
        try {
            java.lang.reflect.Field idField = ChatSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(session, ChatSessionId.of(id));
        } catch (Exception e) {
            // Ignore for testing - ID will be random
        }
        // Add messages using reflection
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
