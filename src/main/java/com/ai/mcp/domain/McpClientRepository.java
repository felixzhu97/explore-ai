package com.ai.mcp.domain;

import com.ai.mcp.domain.McpToolDefinition;
import com.ai.mcp.domain.McpServerConnection;

import java.util.List;
import java.util.Map;

public interface McpClientRepository {

    void registerTools(List<McpToolDefinition> tools, String serverName);

    List<McpToolDefinition> listTools();

    Map<String, McpServerConnection> listServers();

    int toolCount();

    void clearTools();
}
