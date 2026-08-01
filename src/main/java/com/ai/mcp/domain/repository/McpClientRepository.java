package com.ai.mcp.domain.repository;

import com.ai.mcp.domain.model.McpPromptDefinition;
import com.ai.mcp.domain.model.McpResourceDefinition;
import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.vo.McpServerConnection;

import java.util.List;
import java.util.Map;

public interface McpClientRepository {

    void registerTools(List<McpToolDefinition> tools, String serverName);

    void registerResources(List<McpResourceDefinition> resources, String serverName);

    void registerPrompts(List<McpPromptDefinition> prompts, String serverName);

    void updateServerCapabilities(
            String serverName,
            boolean toolsSupported,
            boolean resourcesSupported,
            boolean promptsSupported);

    List<McpToolDefinition> listTools();

    List<McpResourceDefinition> listResources();

    List<McpPromptDefinition> listPrompts();

    Map<String, McpServerConnection> listServers();

    int toolCount();

    void clearTools();
}
