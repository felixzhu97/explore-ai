package com.ai.domain.service;

import com.ai.domain.model.ChatMessage;
import com.ai.domain.model.ChatSession;
import com.ai.domain.repository.ChatSessionRepository;
import com.ai.domain.vo.ChatSessionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.ai.chat.memory.ChatMemory;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatService")
class AiChatServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatSessionRepository repository;

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private RetryTemplate retryTemplate;

    private AiChatService service;

    private static final String TEST_SESSION_ID = "test-session-id";

    @BeforeEach
    void setUp() {
        lenient().when(chatClientBuilder.build()).thenReturn(chatClient);
        lenient().when(retryTemplate.execute(any())).thenAnswer(inv -> {
            org.springframework.retry.RetryCallback<String, org.springframework.retry.RetryException> callback = inv.getArgument(0);
            return callback.doWithRetry(null);
        });
        service = new AiChatService(chatClientBuilder, repository, retryTemplate, chatMemory);
    }

    @Nested
    @DisplayName("session management")
    class SessionManagement {

        @Test
        @DisplayName("should create session with title")
        void shouldCreateSessionWithTitle() {
            doNothing().when(repository).save(any(ChatSession.class));

            ChatSession result = service.createSession("My Chat");

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("My Chat");
            verify(repository).save(any(ChatSession.class));
        }

        @Test
        @DisplayName("should create session with default title")
        void shouldCreateSessionWithDefaultTitle() {
            doNothing().when(repository).save(any(ChatSession.class));

            ChatSession result = service.createSession();

            assertThat(result.getTitle()).isEqualTo("New Chat");
        }

        @Test
        @DisplayName("should get session by ID")
        void shouldGetSessionById() {
            ChatSession session = ChatSession.create("Test");
            when(repository.findById(ChatSessionId.of(TEST_SESSION_ID)))
                    .thenReturn(Optional.of(session));

            Optional<ChatSession> result = service.getSession(TEST_SESSION_ID);

            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Test");
        }

        @Test
        @DisplayName("should return empty when session not found")
        void shouldReturnEmptyWhenSessionNotFound() {
            when(repository.findById(ChatSessionId.of(TEST_SESSION_ID)))
                    .thenReturn(Optional.empty());

            Optional<ChatSession> result = service.getSession(TEST_SESSION_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should delete session")
        void shouldDeleteSession() {
            service.deleteSession(TEST_SESSION_ID);

            verify(repository).delete(ChatSessionId.of(TEST_SESSION_ID));
        }

        @Test
        @DisplayName("should get all sessions")
        void shouldGetAllSessions() {
            List<ChatSession> sessions = List.of(
                    ChatSession.create("Session 1"),
                    ChatSession.create("Session 2")
            );
            when(repository.findAll()).thenReturn(sessions);

            List<ChatSession> result = service.getAllSessions();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no sessions")
        void shouldReturnEmptyListWhenNoSessions() {
            when(repository.findAll()).thenReturn(List.of());

            List<ChatSession> result = service.getAllSessions();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getChatClient")
    class GetChatClient {

        @Test
        @DisplayName("should return ChatClient instance")
        void shouldReturnChatClientInstance() {
            ChatClient result = service.getChatClient();

            assertThat(result).isEqualTo(chatClient);
        }
    }

    @Nested
    @DisplayName("getSessionHistory")
    class GetSessionHistory {

        @Test
        @DisplayName("should get session history")
        void shouldGetSessionHistory() {
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Hello");
            session.addAssistantMessage("Hi!");
            when(repository.findById(ChatSessionId.of(TEST_SESSION_ID)))
                    .thenReturn(Optional.of(session));

            List<ChatMessage> history = service.getSessionHistory(TEST_SESSION_ID);

            assertThat(history).hasSize(2);
        }

        @Test
        @DisplayName("should throw exception when session not found")
        void shouldThrowExceptionWhenSessionNotFound() {
            when(repository.findById(ChatSessionId.of(TEST_SESSION_ID)))
                    .thenReturn(Optional.empty());

            org.junit.jupiter.api.Assertions.assertThrows(
                    com.ai.domain.model.ChatSessionNotFoundException.class,
                    () -> service.getSessionHistory(TEST_SESSION_ID)
            );
        }
    }
}
