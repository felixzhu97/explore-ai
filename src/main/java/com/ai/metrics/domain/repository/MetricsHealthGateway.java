package com.ai.metrics.domain.repository;

import com.ai.metrics.domain.vo.ModuleStatus;

/** Gateway supplying system, agent, and MCP health to the metrics dashboard. */
public interface MetricsHealthGateway {

  /** Agent pipeline health: UP only when every registered agent is healthy. */
  record AgentsHealth(ModuleStatus status, long agentCount, long healthyAgentCount) {}

  /** MCP client health: DISABLED when the MCP module is off. */
  record McpHealth(ModuleStatus status, long registeredTools, long connectedServers) {}

  ModuleStatus getSystemStatus();

  AgentsHealth checkAgentsHealth();

  McpHealth checkMcpHealth();
}
