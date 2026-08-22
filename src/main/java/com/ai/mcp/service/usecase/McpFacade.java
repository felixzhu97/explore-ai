package com.ai.mcp.service.usecase;

import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.repository.McpClientRepository;
import com.ai.mcp.domain.vo.McpServerConnection;
import com.ai.mcp.service.McpToolCallbackRegistry;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

/** Documentation. */
@Service
public class McpFacade {

  private final McpClientRepository mcpClientRepository;
  private final McpToolCallbackRegistry toolCallbackRegistry;
  private final ChatClientProvider chatClientProvider;

  /** Documentation. */
  public McpFacade(
      McpClientRepository mcpClientRepository,
      McpToolCallbackRegistry toolCallbackRegistry,
      ChatClientProvider chatClientProvider) {
    this.mcpClientRepository = mcpClientRepository;
    this.toolCallbackRegistry = toolCallbackRegistry;
    this.chatClientProvider = chatClientProvider;
  }

  public int getTotalToolCount() {
    return mcpClientRepository.toolCount();
  }

  public Map<String, McpServerConnection> getConnectedServers() {
    return mcpClientRepository.listServers();
  }

  public List<McpToolDefinition> getToolDefinitions() {
    return mcpClientRepository.listTools();
  }

  /** Documentation. */
  public void registerToolCallbacks(ToolCallback[] tools, String serverName) {
    toolCallbackRegistry.registerToolCallbacks(tools, serverName);
  }

  /** Documentation. */
  public void clearTools() {
    mcpClientRepository.clearTools();
  }

  /** Documentation. */
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
