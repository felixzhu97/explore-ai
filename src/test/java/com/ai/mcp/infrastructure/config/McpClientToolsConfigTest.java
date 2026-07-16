package com.ai.mcp.infrastructure.config;

import com.ai.mcp.application.port.McpToolCallbackRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("McpClientToolsConfig")
class McpClientToolsConfigTest {

    private final McpClientToolsConfig config = new McpClientToolsConfig();

    @Nested
    @DisplayName("mcpToolCallbacks")
    class McpToolCallbacks {

        @Test
        @DisplayName("should_returnEmptyCallbacks_when_providerMissing")
        void should_returnEmptyCallbacks_when_providerMissing() {
            ObjectProvider<ToolCallbackProvider> providers = toolCallbackProviders(null);
            McpToolCallbackRegistry registry = mock(McpToolCallbackRegistry.class);

            ToolCallback[] callbacks = config.mcpToolCallbacks(providers, registry);

            assertThat(callbacks).isEmpty();
            verifyNoInteractions(registry);
        }

        @Test
        @DisplayName("should_returnEmptyCallbacks_when_providerHasNoTools")
        void should_returnEmptyCallbacks_when_providerHasNoTools() {
            ToolCallbackProvider provider = mock(ToolCallbackProvider.class);
            when(provider.getToolCallbacks()).thenReturn(new ToolCallback[0]);
            McpToolCallbackRegistry registry = mock(McpToolCallbackRegistry.class);

            ToolCallback[] callbacks = config.mcpToolCallbacks(toolCallbackProviders(provider), registry);

            assertThat(callbacks).isEmpty();
            verifyNoInteractions(registry);
        }

        @Test
        @DisplayName("should_registerExternalMcpCallbacks_when_providerHasTools")
        void should_registerExternalMcpCallbacks_when_providerHasTools() {
            ToolCallback fetch = callback("fetch");
            ToolCallbackProvider provider = mock(ToolCallbackProvider.class);
            when(provider.getToolCallbacks()).thenReturn(new ToolCallback[]{fetch});
            McpToolCallbackRegistry registry = mock(McpToolCallbackRegistry.class);

            ToolCallback[] callbacks = config.mcpToolCallbacks(toolCallbackProviders(provider), registry);

            assertThat(callbacks).containsExactly(fetch);
            verify(registry).registerToolCallbacks(callbacks, "external-mcp");
        }
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ToolCallbackProvider> toolCallbackProviders(ToolCallbackProvider provider) {
        ObjectProvider<ToolCallbackProvider> providers = mock(ObjectProvider.class);
        when(providers.getIfAvailable()).thenReturn(provider);
        return providers;
    }

    private static ToolCallback callback(String name) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder()
                        .name(name)
                        .description(name + " external tool")
                        .inputSchema("{}")
                        .build();
            }

            @Override
            public String call(String toolInput) {
                return toolInput;
            }
        };
    }
}
