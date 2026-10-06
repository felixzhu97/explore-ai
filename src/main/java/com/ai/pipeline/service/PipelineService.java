package com.ai.pipeline.service;

import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentPipeline;
import com.ai.pipeline.domain.repository.AgentRegistry;
import com.ai.pipeline.domain.vo.AgentType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/** Entry point for agent listing and supervisor, single-agent and pipeline invocations. */
@Service
@RequiredArgsConstructor
public class PipelineService {

  private final AgentRegistry registry;
  private final PipelineOrchestrationService orchestrator;

  public List<AgentDefinition> listAgents(String ownerKey, String language) {
    return orchestrator.listAgents(ownerKey, language);
  }

  public int countBuiltins() {
    return registry.listBuiltins("en").size();
  }

  public AgentDefinition getHealth(String agentType, String ownerKey, String language) {
    return orchestrator.getHealth(AgentType.of(agentType), ownerKey, language);
  }

  public Flux<ServerSentEvent<String>> invokeSupervisor(
      String message, String ownerKey, String language) {
    return orchestrator.invokeSupervisor(message, ownerKey, language);
  }

  public Flux<ServerSentEvent<String>> invokePipeline(
      String message, AgentPipeline pipeline, String ownerKey, String language) {
    return orchestrator.invokePipeline(message, pipeline, ownerKey, language);
  }

  public Flux<ServerSentEvent<String>> invokeAgent(
      String agentType, String message, String ownerKey, String language) {
    return orchestrator.invokeAgent(AgentType.of(agentType), message, ownerKey, language);
  }

  public String invokePipelineSync(
      String message, AgentPipeline pipeline, String ownerKey, String language) {
    return orchestrator.invokePipelineSync(message, pipeline, ownerKey, language);
  }
}
