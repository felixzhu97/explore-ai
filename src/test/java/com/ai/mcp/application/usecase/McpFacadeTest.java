package com.ai.mcp.application.usecase;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.mcp.application.port.McpToolCallbackRegistry;
import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.repository.McpClientRepository;
import com.ai.mcp.domain.vo.McpServerConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpFacade")
class McpFacadeTest {

    @Mock
    private McpClientRepository mcpClientRepository;

    @Mock
    private McpToolCallbackRegistry toolCallbackRegistry;

    @Mock
    private ChatClientProvider chatClientProvider;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private ToolCallback toolCallback;

    private McpFacade facade;

    @BeforeEach
    void setUp() {
        facade = new McpFacade(mcpClientRepository, toolCallbackRegistry, chatClientProvider);
    }

    @Test
    @DisplayName("should_delegate_tool_count_to_repository")
    void should_delegate_tool_count_to_repository() {
        when(mcpClientRepository.toolCount()).thenReturn(7);

        assertThat(facade.getTotalToolCount()).isEqualTo(7);
        verify(mcpClientRepository).toolCount();
    }

    @Test
    @DisplayName("should_return_connected_servers_from_repository")
    void should_return_connected_servers_from_repository() {
        Map<String, McpServerConnection> servers = Map.of(
                "weather", McpServerConnection.connected("weather", 2));
        when(mcpClientRepository.listServers()).thenReturn(servers);

        assertThat(facade.getConnectedServers()).isEqualTo(servers);
    }

    @Test
    @DisplayName("should_return_tool_definitions_from_repository")
    void should_return_tool_definitions_from_repository() {
        List<McpToolDefinition> tools = List.of(McpToolDefinition.create("weather", "Weather lookup"));
        when(mcpClientRepository.listTools()).thenReturn(tools);

        assertThat(facade.getToolDefinitions()).isEqualTo(tools);
    }

    @Test
    @DisplayName("should_register_tool_callbacks_through_registry")
    void should_register_tool_callbacks_through_registry() {
        ToolCallback[] callbacks = new ToolCallback[] {toolCallback};

        facade.registerToolCallbacks(callbacks, "weather");

        verify(toolCallbackRegistry).registerToolCallbacks(callbacks, "weather");
    }

    @Test
    @DisplayName("should_clear_tools_through_repository")
    void should_clear_tools_through_repository() {
        facade.clearTools();

        verify(mcpClientRepository).clearTools();
    }

    @Test
    @DisplayName("should_chat_with_registered_tools")
    void should_chat_with_registered_tools() {
        ToolCallback[] callbacks = new ToolCallback[] {toolCallback};
        when(toolCallbackRegistry.getRegisteredToolCallbacks()).thenReturn(callbacks);
        when(chatClientProvider.createStateless(any(TextChatOptions.class))).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user("What is the weather?")).thenReturn(requestSpec);
        when(requestSpec.tools((Object[]) callbacks)).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Sunny");

        String answer = facade.chatWithTools("What is the weather?");

        assertThat(answer).isEqualTo("Sunny");
        verify(chatClientProvider).createStateless(any(TextChatOptions.class));
        verify(requestSpec).tools((Object[]) callbacks);
    }
}
