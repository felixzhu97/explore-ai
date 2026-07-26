package com.ai.metrics.infrastructure.health;

import com.ai.agent.application.usecase.AgentFacade;
import com.ai.agent.domain.model.AgentDefinition;
import com.ai.mcp.application.usecase.McpFacade;
import com.ai.metrics.domain.repository.MetricsHealthGateway;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DomainHealthGateway implements MetricsHealthGateway {

    private final AgentFacade agentFacade;
    private final McpFacade mcpFacade;

    public DomainHealthGateway(AgentFacade agentFacade, McpFacade mcpFacade) {
        this.agentFacade = agentFacade;
        this.mcpFacade = mcpFacade;
    }

    @Override
    public Map<String, Object> systemStatus() {
        return Map.of("status", "UP");
    }

    @Override
    public Map<String, Object> agentsHealth() {
        List<AgentDefinition> agents = agentFacade.listAgents();
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
