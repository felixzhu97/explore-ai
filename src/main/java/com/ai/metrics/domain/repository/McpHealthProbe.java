package com.ai.metrics.domain.repository;

/**
 * Optional MCP module health snapshot for metrics. Implemented only when the MCP module is on the
 * classpath and enabled.
 */
public interface McpHealthProbe {

  /** Counts the registered MCP tools. */
  int countRegisteredTools();

  /** Counts the connected MCP servers. */
  int countConnectedServers();
}
