package com.ai.chat.application.usecase;

import com.ai.chat.domain.exception.ChatSessionNotFoundException;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.vo.ChatSessionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiChatUseCase")
class SpringAiChatUseCaseTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatSessionRepository repository;

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private SessionTitleGenerator sessionTitleGenerator;

    private SpringAiChatUseCase useCase;
    private RetryTemplate retryTemplate;

    @BeforeEach
    void setUp() {
        retryTemplate = RetryTemplate.builder()
                .maxAttempts(1)
                .build();

        lenient().when(chatClientBuilder.build()).thenReturn(chatClient);

        useCase = new SpringAiChatUseCase(
                chatClientBuilder,
                repository,
                retryTemplate,
                chatMemory,
                sessionTitleGenerator
        );
    }

    @Nested
    @DisplayName("createSession()")
    class CreateSession {

        @Test
        @DisplayName("should create and save session with title")
        void shouldCreateAndSaveSessionWithTitle() {
            doNothing().when(repository).save(any(ChatSession.class));

            ChatSession result = useCase.createSession("My Chat");

            assertThat(result.getTitle()).isEqualTo("My Chat");
            verify(repository).save(any(ChatSession.class));
        }

        @Test
        @DisplayName("should create session with default title")
        void shouldCreateSessionWithDefaultTitle() {
            doNothing().when(repository).save(any(ChatSession.class));

            ChatSession result = useCase.createSession();

            assertThat(result.getTitle()).isEqualTo("New Chat");
            verify(repository).save(any(ChatSession.class));
        }
    }

    @Nested
    @DisplayName("getSession()")
    class GetSession {

        @Test
        @DisplayName("should return session when found")
        void shouldReturnSessionWhenFound() {
            ChatSession session = ChatSession.create("Test");
            when(repository.findById(ChatSessionId.of("session-123")))
                    .thenReturn(Optional.of(session));

            Optional<ChatSession> result = useCase.getSession("session-123");

            assertThat(result).isPresent().contains(session);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            when(repository.findById(ChatSessionId.of("non-existent")))
                    .thenReturn(Optional.empty());

            Optional<ChatSession> result = useCase.getSession("non-existent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getSessionHistory()")
    class GetSessionHistory {

        @Test
        @DisplayName("should return messages for session")
        void shouldReturnMessagesForSession() {
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");
            session.addAssistantMessage("Hi!");
            when(repository.findById(ChatSessionId.of("session-123")))
                    .thenReturn(Optional.of(session));

            List<ChatMessage> history = useCase.getSessionHistory("session-123");

            assertThat(history).hasSize(2);
        }

        @Test
        @DisplayName("should throw exception when session not found")
        void shouldThrowExceptionWhenSessionNotFound() {
            when(repository.findById(ChatSessionId.of("non-existent")))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.getSessionHistory("non-existent"))
                    .isInstanceOf(ChatSessionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getRecentMessages()")
    class GetRecentMessages {

        @Test
        @DisplayName("should return recent messages")
        void shouldReturnRecentMessages() {
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("First");
            session.addUserMessage("Second");
            session.addUserMessage("Third");
            when(repository.findById(ChatSessionId.of("session-123")))
                    .thenReturn(Optional.of(session));

            List<ChatMessage> recent = useCase.getRecentMessages("session-123", 2);

            assertThat(recent).hasSize(2);
        }

        @Test
        @DisplayName("should throw exception when session not found")
        void shouldThrowExceptionWhenSessionNotFound() {
            when(repository.findById(ChatSessionId.of("non-existent")))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.getRecentMessages("non-existent", 5))
                    .isInstanceOf(ChatSessionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteSession()")
    class DeleteSession {

        @Test
        @DisplayName("should delete session from repository")
        void shouldDeleteSessionFromRepository() {
            doNothing().when(repository).delete(ChatSessionId.of("session-123"));

            useCase.deleteSession("session-123");

            verify(repository).delete(ChatSessionId.of("session-123"));
        }
    }

    @Nested
    @DisplayName("getAllSessions()")
    class GetAllSessions {

        @Test
        @DisplayName("should return all sessions")
        void shouldReturnAllSessions() {
            List<ChatSession> sessions = List.of(
                    ChatSession.create("Session 1"),
                    ChatSession.create("Session 2")
            );
            when(repository.findAll()).thenReturn(sessions);

            List<ChatSession> result = useCase.getAllSessions();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no sessions")
        void shouldReturnEmptyListWhenNoSessions() {
            when(repository.findAll()).thenReturn(List.of());

            List<ChatSession> result = useCase.getAllSessions();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("clearConversationMemory()")
    class ClearConversationMemory {

        @Test
        @DisplayName("should clear chat memory")
        void shouldClearChatMemory() {
            doNothing().when(chatMemory).clear("conversation-123");

            useCase.clearConversationMemory("conversation-123");

            verify(chatMemory).clear("conversation-123");
        }
    }

    @Nested
    @DisplayName("getChatClient()")
    class GetChatClient {

        @Test
        @DisplayName("should return underlying ChatClient")
        void shouldReturnUnderlyingChatClient() {
            ChatClient result = useCase.getChatClient();

            assertThat(result).isSameAs(chatClient);
        }
    }
}
