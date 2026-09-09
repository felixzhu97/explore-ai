package com.ai.mcp.infra.health;

import com.ai.mcp.service.usecase.McpFacade;
import com.ai.metrics.domain.repository.McpHealthProbe;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Bridges {@link McpFacade} into metrics health without coupling metrics to MCP types. */
@Component
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-mcp",
    havingValue = "true",
    matchIfMissing = false)
public class McpHealthProbeAdapter implements McpHealthProbe {

  private final McpFacade mcpFacade;

  /** Documentation. */
  public McpHealthProbeAdapter(McpFacade mcpFacade) {
    this.mcpFacade = mcpFacade;
  }

  @Override
  public int registeredToolCount() {
    return mcpFacade.getTotalToolCount();
  }

  @Override
  public int connectedServerCount() {
    return mcpFacade.getConnectedServers().size();
  }
}
