package com.ai.mcp.application;

import com.ai.common.application.ChatClientProvider;
import com.ai.common.application.TextChatOptions;
import com.ai.mcp.application.McpToolCallbackRegistry;
import com.ai.mcp.domain.McpToolDefinition;
import com.ai.mcp.domain.McpClientRepository;
import com.ai.mcp.domain.McpServerConnection;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class McpFacade {

    private final McpClientRepository mcpClientRepository;
    private final McpToolCallbackRegistry toolCallbackRegistry;
    private final ChatClientProvider chatClientProvider;

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

    public void registerToolCallbacks(ToolCallback[] tools, String serverName) {
        toolCallbackRegistry.registerToolCallbacks(tools, serverName);
    }

    public void clearTools() {
        mcpClientRepository.clearTools();
    }

    public String chatWithTools(String question) {
        ToolCallback[] tools = toolCallbackRegistry.getRegisteredToolCallbacks();
        return chatClientProvider.createStateless(TextChatOptions.defaults())
                .prompt()
                .user(question)
                .tools(tools)
                .call()
                .content();
    }
}
