package com.ai.metrics.domain.repository;

import com.ai.metrics.domain.model.ModuleStatus;
import lombok.Value;

/** Gateway supplying system, agent, and MCP health to the metrics dashboard. */
public interface MetricsHealthGateway {

  /** Agent pipeline health: UP only when every registered agent is healthy. */
  @Value
  class AgentsHealth {
    ModuleStatus status;
    long agentCount;
    long healthyAgentCount;
  }

  /** MCP client health: DISABLED when the MCP module is off. */
  @Value
  class McpHealth {
    ModuleStatus status;
    long registeredTools;
    long connectedServers;
  }

  /** Returns the status of each module. */
  ModuleStatus getSystemStatus();

  /** Checks the health of the agents. */
  AgentsHealth checkAgentsHealth();

  /** Checks the health of MCP. */
  McpHealth checkMcpHealth();
}
