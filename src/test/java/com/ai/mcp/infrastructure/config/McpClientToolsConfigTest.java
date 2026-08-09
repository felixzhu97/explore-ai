package com.ai.mcp.infrastructure.config;

import com.ai.mcp.application.McpToolCallbackRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("McpClientToolsConfig")
class McpClientToolsConfigTest {
    private final McpClientToolsConfig config = new McpClientToolsConfig();

    @Test
    @DisplayName("should return empty callbacks when provider unavailable")
    void shouldReturnEmptyCallbacksWhenProviderUnavailable() {
        @SuppressWarnings("unchecked") ObjectProvider<ToolCallbackProvider> providers = mock(ObjectProvider.class);
        when(providers.getIfAvailable()).thenReturn(null);
        McpToolCallbackRegistry registry = mock(McpToolCallbackRegistry.class);
        assertThat(config.mcpToolCallbacks(providers, registry)).isEmpty();
        verify(registry, never()).registerToolCallbacks(any(), any());
    }

    @Test
    @DisplayName("should return empty callbacks when provider throws")
    void shouldReturnEmptyCallbacksWhenProviderThrows() {
        @SuppressWarnings("unchecked") ObjectProvider<ToolCallbackProvider> providers = mock(ObjectProvider.class);
        when(providers.getIfAvailable()).thenThrow(new RuntimeException("MCP unreachable"));
        McpToolCallbackRegistry registry = mock(McpToolCallbackRegistry.class);
        assertThat(config.mcpToolCallbacks(providers, registry)).isEmpty();
        verify(registry, never()).registerToolCallbacks(any(), any());
    }

    @Test
    @DisplayName("should register callbacks when provider succeeds")
    void shouldRegisterCallbacksWhenProviderSucceeds() {
        ToolCallback callback = mock(ToolCallback.class);
        ToolCallbackProvider provider = mock(ToolCallbackProvider.class);
        when(provider.getToolCallbacks()).thenReturn(new ToolCallback[]{callback});
        @SuppressWarnings("unchecked") ObjectProvider<ToolCallbackProvider> providers = mock(ObjectProvider.class);
        when(providers.getIfAvailable()).thenReturn(provider);
        McpToolCallbackRegistry registry = mock(McpToolCallbackRegistry.class);
        ToolCallback[] callbacks = config.mcpToolCallbacks(providers, registry);
        assertThat(callbacks).containsExactly(callback);
        verify(registry).registerToolCallbacks(callbacks, "external-mcp");
    }
}
