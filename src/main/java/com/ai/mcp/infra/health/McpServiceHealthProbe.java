package com.ai.mcp.infra.health;

import com.ai.mcp.service.McpService;
import com.ai.metrics.domain.repository.McpHealthProbe;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Bridges {@link McpService} into metrics health without coupling metrics to MCP types. */
@Component
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-mcp",
    havingValue = "true",
    matchIfMissing = false)
@RequiredArgsConstructor
public class McpServiceHealthProbe implements McpHealthProbe {

  private final McpService mcpService;

  @Override
  public int countRegisteredTools() {
    return mcpService.getTotalToolCount();
  }

  @Override
  public int countConnectedServers() {
    return mcpService.getConnectedServers().size();
  }
}
