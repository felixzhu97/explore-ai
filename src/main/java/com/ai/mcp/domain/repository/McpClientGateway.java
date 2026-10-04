package com.ai.mcp.domain.repository;

import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.vo.McpServerConnection;
import java.util.List;
import java.util.Map;

/** Repository of tool definitions and connections for MCP servers this app consumes. */
public interface McpClientGateway {
  void registerTools(List<McpToolDefinition> tools, String serverName);

  List<McpToolDefinition> listTools();

  Map<String, McpServerConnection> listServers();

  int toolCount();

  void clearTools();
}
