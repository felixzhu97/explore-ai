package com.ai.application.usecase;

import com.ai.application.port.AiChatPort;
import com.ai.application.port.ChatSessionRepositoryPort;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SendChatMessageUseCase Unit Tests
 * 
 * Tests using Mockito to mock external dependencies (ports):
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests chat message flow with AI response
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SendChatMessageUseCase")
class SendChatMessageUseCaseTest {

    @Mock
    private ChatSessionRepositoryPort repositoryPort;

    @Mock
    private AiChatPort aiChatPort;

    private SendChatMessageUseCase useCase;

    private static final String TEST_SESSION_ID = "test-session-id";
    private static final String USER_MESSAGE = "Hello, how are you?";
    private static final String ASSISTANT_RESPONSE = "I'm doing well, thank you!";

    @BeforeEach
    void setUp() {
        useCase = new SendChatMessageUseCase(repositoryPort, aiChatPort);
    }

    @Nested
    @DisplayName("execute with session ID")
    class ExecuteWithSessionId {

        @Test
        @DisplayName("should return assistant message when valid session and message")
        void shouldReturnAssistantMessageWhenValidSessionAndMessage() {
            // Arrange
            ChatSession session = ChatSession.create("Test Session");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));
            when(aiChatPort.chat(USER_MESSAGE)).thenReturn(ASSISTANT_RESPONSE);

