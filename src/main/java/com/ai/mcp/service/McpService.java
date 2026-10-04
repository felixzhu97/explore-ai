package com.ai.mcp.service;

import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.repository.McpClientGateway;
import com.ai.mcp.domain.vo.McpServerConnection;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Entry point for MCP tool registration, server listing, and tool-augmented chat. */
@Service
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-mcp",
    havingValue = "true",
    matchIfMissing = false)
public class McpService {

  private final McpClientGateway mcpClientGateway;
  private final McpToolCallbackRegistry toolCallbackRegistry;
  private final ChatClientProvider chatClientProvider;

  public McpService(
      McpClientGateway mcpClientGateway,
      McpToolCallbackRegistry toolCallbackRegistry,
      ChatClientProvider chatClientProvider) {
    this.mcpClientGateway = mcpClientGateway;
    this.toolCallbackRegistry = toolCallbackRegistry;
    this.chatClientProvider = chatClientProvider;
  }

  public int getTotalToolCount() {
    return mcpClientGateway.toolCount();
  }

  public Map<String, McpServerConnection> getConnectedServers() {
    return mcpClientGateway.listServers();
  }

  public List<McpToolDefinition> getToolDefinitions() {
    return mcpClientGateway.listTools();
  }

  public void registerToolCallbacks(ToolCallback[] tools, String serverName) {
    toolCallbackRegistry.registerToolCallbacks(tools, serverName);
  }

  public void clearTools() {
    mcpClientGateway.clearTools();
  }

  /** Answers the question with a stateless chat that can call all registered MCP tools. */
  public String chatWithTools(String question) {
    ToolCallback[] tools = toolCallbackRegistry.getRegisteredToolCallbacks();
    return chatClientProvider
        .createStateless(TextChatOptions.defaults())
        .prompt()
        .user(question)
        .tools(tools)
        .call()
        .content();
  }
}
