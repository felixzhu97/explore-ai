package com.ai.mcp.application.usecase;

import com.ai.chat.application.usecase.TextChatOptions;
import com.ai.chat.infrastructure.llm.ChatClientFactory;
import com.ai.mcp.application.port.McpToolCallbackRegistry;
import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.repository.McpClientRepository;
import com.ai.mcp.domain.vo.McpServerConnection;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class McpFacade {

    private final McpClientRepository mcpClientRepository;
    private final McpToolCallbackRegistry toolCallbackRegistry;
    private final ChatClientFactory chatClientFactory;

    public McpFacade(
            McpClientRepository mcpClientRepository,
            McpToolCallbackRegistry toolCallbackRegistry,
            ChatClientFactory chatClientFactory) {
        this.mcpClientRepository = mcpClientRepository;
        this.toolCallbackRegistry = toolCallbackRegistry;
        this.chatClientFactory = chatClientFactory;
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
        return chatClientFactory.createStateless(TextChatOptions.defaults())
                .prompt()
                .user(question)
                .tools(tools)
                .call()
                .content();
    }
}
