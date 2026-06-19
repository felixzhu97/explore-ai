package com.ai.domain.service;

import com.ai.domain.model.ChatMessage;
import com.ai.domain.model.ChatSession;
import com.ai.domain.model.ChatSessionNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AiChatService Unit Tests
 *
 * Tests covering session management and repository interactions.
 * Note: Chat operations with Spring AI ChatClient are tested via integration tests.
 */
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

    // ============ Session Management Tests ============

    @Nested
    @DisplayName("createSession")
    class CreateSession {

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
        @DisplayName("should create unique session IDs")
        void shouldCreateUniqueSessionIds() {
            doNothing().when(repository).save(any(ChatSession.class));

            ChatSession session1 = service.createSession("Session 1");
            ChatSession session2 = service.createSession("Session 2");

            assertThat(session1.getId()).isNotEqualTo(session2.getId());
        }

        @Test
        @DisplayName("should save session to repository")
        void shouldSaveSessionToRepository() {
            doNothing().when(repository).save(any(ChatSession.class));

            service.createSession("Test");

            verify(repository).save(any(ChatSession.class));
        }
    }

    @Nested
    @DisplayName("getSession")
    class GetSession {

        @Test
        @DisplayName("should return session when exists")
        void shouldReturnSessionWhenExists() {
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
        @DisplayName("should call repository with correct ID")
        void shouldCallRepositoryWithCorrectId() {
            service.getSession(TEST_SESSION_ID);

            verify(repository).findById(ChatSessionId.of(TEST_SESSION_ID));
        }
    }

    @Nested
    @DisplayName("deleteSession")
    class DeleteSession {

        @Test
        @DisplayName("should delete session from repository")
        void shouldDeleteSessionFromRepository() {
            service.deleteSession(TEST_SESSION_ID);

            verify(repository).delete(ChatSessionId.of(TEST_SESSION_ID));
        }
    }

    @Nested
    @DisplayName("getAllSessions")
    class GetAllSessions {

        @Test
        @DisplayName("should return all sessions")
        void shouldReturnAllSessions() {
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

    // ============ Session History Tests ============

    @Nested
    @DisplayName("getSessionHistory")
    class GetSessionHistory {

        @Test
        @DisplayName("should return session messages")
        void shouldReturnSessionMessages() {
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

            assertThatThrownBy(() -> service.getSessionHistory(TEST_SESSION_ID))
                    .isInstanceOf(ChatSessionNotFoundException.class);
        }

        @Test
        @DisplayName("should return empty list for new session")
        void shouldReturnEmptyListForNewSession() {
            ChatSession session = ChatSession.create("Test");
            when(repository.findById(ChatSessionId.of(TEST_SESSION_ID)))
                    .thenReturn(Optional.of(session));

            List<ChatMessage> history = service.getSessionHistory(TEST_SESSION_ID);

            assertThat(history).isEmpty();
        }
    }

    // ============ Recent Messages Tests ============

    @Nested
    @DisplayName("getRecentMessages")
    class GetRecentMessages {

        @Test
        @DisplayName("should return recent messages")
        void shouldReturnRecentMessages() {
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Message 1");
            session.addUserMessage("Message 2");
            session.addUserMessage("Message 3");
            when(repository.findById(ChatSessionId.of(TEST_SESSION_ID)))
                    .thenReturn(Optional.of(session));

            List<ChatMessage> result = service.getRecentMessages(TEST_SESSION_ID, 2);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should throw exception when session not found")
        void shouldThrowExceptionWhenSessionNotFound() {
            when(repository.findById(ChatSessionId.of(TEST_SESSION_ID)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRecentMessages(TEST_SESSION_ID, 5))
                    .isInstanceOf(ChatSessionNotFoundException.class);
        }

        @Test
        @DisplayName("should return all messages when count exceeds total")
        void shouldReturnAllMessagesWhenCountExceedsTotal() {
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Message 1");
            when(repository.findById(ChatSessionId.of(TEST_SESSION_ID)))
                    .thenReturn(Optional.of(session));

            List<ChatMessage> result = service.getRecentMessages(TEST_SESSION_ID, 100);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should return empty for zero count")
        void shouldReturnEmptyForZeroCount() {
            ChatSession session = ChatSession.create("Test");
            session.addUserMessage("Message 1");
            when(repository.findById(ChatSessionId.of(TEST_SESSION_ID)))
                    .thenReturn(Optional.of(session));

            List<ChatMessage> result = service.getRecentMessages(TEST_SESSION_ID, 0);

            assertThat(result).isEmpty();
        }
    }

    // ============ ChatMemory Tests ============

    @Nested
    @DisplayName("clearConversationMemory")
    class ClearConversationMemory {

        @Test
        @DisplayName("should clear conversation memory")
        void shouldClearConversationMemory() {
            service.clearConversationMemory("conversation-123");

            verify(chatMemory).clear("conversation-123");
        }

        @Test
        @DisplayName("should handle different conversation IDs")
        void shouldHandleDifferentConversationIds() {
            service.clearConversationMemory("conv-1");
            service.clearConversationMemory("conv-2");

            verify(chatMemory).clear("conv-1");
            verify(chatMemory).clear("conv-2");
        }
    }

    // ============ ChatClient Access Tests ============

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
}
