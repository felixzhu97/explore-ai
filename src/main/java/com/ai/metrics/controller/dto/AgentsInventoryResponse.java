package com.ai.metrics.controller.dto;

import com.ai.metrics.domain.repository.MetricsHealthGateway.AgentsHealth;
import com.ai.metrics.domain.vo.ModuleStatus;

public record AgentsInventoryResponse(ModuleStatus status, long agentCount, long healthyAgentCount)
    implements DomainInventoryResponse {

  /** Maps agent health to a response. */
  public static AgentsInventoryResponse from(AgentsHealth health) {
    return new AgentsInventoryResponse(
        health.status(), health.agentCount(), health.healthyAgentCount());
  }
}
