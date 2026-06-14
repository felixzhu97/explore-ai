package com.ai.infrastructure.config;

import com.ai.application.port.ToolRegistryPort;
import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.infrastructure.adapter.mcp.McpToolCallbackAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link McpServerConfig}.
 * Since {@code mcpToolCallbacks()} and {@code mcpToolCallbackList()} are @Bean factory
 * methods, we invoke them directly with the mock registry — bypassing Spring's
 * {@code @Autowired} parameter resolution (which is equivalent at runtime).
 */
@DisplayName("McpServerConfig")
class McpServerConfigTest {

    private ToolRegistryPort mockRegistry;

    @BeforeEach
    void setUp() {
        mockRegistry = mock(ToolRegistryPort.class);
    }

    private ToolDefinition mockDefinition(String name, String category) {
        return ToolDefinition.atomic(name, "Description for " + name,
                Map.of("type", "object"), category);
    }

    @Nested
    @DisplayName("mcpToolCallbacks()")
    class McpToolCallbacksBean {

        @Test
        @DisplayName("should create provider with zero callbacks when registry is empty")
        void shouldCreateProviderWithZeroCallbacksWhenRegistryIsEmpty() {
            when(mockRegistry.listTools()).thenReturn(List.of());

            McpServerConfig config = new McpServerConfig();
            ToolCallbackProvider provider = config.mcpToolCallbacks(mockRegistry);

            assertThat(provider.getToolCallbacks()).isEmpty();
        }

        @Test
        @DisplayName("should create callbacks for all registered tools")
        void shouldCreateCallbacksForAllRegisteredTools() {
            List<ToolDefinition> tools = List.of(
                    mockDefinition("rag_search", "rag"),
                    mockDefinition("get_cpu", "monitor"),
                    mockDefinition("supervisor_overview", "composite")
            );
            when(mockRegistry.listTools()).thenReturn(tools);

            McpServerConfig config = new McpServerConfig();
            ToolCallbackProvider provider = config.mcpToolCallbacks(mockRegistry);

            assertThat(provider.getToolCallbacks()).hasSize(3);
        }

        @Test
        @DisplayName("should map tool definition name to callback definition name")
        void shouldMapToolDefinitionNameToCallbackDefinitionName() {
            ToolDefinition ragDef = mockDefinition("rag_search", "rag");
            ToolDefinition cpuDef = mockDefinition("get_cpu", "monitor");
            when(mockRegistry.listTools()).thenReturn(List.of(ragDef, cpuDef));

            McpServerConfig config = new McpServerConfig();
            ToolCallbackProvider provider = config.mcpToolCallbacks(mockRegistry);

            List<String> names = List.of(provider.getToolCallbacks()).stream()
                    .map(cb -> cb.getToolDefinition().name())
                    .toList();

            assertThat(names).containsExactlyInAnyOrder("rag_search", "get_cpu");
        }

        @Test
        @DisplayName("should throw ToolExecutionException when registry returns error result")
        void shouldThrowToolExecutionExceptionWhenRegistryReturnsErrorResult() {
            ToolDefinition def = mockDefinition("document_get", "rag");
            when(mockRegistry.listTools()).thenReturn(List.of(def));
            when(mockRegistry.invoke(any(ToolInvocation.class)))
                    .thenReturn(ToolResult.error("Document not found"));

            McpServerConfig config = new McpServerConfig();
            ToolCallbackProvider provider = config.mcpToolCallbacks(mockRegistry);

            ToolCallback callback = provider.getToolCallbacks()[0];

            assertThatThrownBy(() -> callback.call("{\"docId\":\"bad-id\"}"))
                    .isInstanceOf(McpToolCallbackAdapter.ToolExecutionException.class)
                    .hasMessageContaining("Document not found");
        }

        @Test
        @DisplayName("should return adapter for successful tool invocation")
        void shouldReturnAdapterForSuccessfulToolInvocation() {
            ToolDefinition def = mockDefinition("get_memory", "monitor");
            when(mockRegistry.listTools()).thenReturn(List.of(def));
            when(mockRegistry.invoke(any(ToolInvocation.class)))
                    .thenReturn(ToolResult.success("{\"memoryUsed\":8192}"));

            McpServerConfig config = new McpServerConfig();
            ToolCallbackProvider provider = config.mcpToolCallbacks(mockRegistry);

            ToolCallback callback = provider.getToolCallbacks()[0];
            String result = callback.call("{}");

            assertThat(result).isEqualTo("{\"memoryUsed\":8192}");
        }
    }
}
