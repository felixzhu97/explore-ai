package com.ai.agent.application;

import com.ai.agent.domain.AgentDefinition;
import com.ai.agent.domain.AgentPipeline;
import com.ai.agent.domain.AgentRegistry;
import com.ai.agent.domain.AgentType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class AgentFacade {

    private final AgentRegistry registry;
    private final OrchestratorWorkersUseCase orchestrator;

    public AgentFacade(AgentRegistry registry, OrchestratorWorkersUseCase orchestrator) {
        this.registry = registry;
        this.orchestrator = orchestrator;
    }

    public List<AgentDefinition> listAgents() {
        return orchestrator.listAgents();
    }

    public AgentDefinition health(String agentType) {
        return orchestrator.health(AgentType.of(agentType));
    }

    public Flux<ServerSentEvent<String>> invokeSupervisor(String message) {
        return orchestrator.invokeSupervisor(message);
    }

    public Flux<ServerSentEvent<String>> invokeAgent(String agentType, String message) {
        return orchestrator.invokeAgent(AgentType.of(agentType), message);
    }

    public Flux<ServerSentEvent<String>> invokePipeline(String message, AgentPipeline pipeline) {
        return orchestrator.invokePipeline(message, pipeline);
    }
}
