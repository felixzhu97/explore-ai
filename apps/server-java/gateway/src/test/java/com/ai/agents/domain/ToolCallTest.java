package com.ai.agents.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ToolCall Tests")
class ToolCallTest {

    @Nested
    @DisplayName("create factory method")
    class CreateFactoryMethodTests {

        @Test
        @DisplayName("should create tool call with tool name and arguments")
        void shouldCreateToolCallWithToolNameAndArguments() {
            ToolCall toolCall = ToolCall.create("search", "query string");

            assertThat(toolCall.toolName()).isEqualTo("search");
            assertThat(toolCall.arguments()).isEqualTo("query string");
            assertThat(toolCall.result()).isNull();
            assertThat(toolCall.durationMs()).isZero();
        }

        @Test
        @DisplayName("should create tool call with map arguments")
        void shouldCreateToolCallWithMapArguments() {
            Map<String, Object> args = new HashMap<>();
            args.put("query", "test");
            args.put("limit", 10);
            ToolCall toolCall = ToolCall.create("search", args);

            assertThat(toolCall.toolName()).isEqualTo("search");
            assertThat(toolCall.arguments()).contains("query");
            assertThat(toolCall.arguments()).contains("test");
        }

        @Test
        @DisplayName("should handle null arguments")
        void shouldHandleNullArguments() {
            ToolCall toolCall = ToolCall.create("search", (String) null);

            assertThat(toolCall.arguments()).isEmpty();
        }

        @Test
        @DisplayName("should generate unique tool call id")
        void shouldGenerateUniqueToolCallId() {
            ToolCall toolCall1 = ToolCall.create("search", "query");
            ToolCall toolCall2 = ToolCall.create("search", "query");

            assertThat(toolCall1.id()).isNotEqualTo(toolCall2.id());
        }
    }

    @Nested
    @DisplayName("withResult method")
    class WithResultMethodTests {

        @Test
        @DisplayName("should add result to tool call")
        void shouldAddResultToToolCall() {
            ToolCall toolCall = ToolCall.create("search", "query");
            ToolResult result = ToolResult.success("Found 10 results");

            ToolCall withResult = toolCall.withResult(result, 100);

            assertThat(withResult.result()).isEqualTo(result);
            assertThat(withResult.durationMs()).isEqualTo(100);
        }

        @Test
        @DisplayName("should create tool call with result content")
        void shouldCreateToolCallWithResultContent() {
            ToolCall toolCall = ToolCall.create("search", "query");

            ToolCall withResult = toolCall.withResult("Result content", true);

            assertThat(withResult.hasResult()).isTrue();
            assertThat(withResult.isSuccess()).isTrue();
            assertThat(withResult.isError()).isFalse();
        }
    }

    @Nested
    @DisplayName("withError method")
    class WithErrorMethodTests {

        @Test
        @DisplayName("should mark tool call as error")
        void shouldMarkToolCallAsError() {
            ToolCall toolCall = ToolCall.create("search", "query");

            ToolCall withError = toolCall.withError("Connection failed");

            assertThat(withError.hasResult()).isTrue();
            assertThat(withError.isSuccess()).isFalse();
            assertThat(withError.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("status checking methods")
    class StatusCheckingMethodTests {

        @Test
        @DisplayName("hasResult should return false for new tool call")
        void hasResultShouldReturnFalseForNewToolCall() {
            ToolCall toolCall = ToolCall.create("search", "query");

            assertThat(toolCall.hasResult()).isFalse();
        }

        @Test
        @DisplayName("hasResult should return true after result is added")
        void hasResultShouldReturnTrueAfterResultIsAdded() {
            ToolCall toolCall = ToolCall.create("search", "query")
                    .withResult("result", true);

            assertThat(toolCall.hasResult()).isTrue();
        }

        @Test
        @DisplayName("isSuccess should return true for successful result")
        void isSuccessShouldReturnTrueForSuccessfulResult() {
            ToolCall toolCall = ToolCall.create("search", "query")
                    .withResult("result", true);

            assertThat(toolCall.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("isError should return true for error result")
        void isErrorShouldReturnTrueForErrorResult() {
            ToolCall toolCall = ToolCall.create("search", "query")
                    .withError("Error occurred");

            assertThat(toolCall.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("ToolCallId inner class")
    class ToolCallIdTests {

        @Test
        @DisplayName("generated ids should be unique")
        void generatedIdsShouldBeUnique() {
            var toolCall1 = ToolCall.create("search", "query1");
            var toolCall2 = ToolCall.create("search", "query2");

            assertThat(toolCall1.id()).isNotEqualTo(toolCall2.id());
        }
    }
}
