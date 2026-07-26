package com.ai.chat.application.usecase;

import com.ai.chat.domain.exception.ChatSessionNotFoundException;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import com.ai.common.infrastructure.prompt.PromptTemplates;
import com.ai.metrics.application.AiInvocationRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiChatUseCase")
class SpringAiChatUseCaseTest {

    private static final String CLIENT_A = "11111111-1111-1111-1111-111111111111";
    private static final String CLIENT_B = "22222222-2222-2222-2222-222222222222";

    @Mock
    private ChatClientProvider chatClientProvider;

    @Mock
    private ChatSessionRepository repository;

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private ConversationMemoryRepository conversationMemoryRepository;

    @Mock
    private SessionTitleGenerator sessionTitleGenerator;

    @Mock
    private ChatWebSourcesRepository chatWebSourcesRepository;

    @Mock
    private AiInvocationRecorder invocationRecorder;

    private SpringAiChatUseCase useCase;
    private RetryTemplate retryTemplate;

    @BeforeEach
    void setUp() {
        retryTemplate = RetryTemplate.builder()
                .maxAttempts(1)
                .build();

        useCase = new SpringAiChatUseCase(
                chatClientProvider,
                repository,
                retryTemplate,
                chatMemory,
                conversationMemoryRepository,
                sessionTitleGenerator,
                chatWebSourcesRepository,
                new PromptTemplates(),
                invocationRecorder
        );
    }

    @Nested
    @DisplayName("createSession()")
    class CreateSession {

        @Test
        @DisplayName("should create and save session with title and client")
        void should_createAndSaveSession_whenTitleAndClientProvided() {
            doNothing().when(repository).save(any(ChatSession.class));

            ChatSession result = useCase.createSession("My Chat", CLIENT_A);

            assertThat(result.getTitle()).isEqualTo("My Chat");
            assertThat(result.getClientId()).isEqualTo(CLIENT_A);
            verify(repository).save(any(ChatSession.class));
        }
    }

    @Nested
    @DisplayName("getSession()")
    class GetSession {

        @Test
        @DisplayName("should return session when owned by client")
        void should_returnSession_whenOwnedByClient() {
            ChatSession session = ChatSession.create("Test", CLIENT_A);
            when(repository.findByIdAndClientId(ChatSessionId.of("session-123"), CLIENT_A))
                    .thenReturn(Optional.of(session));

            Optional<ChatSession> result = useCase.getSession("session-123", CLIENT_A);

            assertThat(result).isPresent().contains(session);
            verify(conversationMemoryRepository).syncToSession(eq("session-123"), eq(session));
        }

        @Test
        @DisplayName("should return empty when owned by another client")
        void should_returnEmpty_whenOwnedByAnotherClient() {
            when(repository.findByIdAndClientId(ChatSessionId.of("session-123"), CLIENT_B))
                    .thenReturn(Optional.empty());

            Optional<ChatSession> result = useCase.getSession("session-123", CLIENT_B);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getSessionHistory()")
    class GetSessionHistory {

        @Test
        @DisplayName("should return messages for owned session")
        void should_returnMessages_whenSessionOwned() {
            ChatSession session = ChatSession.create("Test", CLIENT_A);
            session.addUserMessage("Hello");
            session.addAssistantMessage("Hi!");
            when(repository.findByIdAndClientId(ChatSessionId.of("session-123"), CLIENT_A))
                    .thenReturn(Optional.of(session));

            List<ChatMessage> history = useCase.getSessionHistory("session-123", CLIENT_A);

            assertThat(history).hasSize(2);
            verify(conversationMemoryRepository).syncToSession(eq("session-123"), eq(session));
        }

        @Test
        @DisplayName("should throw when session not owned")
        void should_throw_whenSessionNotOwned() {
            when(repository.findByIdAndClientId(ChatSessionId.of("non-existent"), CLIENT_A))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.getSessionHistory("non-existent", CLIENT_A))
                    .isInstanceOf(ChatSessionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteSession()")
    class DeleteSession {

        @Test
        @DisplayName("should delete owned session")
        void should_deleteSession_whenOwned() {
            ChatSession session = ChatSession.createWithId(
                    ChatSessionId.of("session-123"), "Test", CLIENT_A);
            when(repository.findByIdAndClientId(ChatSessionId.of("session-123"), CLIENT_A))
                    .thenReturn(Optional.of(session));
            doNothing().when(repository).delete(ChatSessionId.of("session-123"));

            useCase.deleteSession("session-123", CLIENT_A);

            verify(conversationMemoryRepository).clear("session-123");
            verify(chatWebSourcesRepository).deleteByConversationId("session-123");
            verify(repository).delete(ChatSessionId.of("session-123"));
        }

        @Test
        @DisplayName("should throw when deleting another client's session")
        void should_throw_whenDeletingAnotherClientsSession() {
            when(repository.findByIdAndClientId(ChatSessionId.of("session-123"), CLIENT_B))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.deleteSession("session-123", CLIENT_B))
                    .isInstanceOf(ChatSessionNotFoundException.class);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("should erase all sessions for client")
        void should_eraseAllSessions_whenClientRequestsPrivacyDelete() {
            ChatSession owned = ChatSession.createWithId(
                    ChatSessionId.of("session-123"), "Test", CLIENT_A);
            when(repository.findByClientId(CLIENT_A)).thenReturn(List.of(owned));

            useCase.deleteAllSessionsForClient(CLIENT_A);

            verify(conversationMemoryRepository).clear("session-123");
            verify(chatWebSourcesRepository).deleteByConversationId("session-123");
            verify(repository).delete(ChatSessionId.of("session-123"));
        }
    }

    @Nested
    @DisplayName("getSessionsForClient()")
    class GetSessionsForClient {

        @Test
        @DisplayName("should return sessions for client")
        void should_returnSessions_whenClientHasSessions() {
            List<ChatSession> sessions = List.of(
                    ChatSession.create("Session 1", CLIENT_A),
                    ChatSession.create("Session 2", CLIENT_A)
            );
            when(repository.findByClientId(CLIENT_A)).thenReturn(sessions);

            List<ChatSession> result = useCase.getSessionsForClient(CLIENT_A);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when client has no sessions")
        void should_returnEmptyList_whenClientHasNoSessions() {
            when(repository.findByClientId(CLIENT_A)).thenReturn(List.of());

            List<ChatSession> result = useCase.getSessionsForClient(CLIENT_A);

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
}