            // Act
            ChatMessage result = useCase.execute(TEST_SESSION_ID, USER_MESSAGE);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getText()).isEqualTo(ASSISTANT_RESPONSE);
            assertThat(result.isFromAssistant()).isTrue();
        }

        @Test
        @DisplayName("should add user message to session")
        void shouldAddUserMessageToSession() {
            // Arrange
            ChatSession session = ChatSession.create("Test Session");
            ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));
            when(aiChatPort.chat(USER_MESSAGE)).thenReturn(ASSISTANT_RESPONSE);

            // Act
            useCase.execute(TEST_SESSION_ID, USER_MESSAGE);

            // Assert
            verify(repositoryPort, atLeastOnce()).save(sessionCaptor.capture());
            ChatSession savedSession = sessionCaptor.getValue();
            assertThat(savedSession.getMessages()).isNotEmpty();
        }

        @Test
        @DisplayName("should call AI chat with user message")
        void shouldCallAiChatWithUserMessage() {
            // Arrange
            ChatSession session = ChatSession.create("Test Session");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));
            when(aiChatPort.chat(USER_MESSAGE)).thenReturn(ASSISTANT_RESPONSE);

            // Act
            useCase.execute(TEST_SESSION_ID, USER_MESSAGE);

            // Assert
            verify(aiChatPort).chat(USER_MESSAGE);
        }

        @Test
        @DisplayName("should save session after adding messages")
        void shouldSaveSessionAfterAddingMessages() {
            // Arrange
            ChatSession session = ChatSession.create("Test Session");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));
            when(aiChatPort.chat(USER_MESSAGE)).thenReturn(ASSISTANT_RESPONSE);

            // Act
            useCase.execute(TEST_SESSION_ID, USER_MESSAGE);

            // Assert
            verify(repositoryPort, times(2)).save(any(ChatSession.class));
        }

        @Test
        @DisplayName("should throw ChatSessionNotFoundException when session not found")
        void shouldThrowChatSessionNotFoundExceptionWhenSessionNotFound() {
            // Arrange
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> useCase.execute(TEST_SESSION_ID, USER_MESSAGE))
                .isInstanceOf(ChatSessionNotFoundException.class)
                .hasMessageContaining(TEST_SESSION_ID);
        }

        @Test
        @DisplayName("should not call AI chat when session not found")
        void shouldNotCallAiChatWhenSessionNotFound() {
            // Arrange
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.empty());

            // Act & Assert
            try {
                useCase.execute(TEST_SESSION_ID, USER_MESSAGE);
            } catch (ChatSessionNotFoundException ignored) {
            }

            // Assert
            verify(aiChatPort, never()).chat(any());
        }

        @Test
        @DisplayName("should return message with correct role")
        void shouldReturnMessageWithCorrectRole() {
            // Arrange
            ChatSession session = ChatSession.create("Test Session");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));
            when(aiChatPort.chat(USER_MESSAGE)).thenReturn(ASSISTANT_RESPONSE);

            // Act
            ChatMessage result = useCase.execute(TEST_SESSION_ID, USER_MESSAGE);

            // Assert
            assertThat(result.getRole()).isEqualTo("assistant");
        }

        @Test
        @DisplayName("should handle empty AI response")
        void shouldHandleEmptyAiResponse() {
            // Arrange
            ChatSession session = ChatSession.create("Test Session");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));
            when(aiChatPort.chat(USER_MESSAGE)).thenReturn("placeholder");

            // Act
            ChatMessage result = useCase.execute(TEST_SESSION_ID, USER_MESSAGE);

            // Assert
            assertThat(result.getText()).isEqualTo("placeholder");
            assertThat(result.isFromAssistant()).isTrue();
        }

        @Test
        @DisplayName("should handle long AI response")
        void shouldHandleLongAiResponse() {
            // Arrange - message within 10000 char limit
            String longResponse = "A".repeat(9999);
            ChatSession session = ChatSession.create("Test Session");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));
            when(aiChatPort.chat(USER_MESSAGE)).thenReturn(longResponse);

            // Act
            ChatMessage result = useCase.execute(TEST_SESSION_ID, USER_MESSAGE);

            // Assert
            assertThat(result.getText()).isEqualTo(longResponse);
            assertThat(result.getText()).hasSize(9999);
        }
    }

    @Nested
    @DisplayName("executeInDefaultSession")
    class ExecuteInDefaultSession {

        @Test
        @DisplayName("should return assistant message in default session")
        void shouldReturnAssistantMessageInDefaultSession() {
            // Arrange
            ChatSession defaultSession = ChatSession.create("Default Chat");
            when(repositoryPort.getOrCreateDefaultSession()).thenReturn(defaultSession);
            when(aiChatPort.chat(USER_MESSAGE)).thenReturn(ASSISTANT_RESPONSE);

            // Act
            ChatMessage result = useCase.executeInDefaultSession(USER_MESSAGE);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getText()).isEqualTo(ASSISTANT_RESPONSE);
            assertThat(result.isFromAssistant()).isTrue();
        }

        @Test
        @DisplayName("should create default session when none exists")
        void shouldCreateDefaultSessionWhenNoneExists() {
            // Arrange
            ChatSession newSession = ChatSession.create("Default Chat");
            when(repositoryPort.getOrCreateDefaultSession()).thenReturn(newSession);
            when(aiChatPort.chat(USER_MESSAGE)).thenReturn(ASSISTANT_RESPONSE);

            // Act
            useCase.executeInDefaultSession(USER_MESSAGE);

            // Assert
            verify(repositoryPort).getOrCreateDefaultSession();
        }

        @Test
        @DisplayName("should add user message to default session")
        void shouldAddUserMessageToDefaultSession() {
            // Arrange
            ChatSession defaultSession = ChatSession.create("Default Chat");
            ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
            when(repositoryPort.getOrCreateDefaultSession()).thenReturn(defaultSession);
            when(aiChatPort.chat(USER_MESSAGE)).thenReturn(ASSISTANT_RESPONSE);

            // Act
            useCase.executeInDefaultSession(USER_MESSAGE);

            // Assert
            verify(repositoryPort).save(sessionCaptor.capture());
            assertThat(sessionCaptor.getValue().getMessages()).isNotEmpty();
        }

        @Test
        @DisplayName("should save default session after conversation")
        void shouldSaveDefaultSessionAfterConversation() {
            // Arrange
            ChatSession defaultSession = ChatSession.create("Default Chat");
            when(repositoryPort.getOrCreateDefaultSession()).thenReturn(defaultSession);
            when(aiChatPort.chat(USER_MESSAGE)).thenReturn(ASSISTANT_RESPONSE);

            // Act
            useCase.executeInDefaultSession(USER_MESSAGE);

            // Assert
            verify(repositoryPort).save(any(ChatSession.class));
        }

        @Test
        @DisplayName("should call AI chat with user message")
        void shouldCallAiChatWithUserMessage() {
            // Arrange
            ChatSession defaultSession = ChatSession.create("Default Chat");
            when(repositoryPort.getOrCreateDefaultSession()).thenReturn(defaultSession);
            when(aiChatPort.chat(USER_MESSAGE)).thenReturn(ASSISTANT_RESPONSE);

            // Act
            useCase.executeInDefaultSession(USER_MESSAGE);

            // Assert
            verify(aiChatPort).chat(USER_MESSAGE);
        }

        @Test
        @DisplayName("should return message with assistant role")
        void shouldReturnMessageWithAssistantRole() {
            // Arrange
            ChatSession defaultSession = ChatSession.create("Default Chat");
            when(repositoryPort.getOrCreateDefaultSession()).thenReturn(defaultSession);
            when(aiChatPort.chat(USER_MESSAGE)).thenReturn(ASSISTANT_RESPONSE);

            // Act
            ChatMessage result = useCase.executeInDefaultSession(USER_MESSAGE);

            // Assert
            assertThat(result.isFromAssistant()).isTrue();
            assertThat(result.getRole()).isEqualTo("assistant");
        }

        @Test
        @DisplayName("should handle special characters in message")
        void shouldHandleSpecialCharactersInMessage() {
            // Arrange
            String specialMessage = "Hello! 👋 How are you? 🎉";
            String specialResponse = "Great! 🎊";
            ChatSession defaultSession = ChatSession.create("Default Chat");
            when(repositoryPort.getOrCreateDefaultSession()).thenReturn(defaultSession);
            when(aiChatPort.chat(specialMessage)).thenReturn(specialResponse);

            // Act
            ChatMessage result = useCase.executeInDefaultSession(specialMessage);

            // Assert
            assertThat(result.getText()).isEqualTo(specialResponse);
        }
    }

    @Nested
    @DisplayName("Multi-turn conversation")
    class MultiTurnConversation {

        @Test
        @DisplayName("should maintain message order in session")
        void shouldMaintainMessageOrderInSession() {
            // Arrange
            ChatSession session = ChatSession.create("Test Session");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));
            when(aiChatPort.chat(any())).thenReturn("Response 1", "Response 2");

            // Act
            useCase.execute(TEST_SESSION_ID, "First message");
            useCase.execute(TEST_SESSION_ID, "Second message");

            // Assert
            verify(repositoryPort, atLeast(2)).save(any(ChatSession.class));
            assertThat(session.getMessageCount()).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("should track message count correctly")
        void shouldTrackMessageCountCorrectly() {
            // Arrange
            ChatSession session = ChatSession.create("Test Session");
            ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));
            when(aiChatPort.chat(any())).thenReturn("Response");

            // Act
            ChatMessage result1 = useCase.execute(TEST_SESSION_ID, "User message 1");
            ChatMessage result2 = useCase.execute(TEST_SESSION_ID, "User message 2");

            // Assert
            assertThat(result1.isFromAssistant()).isTrue();
            assertThat(result2.isFromAssistant()).isTrue();
            assertThat(session.getUserMessageCount()).isEqualTo(2);
            assertThat(session.getAssistantMessageCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle very long user message")
        void shouldHandleVeryLongUserMessage() {
            // Arrange - message within 10000 char domain limit
            String longMessage = "A".repeat(9999);
            ChatSession session = ChatSession.create("Test Session");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));
            when(aiChatPort.chat(longMessage)).thenReturn(ASSISTANT_RESPONSE);

            // Act
            ChatMessage result = useCase.execute(TEST_SESSION_ID, longMessage);

            // Assert
            assertThat(result.getText()).isEqualTo(ASSISTANT_RESPONSE);
            verify(aiChatPort).chat(longMessage);
        }

        @Test
        @DisplayName("should handle unicode characters in message")
        void shouldHandleUnicodeCharactersInMessage() {
            // Arrange
            String unicodeMessage = "你好，世界！🌍 Привет мир!";
            ChatSession session = ChatSession.create("Test Session");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));
            when(aiChatPort.chat(unicodeMessage)).thenReturn(ASSISTANT_RESPONSE);

            // Act
            ChatMessage result = useCase.execute(TEST_SESSION_ID, unicodeMessage);

            // Assert
            assertThat(result.getText()).isEqualTo(ASSISTANT_RESPONSE);
            verify(aiChatPort).chat(unicodeMessage);
        }

        @Test
        @DisplayName("should handle AI service returning null response gracefully")
        void shouldHandleAiServiceReturningNullResponseGracefully() {
            // Arrange
            ChatSession session = ChatSession.create("Test Session");
            when(repositoryPort.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));
            when(aiChatPort.chat(USER_MESSAGE)).thenReturn("placeholder");

            // Act
            ChatMessage result = useCase.execute(TEST_SESSION_ID, USER_MESSAGE);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isFromAssistant()).isTrue();
        }
    }
}
