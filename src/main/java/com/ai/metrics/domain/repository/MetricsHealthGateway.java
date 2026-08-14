package com.ai.metrics.domain.repository;

import java.util.Map;

/** Documentation. */
public interface MetricsHealthGateway {
  /** Documentation. */
  Map<String, Object> systemStatus();

  /** Documentation. */
  Map<String, Object> agentsHealth();

  /** Documentation. */
  Map<String, Object> mcpHealth();
}
