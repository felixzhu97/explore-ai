package com.ai.application.service;

import com.ai.application.port.AiChatPort;
import com.ai.application.port.ChatSessionRepositoryPort;
import com.ai.application.usecase.SendChatMessageUseCase;
import com.ai.domain.model.ChatMessage;
import com.ai.domain.model.ChatSession;
import com.ai.domain.model.ChatSessionNotFoundException;
import com.ai.domain.vo.ChatSessionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ChatApplicationService Unit Tests
 * 
 * Tests using Mockito to mock use case and port dependencies:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests service facade orchestration
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatApplicationService")
class ChatApplicationServiceTest {

    @Mock
    private ChatSessionRepositoryPort repositoryPort;

    @Mock
    private AiChatPort aiChatPort;

    @Mock
    private SendChatMessageUseCase sendChatMessageUseCase;

    private ChatApplicationService service;

    private static final String TEST_SESSION_ID = "test-session-id";
    private static final String TEST_TITLE = "Test Chat";
    private static final String USER_MESSAGE = "Hello!";
    private static final String ASSISTANT_RESPONSE = "Hi there!";

    @BeforeEach
    void setUp() {
        service = new ChatApplicationService(
            repositoryPort,
            aiChatPort,
            sendChatMessageUseCase
        );
    }

    @Nested
    @DisplayName("processChatMessage with session ID")
    class ProcessChatMessageWithSessionId {

        @Test
        @DisplayName("should return AI response text when session exists")
        void shouldReturnAiResponseTextWhenSessionExists() {
            // Arrange
            ChatMessage assistantMessage = ChatMessage.createAssistantMessage(ASSISTANT_RESPONSE);
            when(sendChatMessageUseCase.execute(TEST_SESSION_ID, USER_MESSAGE))
                .thenReturn(assistantMessage);

            // Act
            String result = service.processChatMessage(TEST_SESSION_ID, USER_MESSAGE);

            // Assert
            assertThat(result).isEqualTo(ASSISTANT_RESPONSE);
        }

        @Test
        @DisplayName("should delegate to use case with correct parameters")
        void shouldDelegateToUseCaseWithCorrectParameters() {
            // Arrange
            ChatMessage assistantMessage = ChatMessage.createAssistantMessage(ASSISTANT_RESPONSE);
            when(sendChatMessageUseCase.execute(TEST_SESSION_ID, USER_MESSAGE))
                .thenReturn(assistantMessage);

            // Act
            service.processChatMessage(TEST_SESSION_ID, USER_MESSAGE);

            // Assert
            verify(sendChatMessageUseCase).execute(TEST_SESSION_ID, USER_MESSAGE);
        }

        @Test
        @DisplayName("should use default session when session not found")
        void shouldUseDefaultSessionWhenSessionNotFound() {
            // Arrange
            ChatMessage assistantMessage = ChatMessage.createAssistantMessage(ASSISTANT_RESPONSE);
            when(sendChatMessageUseCase.execute(TEST_SESSION_ID, USER_MESSAGE))
                .thenThrow(new ChatSessionNotFoundException(TEST_SESSION_ID));
            when(sendChatMessageUseCase.executeInDefaultSession(USER_MESSAGE))
                .thenReturn(assistantMessage);

            // Act
            String result = service.processChatMessage(TEST_SESSION_ID, USER_MESSAGE);

            // Assert
            assertThat(result).isEqualTo(ASSISTANT_RESPONSE);
            verify(sendChatMessageUseCase).executeInDefaultSession(USER_MESSAGE);
        }

        @Test
        @DisplayName("should fallback to default session only once when session not found")
        void shouldFallbackToDefaultSessionOnlyOnceWhenSessionNotFound() {
            // Arrange
            when(sendChatMessageUseCase.execute(TEST_SESSION_ID, USER_MESSAGE))
                .thenThrow(new ChatSessionNotFoundException(TEST_SESSION_ID));
            when(sendChatMessageUseCase.executeInDefaultSession(USER_MESSAGE))
                .thenReturn(ChatMessage.createAssistantMessage(ASSISTANT_RESPONSE));

            // Act
            service.processChatMessage(TEST_SESSION_ID, USER_MESSAGE);

            // Assert
            verify(sendChatMessageUseCase, times(1)).executeInDefaultSession(USER_MESSAGE);
        }

