package com.ai.infrastructure.adapter.tool.chat;

import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.domain.model.ChatSession;
import com.ai.infrastructure.adapter.persistence.InMemoryChatSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ListSessionsTool")
class ListSessionsToolTest {

    private InMemoryChatSessionRepository repository;
    private ListSessionsTool tool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository = new InMemoryChatSessionRepository();
        objectMapper = new ObjectMapper();
        tool = new ListSessionsTool(repository, objectMapper);
    }

    @Nested
    @DisplayName("definition")
    class Definition {

        @Test
        @DisplayName("should have correct tool name")
        void shouldHaveCorrectToolName() {
            assertThat(tool.definition().name()).isEqualTo("list_sessions");
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
        @DisplayName("should return empty list when no sessions")
        void shouldReturnEmptyListWhenNoSessions() {
            ToolResult result = tool.execute(new ToolInvocation("list_sessions", Map.of()));

            assertThat(result.isError()).isFalse();
            assertThat(result.structured()).containsEntry("total", 0);
        }

        @Test
        @DisplayName("should return list of sessions")
        void shouldReturnListOfSessions() {
            ChatSession session1 = ChatSession.create("Session 1");
            ChatSession session2 = ChatSession.create("Session 2");
            repository.save(session1);
            repository.save(session2);

            ToolResult result = tool.execute(new ToolInvocation("list_sessions", Map.of()));

            assertThat(result.isError()).isFalse();
            assertThat(result.structured()).containsEntry("total", 2);
            assertThat(result.structured()).containsKey("sessions");
        }

        @Test
        @DisplayName("should support pagination parameters")
        void shouldSupportPaginationParameters() {
            for (int i = 0; i < 10; i++) {
                ChatSession session = ChatSession.create("Session " + i);
                repository.save(session);
            }

            ToolResult result = tool.execute(new ToolInvocation("list_sessions",
                Map.of("limit", 5, "offset", 0)));

            assertThat(result.isError()).isFalse();
            assertThat(result.structured()).containsEntry("limit", 5);
            assertThat(result.structured()).containsEntry("offset", 0);
        }
    }
}
