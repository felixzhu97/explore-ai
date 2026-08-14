package com.ai.pipeline.application.usecase;

import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentPipeline;
import com.ai.pipeline.domain.repository.AgentRegistry;
import com.ai.pipeline.domain.vo.AgentType;
import java.util.List;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/** Documentation. */
@Service
public class PipelineFacade {

  private final AgentRegistry registry;
  private final OrchestratorWorkersUseCase orchestrator;

  /** Documentation. */
  public PipelineFacade(AgentRegistry registry, OrchestratorWorkersUseCase orchestrator) {
    this.registry = registry;
    this.orchestrator = orchestrator;
  }

  /** Documentation. */
  public List<AgentDefinition> listAgents(String clientId, String language) {
    return orchestrator.listAgents(clientId, language);
  }

  /** Documentation. */
  public AgentDefinition health(String agentType, String clientId, String language) {
    return orchestrator.health(AgentType.of(agentType), clientId, language);
  }

  /** Documentation. */
  public Flux<ServerSentEvent<String>> invokeSupervisor(
      String message, String clientId, String language) {
    return orchestrator.invokeSupervisor(message, clientId, language);
  }

  /** Documentation. */
  public Flux<ServerSentEvent<String>> invokeAgent(
      String agentType, String message, String clientId, String language) {
    return orchestrator.invokeAgent(AgentType.of(agentType), message, clientId, language);
  }

  /** Documentation. */
  public Flux<ServerSentEvent<String>> invokePipeline(
      String message, AgentPipeline pipeline, String clientId, String language) {
    return orchestrator.invokePipeline(message, pipeline, clientId, language);
  }

  /** Documentation. */
  public String invokePipelineSync(
      String message, AgentPipeline pipeline, String clientId, String language) {
    return orchestrator.invokePipelineSync(message, pipeline, clientId, language);
  }

  /** Documentation. */
  public int builtinCount() {
    return registry.listBuiltins("en").size();
  }
}
