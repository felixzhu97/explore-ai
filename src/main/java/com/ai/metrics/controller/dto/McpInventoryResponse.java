package com.ai.metrics.controller.dto;

import com.ai.metrics.domain.model.ModuleStatus;
import com.ai.metrics.domain.repository.MetricsHealthGateway.McpHealth;

public record McpInventoryResponse(
    ModuleStatus status, long registeredTools, long connectedServers) {

  /** Maps MCP health to a response. */
  public static McpInventoryResponse createResponse(McpHealth health) {
    return new McpInventoryResponse(
        health.getStatus(), health.getRegisteredTools(), health.getConnectedServers());
  }
}
