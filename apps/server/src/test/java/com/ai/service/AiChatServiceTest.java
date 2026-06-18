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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatService")
class AiChatServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private ChatSessionRepository repository;

    private AiChatService service;

    private static final String TEST_SESSION_ID = "test-session-id";
    private static final String USER_MESSAGE = "Hello!";
    private static final String ASSISTANT_RESPONSE = "Hi there!";

    @BeforeEach
    void setUp() {
        service = new AiChatService(chatModel, repository);
    }

    private ChatResponse createMockChatResponse(String text) {
        String responseText = text != null ? text : "";
        AssistantMessage assistantMessage = new AssistantMessage(responseText);
        Generation generation = new Generation(assistantMessage);
        return new ChatResponse(List.of(generation));
    }

    @Nested
    @DisplayName("chat")
    class Chat {

        @Test
        @DisplayName("should return AI response")
        void shouldReturnAiResponse() {
            when(chatModel.call(any(Prompt.class)))
                .thenAnswer(inv -> createMockChatResponse(ASSISTANT_RESPONSE));

            String result = service.chat(USER_MESSAGE);

            assertThat(result).isEqualTo(ASSISTANT_RESPONSE);
            verify(chatModel).call(any(Prompt.class));
        }

        @Test
        @DisplayName("should throw exception when AI fails")
        void shouldThrowExceptionWhenAiFails() {
            when(chatModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("AI unavailable"));

            assertThatThrownBy(() -> service.chat(USER_MESSAGE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI service error");
        }
    }

    @Nested
    @DisplayName("chatWithHistory")
    class ChatWithHistory {

        @Test
        @DisplayName("should return AI response with history")
        void shouldReturnAiResponseWithHistory() {
            List<ChatMessage> messages = List.of(
                ChatMessage.createUserMessage("Hello"),
                ChatMessage.createAssistantMessage("Hi!")
            );
            when(chatModel.call(any(Prompt.class)))
                .thenAnswer(inv -> createMockChatResponse(ASSISTANT_RESPONSE));

            String result = service.chatWithHistory(messages);

            assertThat(result).isEqualTo(ASSISTANT_RESPONSE);
        }
    }

    @Nested
    @DisplayName("processChatMessage with session")
    class ProcessChatMessageWithSession {

        @Test
        @DisplayName("should process message and save session")
        void shouldProcessMessageAndSaveSession() {
            ChatSession session = ChatSession.create("Test");
            when(repository.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.of(session));
            when(chatModel.call(any(Prompt.class)))
                .thenAnswer(inv -> createMockChatResponse(ASSISTANT_RESPONSE));

            String result = service.processChatMessage(TEST_SESSION_ID, USER_MESSAGE);

            assertThat(result).isEqualTo(ASSISTANT_RESPONSE);
            verify(repository, times(2)).save(any(ChatSession.class));
        }

        @Test
        @DisplayName("should fallback to default session when session not found")
        void shouldFallbackToDefaultSessionWhenSessionNotFound() {
            ChatSession defaultSession = ChatSession.create("Default");
            when(repository.findById(ChatSessionId.of(TEST_SESSION_ID)))
                .thenReturn(Optional.empty());
            when(repository.getOrCreateDefaultSession()).thenReturn(defaultSession);
            when(chatModel.call(any(Prompt.class)))
                .thenAnswer(inv -> createMockChatResponse(ASSISTANT_RESPONSE));

            String result = service.processChatMessage(TEST_SESSION_ID, USER_MESSAGE);

            assertThat(result).isEqualTo(ASSISTANT_RESPONSE);
        }
    }

    @Nested
    @DisplayName("processChatMessage without session")
    class ProcessChatMessageWithoutSession {

        @Test
        @DisplayName("should use default session")
        void shouldUseDefaultSession() {
            ChatSession defaultSession = ChatSession.create("Default");
            when(repository.getOrCreateDefaultSession()).thenReturn(defaultSession);
            when(chatModel.call(any(Prompt.class)))
                .thenAnswer(inv -> createMockChatResponse(ASSISTANT_RESPONSE));

            String result = service.processChatMessage(USER_MESSAGE);

            assertThat(result).isEqualTo(ASSISTANT_RESPONSE);
            verify(repository).save(any(ChatSession.class));
        }
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
    }
}