        @Test
        @DisplayName("should return message text from default session on fallback")
        void shouldReturnMessageTextFromDefaultSessionOnFallback() {
            // Arrange
            String fallbackResponse = "Fallback response";
            when(sendChatMessageUseCase.execute(TEST_SESSION_ID, USER_MESSAGE))
                .thenThrow(new ChatSessionNotFoundException(TEST_SESSION_ID));
            when(sendChatMessageUseCase.executeInDefaultSession(USER_MESSAGE))
                .thenReturn(ChatMessage.createAssistantMessage(fallbackResponse));

            // Act
            String result = service.processChatMessage(TEST_SESSION_ID, USER_MESSAGE);

            // Assert
            assertThat(result).isEqualTo(fallbackResponse);
        }
    }

    @Nested
    @DisplayName("processChatMessage without session ID")
    class ProcessChatMessageWithoutSessionId {

        @Test
        @DisplayName("should return AI response text using default session")
        void shouldReturnAiResponseTextUsingDefaultSession() {
            // Arrange
            ChatMessage assistantMessage = ChatMessage.createAssistantMessage(ASSISTANT_RESPONSE);
            when(sendChatMessageUseCase.executeInDefaultSession(USER_MESSAGE))
                .thenReturn(assistantMessage);

            // Act
            String result = service.processChatMessage(USER_MESSAGE);

            // Assert
            assertThat(result).isEqualTo(ASSISTANT_RESPONSE);
        }

        @Test
        @DisplayName("should delegate to use case with correct parameters")
        void shouldDelegateToUseCaseWithCorrectParameters() {
            // Arrange
            when(sendChatMessageUseCase.executeInDefaultSession(USER_MESSAGE))
                .thenReturn(ChatMessage.createAssistantMessage(ASSISTANT_RESPONSE));

            // Act
            service.processChatMessage(USER_MESSAGE);

            // Assert
            verify(sendChatMessageUseCase).executeInDefaultSession(USER_MESSAGE);
        }

        @Test
        @DisplayName("should always use default session for overloaded method")
        void shouldAlwaysUseDefaultSessionForOverloadedMethod() {
            // Arrange
            String differentMessage = "Different message";
            when(sendChatMessageUseCase.executeInDefaultSession(differentMessage))
                .thenReturn(ChatMessage.createAssistantMessage(ASSISTANT_RESPONSE));

            // Act
            service.processChatMessage(differentMessage);

            // Assert
            verify(sendChatMessageUseCase).executeInDefaultSession(differentMessage);
            verify(sendChatMessageUseCase, never()).execute(any(), any());
        }
    }

    @Nested
    @DisplayName("createSession")
    class CreateSession {

        @Test
        @DisplayName("should create session with given title")
        void shouldCreateSessionWithGivenTitle() {
            // Arrange
            doAnswer(invocation -> invocation.getArgument(0)).when(repositoryPort).save(any(ChatSession.class));

            // Act
            ChatSession result = service.createSession(TEST_TITLE);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(TEST_TITLE);
        }

        @Test
        @DisplayName("should save created session")
        void shouldSaveCreatedSession() {
            // Arrange
            doNothing().when(repositoryPort).save(any(ChatSession.class));

            // Act
            service.createSession(TEST_TITLE);

            // Assert
            verify(repositoryPort).save(any(ChatSession.class));
        }

        @Test
        @DisplayName("should generate unique ID for new session")
        void shouldGenerateUniqueIdForNewSession() {
            // Arrange
            doNothing().when(repositoryPort).save(any(ChatSession.class));

            // Act
            ChatSession session1 = service.createSession(TEST_TITLE);
            ChatSession session2 = service.createSession(TEST_TITLE);

            // Assert
            assertThat(session1.getId()).isNotEqualTo(session2.getId());
        }

        @Test
        @DisplayName("should create session with default title when no title provided")
        void shouldCreateSessionWithDefaultTitleWhenNoTitleProvided() {
            // Arrange
            doNothing().when(repositoryPort).save(any(ChatSession.class));

            // Act
            ChatSession result = service.createSession();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("New Chat");
        }

        @Test
        @DisplayName("should save session created with default title")
        void shouldSaveSessionCreatedWithDefaultTitle() {
            // Arrange
            doNothing().when(repositoryPort).save(any(ChatSession.class));

            // Act
            service.createSession();

            // Assert
            verify(repositoryPort).save(any(ChatSession.class));
        }

        @Test
        @DisplayName("should handle null title by using default")
        void shouldHandleNullTitleByUsingDefault() {
            // Arrange
            doNothing().when(repositoryPort).save(any(ChatSession.class));

            // Act
            ChatSession result = service.createSession(null);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("New Chat");
        }

