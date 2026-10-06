package com.ai.metrics.controller.dto;

import com.ai.metrics.domain.repository.MetricsHealthGateway.McpHealth;
import com.ai.metrics.domain.vo.ModuleStatus;

public record McpInventoryResponse(
    ModuleStatus status, long registeredTools, long connectedServers) {

  /** Maps MCP health to a response. */
  public static McpInventoryResponse from(McpHealth health) {
    return new McpInventoryResponse(
        health.status(), health.registeredTools(), health.connectedServers());
  }
}
