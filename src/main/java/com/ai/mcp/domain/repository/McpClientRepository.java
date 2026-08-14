package com.ai.mcp.domain.repository;

import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.vo.McpServerConnection;
import java.util.List;
import java.util.Map;

/** Documentation. */
public interface McpClientRepository {
  /** Documentation. */
  void registerTools(List<McpToolDefinition> tools, String serverName);

  /** Documentation. */
  List<McpToolDefinition> listTools();

  /** Documentation. */
  Map<String, McpServerConnection> listServers();

  /** Documentation. */
  int toolCount();

  /** Documentation. */
  void clearTools();
}