        @Test
        @DisplayName("should handle empty title by using default")
        void shouldHandleEmptyTitleByUsingDefault() {
            // Arrange
            doNothing().when(repositoryPort).save(any(ChatSession.class));

            // Act
            ChatSession result = service.createSession("");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("New Chat");
        }

        @Test
        @DisplayName("should handle blank title by using default")
        void shouldHandleBlankTitleByUsingDefault() {
            // Arrange
            doNothing().when(repositoryPort).save(any(ChatSession.class));

            // Act
            ChatSession result = service.createSession("   ");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("New Chat");
        }

        @Test
        @DisplayName("should trim whitespace from title")
        void shouldTrimWhitespaceFromTitle() {
            // Arrange
            String titleWithSpaces = "  Test Title  ";
            doNothing().when(repositoryPort).save(any(ChatSession.class));

            // Act
            ChatSession result = service.createSession(titleWithSpaces);

            // Assert
            assertThat(result.getTitle()).isEqualTo("Test Title");
        }
    }

    @Nested
    @DisplayName("getSession")
    class GetSession {

        @Test
        @DisplayName("should return session when found")
        void shouldReturnSessionWhenFound() {
            // Arrange
            ChatSession expectedSession = ChatSession.create(TEST_TITLE);
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(expectedSession));

