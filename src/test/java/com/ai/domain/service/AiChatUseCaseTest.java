package com.ai.domain.service;

import com.ai.ai.domain.model.ChatMessage;
import com.ai.ai.domain.model.ChatSession;
import com.ai.ai.application.usecase.SpringAiChatUseCase;
import com.ai.ai.domain.exception.ChatSessionNotFoundException;
import com.ai.ai.domain.repository.ChatSessionRepository;
import com.ai.ai.domain.vo.ChatSessionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * AiChatUseCase Domain Service Tests
 *
 * Tests for AI chat operations following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests service layer orchestration with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AiChatUseCase")
class AiChatUseCaseTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatSessionRepository repository;

    @Mock
    private org.springframework.retry.support.RetryTemplate retryTemplate;

    @Mock
    private ChatMemory chatMemory;

    private SpringAiChatUseCase aiChatService;

    @BeforeEach
    void setUp() {
        lenient().when(chatClientBuilder.build()).thenReturn(chatClient);
        aiChatService = new SpringAiChatUseCase(chatClientBuilder, repository, retryTemplate, chatMemory);
    }

    @Nested
    @DisplayName("createSession()")
    class CreateSession {

        @Test
        @DisplayName("should create session with provided title")
        void shouldCreateSessionWithProvidedTitle() {
            // Arrange
            String title = "Custom Title";

            // Act
            ChatSession result = aiChatService.createSession(title);

            // Assert
            assertThat(result.getTitle()).isEqualTo(title);
            verify(repository).save(result);
        }

        @Test
        @DisplayName("should create session with default title when no title provided")
        void shouldCreateSessionWithDefaultTitleWhenNoTitleProvided() {
            // Act
            ChatSession result = aiChatService.createSession();

            // Assert
            assertThat(result.getTitle()).isEqualTo("New Chat");
            verify(repository).save(result);
        }
    }

    @Nested
    @DisplayName("getSession()")
    class GetSession {

        @Test
        @DisplayName("should return session when exists")
        void shouldReturnSessionWhenExists() {
            // Arrange
            String sessionId = "test-session-id";
            ChatSession session = ChatSession.create("Test");

            when(repository.findById(ChatSessionId.of(sessionId))).thenReturn(Optional.of(session));

            // Act
            Optional<ChatSession> result = aiChatService.getSession(sessionId);

            // Assert
            assertThat(result).isPresent().contains(session);
        }

        @Test
        @DisplayName("should return empty Optional when session not found")
        void shouldReturnEmptyOptionalWhenSessionNotFound() {
            // Arrange
            String sessionId = "non-existent";

            when(repository.findById(ChatSessionId.of(sessionId))).thenReturn(Optional.empty());

            // Act
            Optional<ChatSession> result = aiChatService.getSession(sessionId);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getSessionHistory()")
    class GetSessionHistory {

        @Test
        @DisplayName("should return message list when session exists")
        void shouldReturnMessageListWhenSessionExists() {
            // Arrange
            String sessionId = "test-session-id";
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");
            session.addAssistantMessage("Hi");

            when(repository.findById(ChatSessionId.of(sessionId))).thenReturn(Optional.of(session));

            // Act
            List<ChatMessage> result = aiChatService.getSessionHistory(sessionId);

            // Assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should throw exception when session not found")
        void shouldThrowExceptionWhenSessionNotFound() {
            // Arrange
            String sessionId = "non-existent";

            when(repository.findById(ChatSessionId.of(sessionId))).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.getSessionHistory(sessionId))
                    .isInstanceOf(ChatSessionNotFoundException.class)
                    .hasMessageContaining(sessionId);
        }
    }

    @Nested
    @DisplayName("getRecentMessages()")
    class GetRecentMessages {

        @Test
        @DisplayName("should return recent messages")
        void shouldReturnRecentMessages() {
            // Arrange
            String sessionId = "test-session-id";
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("First");
            session.addUserMessage("Second");
            session.addUserMessage("Third");

            when(repository.findById(ChatSessionId.of(sessionId))).thenReturn(Optional.of(session));

            // Act
            List<ChatMessage> result = aiChatService.getRecentMessages(sessionId, 2);

            // Assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should throw exception when session not found")
        void shouldThrowExceptionWhenSessionNotFound() {
            // Arrange
            String sessionId = "non-existent";

            when(repository.findById(ChatSessionId.of(sessionId))).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.getRecentMessages(sessionId, 2))
                    .isInstanceOf(ChatSessionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteSession()")
    class DeleteSession {

        @Test
        @DisplayName("should call repository delete")
        void shouldCallRepositoryDelete() {
            // Arrange
            String sessionId = "test-session-id";

            // Act
            aiChatService.deleteSession(sessionId);

            // Assert
            verify(repository).delete(ChatSessionId.of(sessionId));
        }
    }

    @Nested
    @DisplayName("getAllSessions()")
    class GetAllSessions {

        @Test
        @DisplayName("should return all sessions")
        void shouldReturnAllSessions() {
            // Arrange
            List<ChatSession> sessions = List.of(
                    ChatSession.create("Session 1"),
                    ChatSession.create("Session 2")
            );
            when(repository.findAll()).thenReturn(sessions);

            // Act
            List<ChatSession> result = aiChatService.getAllSessions();

            // Assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no sessions")
        void shouldReturnEmptyListWhenNoSessions() {
            // Arrange
            when(repository.findAll()).thenReturn(List.of());

            // Act
            List<ChatSession> result = aiChatService.getAllSessions();

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getChatClient()")
    class GetChatClient {

        @Test
        @DisplayName("should return chat client")
        void shouldReturnChatClient() {
            // Act
            ChatClient result = aiChatService.getChatClient();

            // Assert
            assertThat(result).isSameAs(chatClient);
        }
    }

    @Nested
    @DisplayName("clearConversationMemory()")
    class ClearConversationMemory {

        @Test
        @DisplayName("should call chatMemory clear")
        void shouldCallChatMemoryClear() {
            // Arrange
            String conversationId = "test-conversation";

            // Act
            aiChatService.clearConversationMemory(conversationId);

            // Assert
            verify(chatMemory).clear(conversationId);
        }
    }

    @Nested
    @DisplayName("chatWithSession()")
    class ChatWithSession {

        @Test
        @DisplayName("should update session when session exists")
        void shouldUpdateSessionWhenSessionExists() {
            // Arrange
            String sessionId = "test-session-id";
            String userMessage = "Hello";
            ChatSession session = ChatSession.create("Test Session");

            when(repository.findById(ChatSessionId.of(sessionId))).thenReturn(Optional.of(session));
            when(retryTemplate.execute(any())).thenReturn("AI Response");

            // Act
            String result = aiChatService.chatWithSession(sessionId, userMessage);

            // Assert
            assertThat(result).isEqualTo("AI Response");
            verify(repository, atLeast(1)).save(session);
        }

        @Test
        @DisplayName("should catch exception and use default session when session not found")
        void shouldCatchExceptionAndUseDefaultSessionWhenSessionNotFound() {
            // Arrange
            String sessionId = "non-existent-session";
            String userMessage = "Hello";
            ChatSession defaultSession = ChatSession.create("Default");

            when(repository.findById(ChatSessionId.of(sessionId)))
                    .thenThrow(new ChatSessionNotFoundException(sessionId));
            when(repository.getOrCreateDefaultSession()).thenReturn(defaultSession);
            when(retryTemplate.execute(any())).thenReturn("Default Response");

            // Act
            String result = aiChatService.chatWithSession(sessionId, userMessage);

            // Assert
            assertThat(result).isNotNull();
            verify(repository).getOrCreateDefaultSession();
        }

        @Test
        @DisplayName("should call getOrCreateDefaultSession when using default session")
        void shouldCallGetOrCreateDefaultSessionWhenUsingDefaultSession() {
            // Arrange
            String userMessage = "Hello";
            ChatSession defaultSession = ChatSession.create("Default");

            when(repository.getOrCreateDefaultSession()).thenReturn(defaultSession);
            when(retryTemplate.execute(any())).thenReturn("Response");

            // Act
            aiChatService.chatWithSession(userMessage);

            // Assert
            verify(repository).getOrCreateDefaultSession();
        }
    }
}
