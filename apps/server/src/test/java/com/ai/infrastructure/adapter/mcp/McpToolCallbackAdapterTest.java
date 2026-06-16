package com.ai.infrastructure.adapter.mcp;

import com.ai.application.port.ToolRegistryPort;
import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("McpToolCallbackAdapter")
class McpToolCallbackAdapterTest {

    private ToolRegistryPort mockRegistry;

    @BeforeEach
    void setUp() {
        mockRegistry = mock(ToolRegistryPort.class);
    }

    private ToolDefinition sampleDefinition(String name) {
        Map<String, Object> props = Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of("type", "string", "description", "The search query")
                )
        );
        return ToolDefinition.atomic(name, "Test tool for " + name, props, "test");
    }

    @Nested
    @DisplayName("getToolDefinition")
    class GetToolDefinition {

        @Test
        @DisplayName("should return definition with correct name")
        void shouldReturnDefinitionWithCorrectName() {
            ToolDefinition def = sampleDefinition("rag_search");
            McpToolCallbackAdapter adapter = new McpToolCallbackAdapter(def, mockRegistry);

            assertThat(adapter.getToolDefinition().name()).isEqualTo("rag_search");
        }

        @Test
        @DisplayName("should return definition with correct description")
        void shouldReturnDefinitionWithCorrectDescription() {
            ToolDefinition def = sampleDefinition("rag_search");
            McpToolCallbackAdapter adapter = new McpToolCallbackAdapter(def, mockRegistry);

            assertThat(adapter.getToolDefinition().description()).isEqualTo("Test tool for rag_search");
        }

        @Test
        @DisplayName("should serialize inputSchema as JSON string")
        void shouldSerializeInputSchemaAsJson() {
            Map<String, Object> props = Map.of(
                    "query", Map.of("type", "string", "description", "The search query")
            );
            Map<String, Object> schema = Map.of(
                    "type", "object",
                    "properties", props
            );
            ToolDefinition def = ToolDefinition.atomic("test_tool", "A test tool", schema, "test");
            McpToolCallbackAdapter adapter = new McpToolCallbackAdapter(def, mockRegistry);

            String schemaJson = adapter.getToolDefinition().inputSchema();

            assertThat(schemaJson).contains("\"type\":\"object\"");
            assertThat(schemaJson).contains("\"properties\"");
        }

        @Test
        @DisplayName("should return empty schema for empty inputSchema")
        void shouldReturnEmptySchemaForEmptyInputSchema() {
            ToolDefinition def = ToolDefinition.atomic("composite_tool", "A composite tool",
                    Map.of(), "composite");
            McpToolCallbackAdapter adapter = new McpToolCallbackAdapter(def, mockRegistry);

            assertThat(adapter.getToolDefinition().inputSchema()).isEqualTo("{}");
        }
    }

    @Nested
    @DisplayName("call")
    class Call {

        @Test
        @DisplayName("should parse JSON input and return tool result content")
        void shouldParseJsonInputAndReturnToolResultContent() {
            ToolDefinition def = sampleDefinition("rag_search");
            when(mockRegistry.invoke(any(ToolInvocation.class)))
                    .thenReturn(ToolResult.success("Search results: chunk1, chunk2"));

            McpToolCallbackAdapter adapter = new McpToolCallbackAdapter(def, mockRegistry);

            String result = adapter.call("{\"query\":\"transformer\",\"topK\":3}");

            assertThat(result).isEqualTo("Search results: chunk1, chunk2");
            verify(mockRegistry).invoke(argThat(inv ->
                    inv.toolName().equals("rag_search") &&
                    "transformer".equals(inv.getArg("query", "")) &&
                    3 == inv.getArg("topK", 0)
            ));
        }

        @Test
        @DisplayName("should handle null input as empty args")
        void shouldHandleNullInputAsEmptyArgs() {
            ToolDefinition def = sampleDefinition("document_list");
            when(mockRegistry.invoke(any(ToolInvocation.class)))
                    .thenReturn(ToolResult.success("[{\"docId\":\"doc1\"}]"));

            McpToolCallbackAdapter adapter = new McpToolCallbackAdapter(def, mockRegistry);

            String result = adapter.call(null);

            assertThat(result).isEqualTo("[{\"docId\":\"doc1\"}]");
            verify(mockRegistry).invoke(argThat(inv ->
                    inv.toolName().equals("document_list") &&
                    inv.arguments().isEmpty()
            ));
        }

        @Test
        @DisplayName("should handle empty string input as empty args")
        void shouldHandleEmptyStringInputAsEmptyArgs() {
            ToolDefinition def = sampleDefinition("get_cpu");
            when(mockRegistry.invoke(any(ToolInvocation.class)))
                    .thenReturn(ToolResult.success("{\"cpu\":90.5}"));

            McpToolCallbackAdapter adapter = new McpToolCallbackAdapter(def, mockRegistry);

            String result = adapter.call("  ");

            assertThat(result).isEqualTo("{\"cpu\":90.5}");
            verify(mockRegistry).invoke(argThat(inv ->
                    inv.arguments().isEmpty()
            ));
        }

        @Test
        @DisplayName("should throw ToolExecutionException when result is error")
        void shouldThrowToolExecutionExceptionWhenResultIsError() {
            ToolDefinition def = sampleDefinition("document_get");
            when(mockRegistry.invoke(any(ToolInvocation.class)))
                    .thenReturn(ToolResult.error("Document not found: invalid-uuid"));

            McpToolCallbackAdapter adapter = new McpToolCallbackAdapter(def, mockRegistry);

            assertThatThrownBy(() -> adapter.call("{\"docId\":\"invalid-uuid\"}"))
                    .isInstanceOf(McpToolCallbackAdapter.ToolExecutionException.class)
                    .hasMessageContaining("Document not found: invalid-uuid")
                    .hasMessageContaining("document_get");
        }

        @Test
        @DisplayName("should throw ToolExecutionException when registry returns error result")
        void shouldThrowToolExecutionExceptionWhenRegistryReturnsErrorResult() {
            ToolDefinition def = sampleDefinition("web_search");
            when(mockRegistry.invoke(any(ToolInvocation.class)))
                    .thenReturn(ToolResult.error("Network error"));

            McpToolCallbackAdapter adapter = new McpToolCallbackAdapter(def, mockRegistry);

            assertThatThrownBy(() -> adapter.call("{\"query\":\"test\"}"))
                    .isInstanceOf(McpToolCallbackAdapter.ToolExecutionException.class)
                    .hasMessageContaining("web_search")
                    .hasMessageContaining("Network error");
        }

        @Test
        @DisplayName("should throw ToolExecutionException for malformed JSON input")
        void shouldThrowToolExecutionExceptionForMalformedJsonInput() {
            ToolDefinition def = sampleDefinition("rag_search");
            McpToolCallbackAdapter adapter = new McpToolCallbackAdapter(def, mockRegistry);

            assertThatThrownBy(() -> adapter.call("{ invalid json }"))
                    .isInstanceOf(McpToolCallbackAdapter.ToolExecutionException.class)
                    .hasMessageContaining("Failed to parse tool input JSON");
        }

        @Test
        @DisplayName("should ignore ToolContext parameter")
        void shouldIgnoreToolContextParameter() {
            ToolDefinition def = sampleDefinition("get_memory");
            when(mockRegistry.invoke(any(ToolInvocation.class)))
                    .thenReturn(ToolResult.success("{\"memory\":8192}"));

            McpToolCallbackAdapter adapter = new McpToolCallbackAdapter(def, mockRegistry);

            String result = adapter.call("{}", null);

            assertThat(result).isEqualTo("{\"memory\":8192}");
        }
    }
}
