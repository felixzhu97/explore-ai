package com.ai.pipeline.application.usecase;

import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentPipeline;
import com.ai.pipeline.domain.repository.AgentRegistry;
import com.ai.pipeline.domain.vo.AgentType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class PipelineFacade {

    private final AgentRegistry registry;
    private final OrchestratorWorkersUseCase orchestrator;

    public PipelineFacade(AgentRegistry registry, OrchestratorWorkersUseCase orchestrator) {
        this.registry = registry;
        this.orchestrator = orchestrator;
    }

    public List<AgentDefinition> listAgents(String clientId, String language) {
        return orchestrator.listAgents(clientId, language);
    }

    public AgentDefinition health(String agentType, String clientId, String language) {
        return orchestrator.health(AgentType.of(agentType), clientId, language);
    }

    public Flux<ServerSentEvent<String>> invokeSupervisor(String message, String clientId, String language) {
        return orchestrator.invokeSupervisor(message, clientId, language);
    }

    public Flux<ServerSentEvent<String>> invokeAgent(
            String agentType, String message, String clientId, String language) {
        return orchestrator.invokeAgent(AgentType.of(agentType), message, clientId, language);
    }

    public Flux<ServerSentEvent<String>> invokePipeline(
            String message, AgentPipeline pipeline, String clientId, String language) {
        return orchestrator.invokePipeline(message, pipeline, clientId, language);
    }

    public String invokePipelineSync(
            String message, AgentPipeline pipeline, String clientId, String language) {
        return orchestrator.invokePipelineSync(message, pipeline, clientId, language);
    }

    public int builtinCount() {
        return registry.listBuiltins("en").size();
    }
}
