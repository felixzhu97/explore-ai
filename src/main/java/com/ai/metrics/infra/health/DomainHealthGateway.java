package com.ai.metrics.infra.health;

import com.ai.mcp.service.usecase.McpFacade;
import com.ai.metrics.domain.repository.MetricsHealthGateway;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.service.usecase.PipelineFacade;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Documentation. */
@Component
public class DomainHealthGateway implements MetricsHealthGateway {

  private final PipelineFacade pipelineFacade;
  private final McpFacade mcpFacade;

  /** Documentation. */
  public DomainHealthGateway(PipelineFacade pipelineFacade, McpFacade mcpFacade) {
    this.pipelineFacade = pipelineFacade;
    this.mcpFacade = mcpFacade;
  }

  @Override
  public Map<String, Object> systemStatus() {
    return Map.of("status", "UP");
  }

  @Override
  public Map<String, Object> agentsHealth() {
    List<AgentDefinition> agents = pipelineFacade.listAgents(null, "en");
    long healthy = agents.stream().filter(AgentDefinition::healthy).count();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", healthy == agents.size() && !agents.isEmpty() ? "UP" : "DEGRADED");
    result.put("agentCount", agents.size());
    result.put("healthyAgentCount", healthy);
    return result;
  }

  @Override
  public Map<String, Object> mcpHealth() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "UP");
    result.put("registeredTools", mcpFacade.getTotalToolCount());
    result.put("connectedServers", mcpFacade.getConnectedServers().size());
    return result;
  }
}
