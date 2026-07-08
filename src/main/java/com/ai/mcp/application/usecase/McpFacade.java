package com.ai.mcp.application.usecase;

import com.ai.mcp.application.port.McpToolCallbackRegistry;
import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.repository.McpClientRepository;
import com.ai.mcp.domain.vo.McpServerConnection;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class McpFacade {

    private final McpClientRepository mcpClientRepository;
    private final McpToolCallbackRegistry toolCallbackRegistry;
    private final ChatClient chatClient;

    public McpFacade(
            McpClientRepository mcpClientRepository,
            McpToolCallbackRegistry toolCallbackRegistry,
            ChatClient chatClient) {
        this.mcpClientRepository = mcpClientRepository;
        this.toolCallbackRegistry = toolCallbackRegistry;
        this.chatClient = chatClient;
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

    public void registerToolCallbacks(ToolCallback[] tools, String serverName) {
        toolCallbackRegistry.registerToolCallbacks(tools, serverName);
    }

    public void clearTools() {
        mcpClientRepository.clearTools();
    }

    public String chatWithTools(String question) {
        ToolCallback[] tools = toolCallbackRegistry.getRegisteredToolCallbacks();
        return chatClient.prompt().user(question).tools(tools).call().content();
    }
}
