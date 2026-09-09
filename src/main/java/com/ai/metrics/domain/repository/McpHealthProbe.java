package com.ai.metrics.domain.repository;

/**
 * Optional MCP module health snapshot for metrics. Implemented only when the MCP module is on the
 * classpath and enabled.
 */
public interface McpHealthProbe {

  /** Documentation. */
  int registeredToolCount();

  /** Documentation. */
  int connectedServerCount();
}
