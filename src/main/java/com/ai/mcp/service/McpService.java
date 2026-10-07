package com.ai.mcp.service;

import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.mcp.domain.model.McpServerConnection;
import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.repository.McpClientGateway;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class McpService {

  private final McpClientGateway mcpClientGateway;
  private final McpToolCallbackRegistry toolCallbackRegistry;
  private final ChatClientProvider chatClientProvider;

  /** Counts the registered tools. */
  public int getTotalToolCount() {
    return mcpClientGateway.countTools();
  }

  /** Lists the connected servers by name. */
  public Map<String, McpServerConnection> getConnectedServers() {
    return mcpClientGateway.listServers();
  }

  /** Lists the registered tool definitions. */
  public List<McpToolDefinition> getToolDefinitions() {
    return mcpClientGateway.listTools();
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

  /** Registers the server's tool callbacks. */
  public void registerToolCallbacks(ToolCallback[] tools, String serverName) {
    toolCallbackRegistry.registerToolCallbacks(tools, serverName);
  }

  /** Removes all registered tools. */
  public void clearTools() {
    mcpClientGateway.clearTools();
  }
}
