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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.retry.support.RetryTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * AiChatService Unit Tests
 *
 * Tests session management, repository operations, and ChatMemory.
 * Note: chat() and chatWithHistory() methods require Spring AI integration tests
 * as they depend on ChatClient's fluent API which is difficult to mock.
 */
@DisplayName("AiChatService")
class AiChatServiceTest {

    private AiChatService service;
    private TestChatMemoryRepository repository;
    private MockChatMemory mockChatMemory;
    private ChatClient mockChatClient;
    private ChatClient.Builder mockChatClientBuilder;

    @BeforeEach
    void setUp() {
        repository = new TestChatMemoryRepository();
        mockChatMemory = new MockChatMemory();
        mockChatClient = mock(ChatClient.class);
        mockChatClientBuilder = mock(ChatClient.Builder.class);
        
        when(mockChatClientBuilder.build()).thenReturn(mockChatClient);
        when(mockChatClient.prompt()).thenReturn(null);

        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)
                .fixedBackoff(10)
                .build();

        service = new AiChatService(
                mockChatClientBuilder,
                repository,
                retryTemplate,
                mockChatMemory
        );
    }

    // ============ Session Management Tests ============

    @Nested
    @DisplayName("createSession")
    class CreateSession {

        @Test
        @DisplayName("should create session with title")
        void shouldCreateSessionWithTitle() {
            ChatSession result = service.createSession("My Chat");

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("My Chat");
        }

        @Test
        @DisplayName("should create session with default title")
        void shouldCreateSessionWithDefaultTitle() {
            ChatSession result = service.createSession();

            assertThat(result.getTitle()).isEqualTo("New Chat");
        }

        @Test
        @DisplayName("should create unique session IDs")
        void shouldCreateUniqueSessionIds() {
            ChatSession session1 = service.createSession("Session 1");
            ChatSession session2 = service.createSession("Session 2");

            assertThat(session1.getId()).isNotEqualTo(session2.getId());
        }

        @Test
        @DisplayName("should save session to repository")
        void shouldSaveSessionToRepository() {
            ChatSession session = service.createSession("Test");

            assertThat(repository.findAll()).contains(session);
        }

        @Test
        @DisplayName("should use default title for empty string")
        void shouldUseDefaultTitleForEmptyString() {
            ChatSession result = service.createSession("");

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("New Chat");
        }

        @Test
        @DisplayName("should create session with special characters in title")
        void shouldCreateSessionWithSpecialCharactersInTitle() {
            ChatSession result = service.createSession("Test <>&\"'");

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Test <>&\"'");
        }
    }

    @Nested
    @DisplayName("getSession")
    class GetSession {

        @Test
        @DisplayName("should return session when exists")
        void shouldReturnSessionWhenExists() {
            ChatSession created = service.createSession("Test");

            Optional<ChatSession> result = service.getSession(created.getId().value());

            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Test");
        }

        @Test
        @DisplayName("should return empty when session not found")
        void shouldReturnEmptyWhenSessionNotFound() {
            Optional<ChatSession> result = service.getSession("non-existent-id");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should find session by ID with UUID format")
        void shouldFindSessionByIdWithUuidFormat() {
            ChatSession session = service.createSession("Test");
            String uuidStr = session.getId().value();

            Optional<ChatSession> result = service.getSession(uuidStr);

            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("deleteSession")
    class DeleteSession {

        @Test
        @DisplayName("should delete session from repository")
        void shouldDeleteSessionFromRepository() {
            ChatSession session = service.createSession("Test");
            String sessionId = session.getId().value();

            service.deleteSession(sessionId);

            assertThat(service.getSession(sessionId)).isEmpty();
        }

        @Test
        @DisplayName("should not throw when deleting non-existent session")
        void shouldNotThrowWhenDeletingNonExistentSession() {
            service.deleteSession("non-existent-id");
        }

        @Test
        @DisplayName("should allow recreation after deletion")
        void shouldAllowRecreationAfterDeletion() {
            ChatSession session1 = service.createSession("Test");
            service.deleteSession(session1.getId().value());

            ChatSession session2 = service.createSession("Test");

            assertThat(session1.getId()).isNotEqualTo(session2.getId());
        }
    }

    @Nested
    @DisplayName("getAllSessions")
    class GetAllSessions {

        @Test
        @DisplayName("should return all sessions")
        void shouldReturnAllSessions() {
            service.createSession("Session 1");
            service.createSession("Session 2");

            List<ChatSession> result = service.getAllSessions();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no sessions")
        void shouldReturnEmptyListWhenNoSessions() {
            List<ChatSession> result = service.getAllSessions();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return sessions in insertion order")
        void shouldReturnSessionsInInsertionOrder() {
            ChatSession s1 = service.createSession("First");
            ChatSession s2 = service.createSession("Second");

            List<ChatSession> result = service.getAllSessions();

            assertThat(result.get(0).getTitle()).isEqualTo("First");
            assertThat(result.get(1).getTitle()).isEqualTo("Second");
        }
    }

    // ============ Session History Tests ============

    @Nested
    @DisplayName("getSessionHistory")
    class GetSessionHistory {

        @Test
        @DisplayName("should return session messages")
        void shouldReturnSessionMessages() {
            ChatSession session = service.createSession("Test");
            String sessionId = session.getId().value();
            session.addUserMessage("Hello");
            session.addAssistantMessage("Hi!");
            repository.save(session);

            List<ChatMessage> history = service.getSessionHistory(sessionId);

            assertThat(history).hasSize(2);
        }

        @Test
        @DisplayName("should throw exception when session not found")
        void shouldThrowExceptionWhenSessionNotFound() {
            assertThatThrownBy(() -> service.getSessionHistory("non-existent"))
                    .isInstanceOf(ChatSessionNotFoundException.class);
        }

        @Test
        @DisplayName("should return empty list for new session")
        void shouldReturnEmptyListForNewSession() {
            ChatSession session = service.createSession("Test");
            String sessionId = session.getId().value();

            List<ChatMessage> history = service.getSessionHistory(sessionId);

            assertThat(history).isEmpty();
        }

        @Test
        @DisplayName("should return messages in chronological order")
        void shouldReturnMessagesInChronologicalOrder() {
            ChatSession session = service.createSession("Test");
            String sessionId = session.getId().value();
            session.addUserMessage("First");
            session.addUserMessage("Second");
            repository.save(session);

            List<ChatMessage> history = service.getSessionHistory(sessionId);

            assertThat(history).hasSize(2);
            assertThat(history.get(0).getText()).isEqualTo("First");
        }
    }

    // ============ Recent Messages Tests ============

    @Nested
    @DisplayName("getRecentMessages")
    class GetRecentMessages {

        @Test
        @DisplayName("should return recent messages")
        void shouldReturnRecentMessages() {
            ChatSession session = service.createSession("Test");
            String sessionId = session.getId().value();
            addMessages(session, 5);
            repository.save(session);

            List<ChatMessage> result = service.getRecentMessages(sessionId, 2);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should throw exception when session not found")
        void shouldThrowExceptionWhenSessionNotFound() {
            assertThatThrownBy(() -> service.getRecentMessages("non-existent", 5))
                    .isInstanceOf(ChatSessionNotFoundException.class);
        }

        @Test
        @DisplayName("should return all messages when count exceeds total")
        void shouldReturnAllMessagesWhenCountExceedsTotal() {
            ChatSession session = service.createSession("Test");
            String sessionId = session.getId().value();
            addMessages(session, 3);
            repository.save(session);

            List<ChatMessage> result = service.getRecentMessages(sessionId, 100);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("should return empty for zero count")
        void shouldReturnEmptyForZeroCount() {
            ChatSession session = service.createSession("Test");
            String sessionId = session.getId().value();
            addMessages(session, 3);
            repository.save(session);

            List<ChatMessage> result = service.getRecentMessages(sessionId, 0);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return single message")
        void shouldReturnSingleMessage() {
            ChatSession session = service.createSession("Test");
            String sessionId = session.getId().value();
            session.addUserMessage("Single");
            repository.save(session);

            List<ChatMessage> result = service.getRecentMessages(sessionId, 1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getText()).isEqualTo("Single");
        }

        private void addMessages(ChatSession session, int count) {
            for (int i = 0; i < count; i++) {
                session.addUserMessage("Message " + i);
            }
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

            assertThat(mockChatMemory.clearedIds).contains("conversation-123");
        }

        @Test
        @DisplayName("should handle multiple conversation IDs")
        void shouldHandleMultipleConversationIds() {
            service.clearConversationMemory("conv-1");
            service.clearConversationMemory("conv-2");

            assertThat(mockChatMemory.clearedIds).containsExactly("conv-1", "conv-2");
        }

        @Test
        @DisplayName("should clear with empty string")
        void shouldClearWithEmptyString() {
            service.clearConversationMemory("");

            assertThat(mockChatMemory.clearedIds).contains("");
        }
    }

    // ============ GetChatClient Tests ============

    @Nested
    @DisplayName("getChatClient")
    class GetChatClient {

        @Test
        @DisplayName("should return ChatClient instance")
        void shouldReturnChatClientInstance() {
            ChatClient result = service.getChatClient();

            assertThat(result).isNotNull();
            assertThat(result).isSameAs(mockChatClient);
        }
    }

    // ============ Helper Classes ============

    private static class MockChatMemory implements ChatMemory {
        List<String> clearedIds = new ArrayList<>();

        @Override
        public void add(String conversationId, List<org.springframework.ai.chat.messages.Message> messages) {
        }

        @Override
        public List<org.springframework.ai.chat.messages.Message> get(String conversationId) {
            return List.of();
        }

        @Override
        public void clear(String conversationId) {
            clearedIds.add(conversationId);
        }
    }

    private static class TestChatMemoryRepository implements ChatSessionRepository {
        private final Map<ChatSessionId, ChatSession> sessions = new LinkedHashMap<>();
        private ChatSession defaultSession;

        @Override
        public Optional<ChatSession> findById(ChatSessionId id) {
            return Optional.ofNullable(sessions.get(id));
        }

        @Override
        public void save(ChatSession session) {
            sessions.put(session.getId(), session);
        }

        @Override
        public void delete(ChatSessionId id) {
            sessions.remove(id);
        }

        @Override
        public List<ChatSession> findAll() {
            return new ArrayList<>(sessions.values());
        }

        @Override
        public boolean exists(ChatSessionId id) {
            return sessions.containsKey(id);
        }

        @Override
        public ChatSession getOrCreateDefaultSession() {
            if (defaultSession == null) {
                defaultSession = ChatSession.create("Default Session");
                save(defaultSession);
            }
            return defaultSession;
        }
    }
}
