package com.ai.metrics.domain.repository;

import java.util.Map;

/** Gateway supplying system, agent, and MCP health maps to the metrics dashboard. */
public interface MetricsHealthGateway {
  Map<String, Object> systemStatus();

  Map<String, Object> agentsHealth();

  Map<String, Object> mcpHealth();
}