            // Act
            Optional<ChatSession> result = service.getSession(TEST_SESSION_ID);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expectedSession);
        }

        @Test
        @DisplayName("should return empty optional when session not found")
        void shouldReturnEmptyOptionalWhenSessionNotFound() {
            // Arrange
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.empty());

            // Act
            Optional<ChatSession> result = service.getSession(TEST_SESSION_ID);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should delegate to repository")
        void shouldDelegateToRepository() {
            // Arrange
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.empty());

            // Act
            service.getSession(TEST_SESSION_ID);

            // Assert
            verify(repositoryPort).findById(ChatSessionId.of(TEST_SESSION_ID));
        }
    }

    @Nested
    @DisplayName("getSessionHistory")
    class GetSessionHistory {

        @Test
        @DisplayName("should return message history when session exists")
        void shouldReturnMessageHistoryWhenSessionExists() {
            // Arrange
            ChatSession session = ChatSession.create(TEST_TITLE);
            session.addUserMessage("User message");
            session.addAssistantMessage("Assistant message");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));

            // Act
            List<ChatMessage> result = service.getSessionHistory(TEST_SESSION_ID);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).isFromUser()).isTrue();
            assertThat(result.get(1).isFromAssistant()).isTrue();
        }

        @Test
        @DisplayName("should return empty list for new session")
        void shouldReturnEmptyListForNewSession() {
            // Arrange
            ChatSession session = ChatSession.create(TEST_TITLE);
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));

            // Act
            List<ChatMessage> result = service.getSessionHistory(TEST_SESSION_ID);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when session not found")
        void shouldThrowExceptionWhenSessionNotFound() {
            // Arrange
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.getSessionHistory(TEST_SESSION_ID))
                .isInstanceOf(ChatSessionNotFoundException.class)
                .hasMessageContaining(TEST_SESSION_ID);
        }

        @Test
        @DisplayName("should return messages in chronological order")
        void shouldReturnMessagesInChronologicalOrder() {
            // Arrange
            ChatSession session = ChatSession.create(TEST_TITLE);
            session.addUserMessage("First");
            session.addUserMessage("Second");
            session.addUserMessage("Third");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));

            // Act
            List<ChatMessage> result = service.getSessionHistory(TEST_SESSION_ID);

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getText()).isEqualTo("First");
            assertThat(result.get(1).getText()).isEqualTo("Second");
            assertThat(result.get(2).getText()).isEqualTo("Third");
        }
    }

    @Nested
    @DisplayName("getRecentMessages")
    class GetRecentMessages {

        @Test
        @DisplayName("should return recent messages when session exists")
        void shouldReturnRecentMessagesWhenSessionExists() {
            // Arrange
            ChatSession session = ChatSession.create(TEST_TITLE);
            for (int i = 0; i < 10; i++) {
                session.addUserMessage("Message " + i);
            }
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));

            // Act
            List<ChatMessage> result = service.getRecentMessages(TEST_SESSION_ID, 3);

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getText()).isEqualTo("Message 7");
            assertThat(result.get(1).getText()).isEqualTo("Message 8");
            assertThat(result.get(2).getText()).isEqualTo("Message 9");
        }

        @Test
        @DisplayName("should return all messages when count exceeds total")
        void shouldReturnAllMessagesWhenCountExceedsTotal() {
            // Arrange
            ChatSession session = ChatSession.create(TEST_TITLE);
            session.addUserMessage("Message 1");
            session.addUserMessage("Message 2");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));

            // Act
            List<ChatMessage> result = service.getRecentMessages(TEST_SESSION_ID, 100);

            // Assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when count is zero")
        void shouldReturnEmptyListWhenCountIsZero() {
            // Arrange
            ChatSession session = ChatSession.create(TEST_TITLE);
            session.addUserMessage("Message");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));

            // Act
            List<ChatMessage> result = service.getRecentMessages(TEST_SESSION_ID, 0);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when count is negative")
        void shouldReturnEmptyListWhenCountIsNegative() {
            // Arrange
            ChatSession session = ChatSession.create(TEST_TITLE);
            session.addUserMessage("Message");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));

            // Act
            List<ChatMessage> result = service.getRecentMessages(TEST_SESSION_ID, -5);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when session not found")
        void shouldThrowExceptionWhenSessionNotFound() {
            // Arrange
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.getRecentMessages(TEST_SESSION_ID, 5))
                .isInstanceOf(ChatSessionNotFoundException.class)
                .hasMessageContaining(TEST_SESSION_ID);
        }
    }

    @Nested
    @DisplayName("deleteSession")
    class DeleteSession {

        @Test
        @DisplayName("should delete session by ID")
        void shouldDeleteSessionById() {
            // Arrange
            doNothing().when(repositoryPort).delete(any(ChatSessionId.class));

            // Act
            service.deleteSession(TEST_SESSION_ID);

            // Assert
            verify(repositoryPort).delete(ChatSessionId.of(TEST_SESSION_ID));
        }

        @Test
        @DisplayName("should delegate deletion to repository")
        void shouldDelegateDeletionToRepository() {
            // Arrange
            String sessionId = "another-session-id";
            doNothing().when(repositoryPort).delete(any(ChatSessionId.class));

            // Act
            service.deleteSession(sessionId);

            // Assert
            verify(repositoryPort).delete(ChatSessionId.of(sessionId));
        }
    }

    @Nested
    @DisplayName("getAllSessions")
    class GetAllSessions {

        @Test
        @DisplayName("should return all sessions")
        void shouldReturnAllSessions() {
            // Arrange
            List<ChatSession> sessions = List.of(
                ChatSession.create("Session 1"),
                ChatSession.create("Session 2")
            );
            when(repositoryPort.findAll()).thenReturn(sessions);

            // Act
            List<ChatSession> result = service.getAllSessions();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).isEqualTo(sessions);
        }

        @Test
        @DisplayName("should return empty list when no sessions")
        void shouldReturnEmptyListWhenNoSessions() {
            // Arrange
            when(repositoryPort.findAll()).thenReturn(List.of());

            // Act
            List<ChatSession> result = service.getAllSessions();

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should delegate to repository")
        void shouldDelegateToRepository() {
            // Arrange
            when(repositoryPort.findAll()).thenReturn(List.of());

            // Act
            service.getAllSessions();

            // Assert
            verify(repositoryPort).findAll();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle session with special characters in title")
        void shouldHandleSessionWithSpecialCharactersInTitle() {
            // Arrange
            String specialTitle = "Chat - 2024/01/01 (Test)";
            doNothing().when(repositoryPort).save(any(ChatSession.class));

            // Act
            ChatSession result = service.createSession(specialTitle);

            // Assert
            assertThat(result.getTitle()).isEqualTo(specialTitle);
        }

        @Test
        @DisplayName("should handle long title by truncating")
        void shouldHandleLongTitleByTruncating() {
            // Arrange
            String longTitle = "A".repeat(150);
            doNothing().when(repositoryPort).save(any(ChatSession.class));

            // Act
            ChatSession result = service.createSession(longTitle);

            // Assert
            assertThat(result.getTitle()).hasSize(100);
            assertThat(result.getTitle()).startsWith("A");
        }

        @Test
        @DisplayName("should handle unicode in session title")
        void shouldHandleUnicodeInSessionTitle() {
            // Arrange
            String unicodeTitle = "聊天会话 - 日本語";
            doNothing().when(repositoryPort).save(any(ChatSession.class));

            // Act
            ChatSession result = service.createSession(unicodeTitle);

            // Assert
            assertThat(result.getTitle()).isEqualTo(unicodeTitle);
        }
    }
}
