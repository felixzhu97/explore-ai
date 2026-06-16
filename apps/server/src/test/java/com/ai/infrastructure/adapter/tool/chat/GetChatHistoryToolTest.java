package com.ai.infrastructure.adapter.tool.chat;

import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.domain.model.ChatMessage;
import com.ai.domain.model.ChatSession;
import com.ai.domain.vo.ChatSessionId;
import com.ai.infrastructure.adapter.persistence.InMemoryChatSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetChatHistoryTool")
class GetChatHistoryToolTest {

    private InMemoryChatSessionRepository repository;
    private GetChatHistoryTool tool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository = new InMemoryChatSessionRepository();
        objectMapper = new ObjectMapper();
        tool = new GetChatHistoryTool(repository, objectMapper);
    }

    @Nested
    @DisplayName("definition")
    class Definition {

        @Test
        @DisplayName("should have correct tool name")
        void shouldHaveCorrectToolName() {
            assertThat(tool.definition().name()).isEqualTo("get_chat_history");
        }

        @Test
        @DisplayName("should have chat category")
        void shouldHaveChatCategory() {
            assertThat(tool.definition().category()).isEqualTo("chat");
        }
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("should return error for missing sessionId")
        void shouldReturnErrorForMissingSessionId() {
            ToolResult result = tool.execute(new ToolInvocation("get_chat_history",
                Map.of("sessionId", "")));

            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("required");
        }

        @Test
        @DisplayName("should return error for non-existent session")
        void shouldReturnErrorForNonExistentSession() {
            ToolResult result = tool.execute(new ToolInvocation("get_chat_history",
                Map.of("sessionId", "non-existent-id")));

            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("not found");
        }

        @Test
        @DisplayName("should return chat history for existing session")
        void shouldReturnChatHistoryForExistingSession() {
            ChatSession session = ChatSession.create("Test Session");
            session.addUserMessage("Hello");
            session.addAssistantMessage("Hi there!");
            repository.save(session);

            ToolResult result = tool.execute(new ToolInvocation("get_chat_history",
                Map.of("sessionId", session.getId().value())));

            assertThat(result.isError()).isFalse();
            assertThat(result.structured()).containsKey("messages");
            assertThat(result.structured()).containsEntry("sessionId", session.getId().value());
        }

        @Test
        @DisplayName("should support limit parameter")
        void shouldSupportLimitParameter() {
            ChatSession session = ChatSession.create("Test Session");
            for (int i = 0; i < 10; i++) {
                session.addUserMessage("Message " + i);
            }
            repository.save(session);

            ToolResult result = tool.execute(new ToolInvocation("get_chat_history",
                Map.of("sessionId", session.getId().value(), "limit", 5)));

            assertThat(result.isError()).isFalse();
            assertThat(result.structured()).containsKey("returnedMessages");
        }
    }
}
