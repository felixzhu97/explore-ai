package com.ai.mcp.infra.health;

import com.ai.mcp.service.McpService;
import com.ai.metrics.domain.repository.McpHealthProbe;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Bridges {@link McpService} into metrics health without coupling metrics to MCP types. */
@Component
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-mcp",
    havingValue = "true",
    matchIfMissing = false)
public class McpServiceHealthProbe implements McpHealthProbe {

  private final McpService mcpService;

  public McpServiceHealthProbe(McpService mcpService) {
    this.mcpService = mcpService;
  }

  @Override
  public int registeredToolCount() {
    return mcpService.getTotalToolCount();
  }

  @Override
  public int connectedServerCount() {
    return mcpService.getConnectedServers().size();
  }
}
