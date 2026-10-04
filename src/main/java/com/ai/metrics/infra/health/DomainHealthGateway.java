package com.ai.metrics.infra.health;

import com.ai.metrics.domain.repository.McpHealthProbe;
import com.ai.metrics.domain.repository.MetricsHealthGateway;
import com.ai.metrics.domain.vo.ModuleStatus;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.service.PipelineService;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Health gateway that reports system, agent pipeline, and MCP status for the metrics views. */
@Component
public class DomainHealthGateway implements MetricsHealthGateway {

  private final PipelineService pipelineService;
  private final ObjectProvider<McpHealthProbe> mcpHealthProbe;

  public DomainHealthGateway(
      PipelineService pipelineService, ObjectProvider<McpHealthProbe> mcpHealthProbe) {
    this.pipelineService = pipelineService;
    this.mcpHealthProbe = mcpHealthProbe;
  }

  @Override
  public ModuleStatus systemStatus() {
    return ModuleStatus.UP;
  }

  @Override
  public AgentsHealth agentsHealth() {
    List<AgentDefinition> agents = pipelineService.listAgents(null, "en");
    long healthy = agents.stream().filter(AgentDefinition::healthy).count();
    ModuleStatus status =
        healthy == agents.size() && !agents.isEmpty() ? ModuleStatus.UP : ModuleStatus.DEGRADED;
    return new AgentsHealth(status, agents.size(), healthy);
  }

  @Override
  public McpHealth mcpHealth() {
    McpHealthProbe probe = mcpHealthProbe.getIfAvailable();
    if (probe == null) {
      return new McpHealth(ModuleStatus.DISABLED, 0, 0);
    }
    return new McpHealth(
        ModuleStatus.UP, probe.registeredToolCount(), probe.connectedServerCount());
  }
}
