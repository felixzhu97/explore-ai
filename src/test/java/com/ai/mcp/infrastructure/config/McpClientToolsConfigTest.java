package com.ai.mcp.infrastructure.config;

import com.ai.mcp.application.port.McpToolCallbackRegistry;
import com.ai.mcp.domain.repository.McpClientRepository;
import io.modelcontextprotocol.client.McpSyncClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("McpClientToolsConfig")
class McpClientToolsConfigTest {
    private final McpClientToolsConfig config = new McpClientToolsConfig();

    @Test
    @DisplayName("should_return_empty_callbacks_when_provider_unavailable")
    void should_return_empty_callbacks_when_provider_unavailable() {
        @SuppressWarnings("unchecked") ObjectProvider<List<McpSyncClient>> clients = mock(ObjectProvider.class);
        when(clients.getIfAvailable(any())).thenReturn(List.of());
        @SuppressWarnings("unchecked") ObjectProvider<ToolCallbackProvider> providers = mock(ObjectProvider.class);
        when(providers.getIfAvailable()).thenReturn(null);
        McpToolCallbackRegistry registry = mock(McpToolCallbackRegistry.class);
        McpClientRepository repository = mock(McpClientRepository.class);
        assertThat(config.mcpToolCallbacks(clients, providers, registry, repository)).isEmpty();
        verify(registry, never()).registerToolCallbacks(any(), any());
    }

    @Test
    @DisplayName("should_return_empty_callbacks_when_provider_throws")
    void should_return_empty_callbacks_when_provider_throws() {
        @SuppressWarnings("unchecked") ObjectProvider<List<McpSyncClient>> clients = mock(ObjectProvider.class);
        when(clients.getIfAvailable(any())).thenReturn(List.of());
        @SuppressWarnings("unchecked") ObjectProvider<ToolCallbackProvider> providers = mock(ObjectProvider.class);
        when(providers.getIfAvailable()).thenThrow(new RuntimeException("MCP unreachable"));
        McpToolCallbackRegistry registry = mock(McpToolCallbackRegistry.class);
        McpClientRepository repository = mock(McpClientRepository.class);
        assertThat(config.mcpToolCallbacks(clients, providers, registry, repository)).isEmpty();
        verify(registry, never()).registerToolCallbacks(any(), any());
    }

    @Test
    @DisplayName("should_register_callbacks_under_mcp_client_when_provider_succeeds")
    void should_register_callbacks_under_mcp_client_when_provider_succeeds() {
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(
                ToolDefinition.builder().name("fetch").description("Fetch URL").inputSchema("{}").build());
        ToolCallbackProvider provider = mock(ToolCallbackProvider.class);
        when(provider.getToolCallbacks()).thenReturn(new ToolCallback[]{callback});
        @SuppressWarnings("unchecked") ObjectProvider<List<McpSyncClient>> clients = mock(ObjectProvider.class);
        when(clients.getIfAvailable(any())).thenReturn(List.of());
        @SuppressWarnings("unchecked") ObjectProvider<ToolCallbackProvider> providers = mock(ObjectProvider.class);
        when(providers.getIfAvailable()).thenReturn(provider);
        McpToolCallbackRegistry registry = mock(McpToolCallbackRegistry.class);
        McpClientRepository repository = mock(McpClientRepository.class);
        ToolCallback[] callbacks = config.mcpToolCallbacks(clients, providers, registry, repository);
        assertThat(callbacks).containsExactly(callback);
        verify(registry).registerToolCallbacks(callbacks, "mcp-client");
        verify(registry, never()).registerToolCallbacks(any(), eq("external-mcp"));
    }
}
