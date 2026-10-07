package com.ai.mcp.domain.repository;

import com.ai.mcp.domain.model.McpServerConnection;
import com.ai.mcp.domain.model.McpToolDefinition;
import java.util.List;
import java.util.Map;

/** Repository of tool definitions and connections for MCP servers this app consumes. */
public interface McpClientGateway {
  /** Registers the server's tools. */
  void registerTools(List<McpToolDefinition> tools, String serverName);

  /** Lists all registered tools. */
  List<McpToolDefinition> listTools();

  /** Lists the connected servers by name. */
  Map<String, McpServerConnection> listServers();

  /** Counts the registered tools. */
  int countTools();

  /** Removes all registered tools. */
  void clearTools();
}
