package com.ai.metrics.infra.health;

import com.ai.metrics.domain.model.ModuleStatus;
import com.ai.metrics.domain.repository.McpHealthProbe;
import com.ai.metrics.domain.repository.MetricsHealthGateway;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.service.PipelineService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Health gateway that reports system, agent pipeline, and MCP status for the metrics views. */
@Component
@RequiredArgsConstructor
public class CapabilityHealthGateway implements MetricsHealthGateway {

  private final PipelineService pipelineService;
  private final ObjectProvider<McpHealthProbe> mcpHealthProbe;

  @Override
  public ModuleStatus getSystemStatus() {
    return ModuleStatus.UP;
  }

  @Override
  public AgentsHealth checkAgentsHealth() {
    List<AgentDefinition> agents = pipelineService.listAgents(null, "en");
    long healthy = agents.stream().filter(AgentDefinition::isHealthy).count();
    ModuleStatus status =
        healthy == agents.size() && !agents.isEmpty() ? ModuleStatus.UP : ModuleStatus.DEGRADED;
    return new AgentsHealth(status, agents.size(), healthy);
  }

  @Override
  public McpHealth checkMcpHealth() {
    McpHealthProbe probe = mcpHealthProbe.getIfAvailable();
    if (probe == null) {
      return new McpHealth(ModuleStatus.DISABLED, 0, 0);
    }
    return new McpHealth(
        ModuleStatus.UP, probe.countRegisteredTools(), probe.countConnectedServers());
  }
}
