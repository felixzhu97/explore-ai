package com.ai.mcp.application.usecase;

import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.repository.McpClientRepository;
import com.ai.mcp.domain.vo.McpServerConnection;
import com.ai.mcp.infrastructure.client.SpringAiMcpClientRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class McpFacade {

    private final McpClientRepository mcpClientRepository;
    private final SpringAiMcpClientRepository mcpClientGateway;
    private final ChatClient chatClient;

    public McpFacade(
            McpClientRepository mcpClientRepository,
            SpringAiMcpClientRepository mcpClientGateway,
            ChatClient.Builder chatClientBuilder) {
        this.mcpClientRepository = mcpClientRepository;
        this.mcpClientGateway = mcpClientGateway;
        this.chatClient = chatClientBuilder.build();
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
        mcpClientGateway.registerToolCallbacks(tools, serverName);
    }

    public void clearTools() {
        mcpClientRepository.clearTools();
    }

    public String chatWithTools(String question) {
        ToolCallback[] tools = mcpClientGateway.getRegisteredToolCallbacks();
        return chatClient.prompt().user(question).tools(tools).call().content();
    }
}
