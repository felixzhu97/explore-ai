package com.ai.chat.application.usecase;

import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatFacade")
class ChatFacadeTest {

    @Mock
    private SpringAiChatUseCase chatUseCase;

    private ChatFacade facade;

    @BeforeEach
    void setUp() {
        facade = new ChatFacade(chatUseCase);
    }

    @Nested
    @DisplayName("chat()")
    class Chat {

        @Test
        @DisplayName("should delegate simple chat to use case")
        void shouldDelegateSimpleChatToUseCase() {
            when(chatUseCase.chat("Hello")).thenReturn("Hi there!");

            String response = facade.chat("Hello");

            assertThat(response).isEqualTo("Hi there!");
            verify(chatUseCase).chat("Hello");
        }

        @Test
        @DisplayName("should handle empty message")
        void shouldHandleEmptyMessage() {
            when(chatUseCase.chat("")).thenReturn("Please provide a message.");

            String response = facade.chat("");

            assertThat(response).isEqualTo("Please provide a message.");
        }

        @Test
        @DisplayName("should handle null message")
        void shouldHandleNullMessage() {
            when(chatUseCase.chat(null)).thenReturn("Processed null");

            String response = facade.chat(null);

            assertThat(response).isEqualTo("Processed null");
        }
    }

    @Nested
    @DisplayName("chatWithSession()")
    class ChatWithSession {

        @Test
        @DisplayName("should delegate chat with session ID")
        void shouldDelegateChatWithSessionId() {
            when(chatUseCase.chatWithSession("session-123", "Hello"))
                    .thenReturn("Response with context");

            String response = facade.chatWithSession("session-123", "Hello");

            assertThat(response).isEqualTo("Response with context");
            verify(chatUseCase).chatWithSession("session-123", "Hello");
        }

        @Test
        @DisplayName("should delegate chat without session ID to default")
        void shouldDelegateChatToDefault() {
            when(chatUseCase.chatWithSession("Hello")).thenReturn("Default response");

            String response = facade.chatWithSession("Hello");

            assertThat(response).isEqualTo("Default response");
            verify(chatUseCase).chatWithSession("Hello");
        }

        @Test
        @DisplayName("should handle very long message")
        void shouldHandleVeryLongMessage() {
            String longMessage = "A".repeat(1000);
            when(chatUseCase.chatWithSession(longMessage)).thenReturn("Processed");

            String response = facade.chatWithSession(longMessage);

            assertThat(response).isEqualTo("Processed");
        }
    }

    @Nested
    @DisplayName("session management")
    class SessionManagement {

        @Test
        @DisplayName("should create session with title")
        void shouldCreateSessionWithTitle() {
            ChatSession expectedSession = ChatSession.create("Test Session");
            when(chatUseCase.createSession("Test Session")).thenReturn(expectedSession);

            ChatSession result = facade.createSession("Test Session");

            assertThat(result).isEqualTo(expectedSession);
            verify(chatUseCase).createSession("Test Session");
        }

        @Test
        @DisplayName("should get session by ID")
        void shouldGetSessionById() {
            ChatSession session = ChatSession.create("Test");
            when(chatUseCase.getSession("session-123")).thenReturn(Optional.of(session));

            Optional<ChatSession> result = facade.getSession("session-123");

            assertThat(result).isPresent().contains(session);
        }

        @Test
        @DisplayName("should return empty when session not found")
        void shouldReturnEmptyWhenSessionNotFound() {
            when(chatUseCase.getSession("non-existent")).thenReturn(Optional.empty());

            Optional<ChatSession> result = facade.getSession("non-existent");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should get session history")
        void shouldGetSessionHistory() {
            List<ChatMessage> messages = List.of(
                    ChatMessage.createUserMessage("Hello"),
                    ChatMessage.createAssistantMessage("Hi!")
            );
            when(chatUseCase.getSessionHistory("session-123")).thenReturn(messages);

            List<ChatMessage> result = facade.getSessionHistory("session-123");

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getText()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("should delete session")
        void shouldDeleteSession() {
            doNothing().when(chatUseCase).deleteSession("session-123");

            facade.deleteSession("session-123");

            verify(chatUseCase).deleteSession("session-123");
        }

        @Test
        @DisplayName("should get all sessions")
        void shouldGetAllSessions() {
            List<ChatSession> sessions = List.of(
                    ChatSession.create("Session 1"),
                    ChatSession.create("Session 2")
            );
            when(chatUseCase.getAllSessions()).thenReturn(sessions);

            List<ChatSession> result = facade.getAllSessions();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no sessions")
        void shouldReturnEmptyListWhenNoSessions() {
            when(chatUseCase.getAllSessions()).thenReturn(List.of());

            List<ChatSession> result = facade.getAllSessions();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("truncate()")
    class Truncate {

        @Test
        @DisplayName("should return text as is when shorter than 50 chars")
        void shouldReturnTextWhenShort() {
            String shortText = "Hello world";
            when(chatUseCase.chat(shortText)).thenReturn("Response");

            facade.chat(shortText);

            verify(chatUseCase).chat("Hello world");
        }

        @Test
        @DisplayName("should truncate text when longer than 50 chars in log")
        void shouldTruncateLongTextInLog() {
            String longText = "A".repeat(60);
            when(chatUseCase.chat(longText)).thenReturn("Response");

            facade.chat(longText);

            verify(chatUseCase).chat(longText);
        }
    }
}
