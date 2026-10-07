package com.ai.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.mcp.domain.model.McpServerConnection;
import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.repository.McpClientGateway;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpService")
class McpServiceTest {

  @Mock private McpClientGateway mcpClientGateway;

  @Mock private McpToolCallbackRegistry toolCallbackRegistry;

  @Mock private ChatClientProvider chatClientProvider;

  @Mock private ChatClient chatClient;

  @Mock private ChatClient.ChatClientRequestSpec requestSpec;

  @Mock private ChatClient.CallResponseSpec callResponseSpec;

  @Mock private ToolCallback toolCallback;

  private McpService service;

  @BeforeEach
  void setUp() {
    service = new McpService(mcpClientGateway, toolCallbackRegistry, chatClientProvider);
  }

  @Test
  @DisplayName("should delegate tool count to repository")
  void shouldDelegateToolCountToRepository() {
    when(mcpClientGateway.countTools()).thenReturn(7);

    assertThat(service.getTotalToolCount()).isEqualTo(7);
    verify(mcpClientGateway).countTools();
  }

  @Test
  @DisplayName("should return connected servers from repository")
  void shouldReturnConnectedServersFromRepository() {
    Map<String, McpServerConnection> servers =
        Map.of("weather", McpServerConnection.connected("weather", 2));
    when(mcpClientGateway.listServers()).thenReturn(servers);

    assertThat(service.getConnectedServers()).isEqualTo(servers);
  }

  @Test
  @DisplayName("should return tool definitions from repository")
  void shouldReturnToolDefinitionsFromRepository() {
    List<McpToolDefinition> tools = List.of(McpToolDefinition.create("weather", "Weather lookup"));
    when(mcpClientGateway.listTools()).thenReturn(tools);

    assertThat(service.getToolDefinitions()).isEqualTo(tools);
  }

  @Test
  @DisplayName("should register tool callbacks through registry")
  void shouldRegisterToolCallbacksThroughRegistry() {
    ToolCallback[] callbacks = new ToolCallback[] {toolCallback};

    service.registerToolCallbacks(callbacks, "weather");

    verify(toolCallbackRegistry).registerToolCallbacks(callbacks, "weather");
  }

  @Test
  @DisplayName("should clear tools through repository")
  void shouldClearToolsThroughRepository() {
    service.clearTools();

    verify(mcpClientGateway).clearTools();
  }

  @Test
  @DisplayName("should chat with registered tools")
  void shouldChatWithRegisteredTools() {
    ToolCallback[] callbacks = new ToolCallback[] {toolCallback};
    when(toolCallbackRegistry.getRegisteredToolCallbacks()).thenReturn(callbacks);
    when(chatClientProvider.createStateless(any(TextChatOptions.class))).thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user("What is the weather?")).thenReturn(requestSpec);
    when(requestSpec.tools((Object[]) callbacks)).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn("Sunny");

    String answer = service.chatWithTools("What is the weather?");

    assertThat(answer).isEqualTo("Sunny");
    verify(chatClientProvider).createStateless(any(TextChatOptions.class));
    verify(requestSpec).tools((Object[]) callbacks);
  }
}
