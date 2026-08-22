package com.ai.pipeline.service;

import com.ai.pipeline.domain.model.AgentDefinition;
import reactor.core.publisher.Flux;

/** Invokes a specialized worker agent with its system prompt. */
public interface WorkerAgentInvoker {
  /** Documentation. */
  Flux<String> invokeStream(AgentDefinition agent, String task);

  /** Documentation. */
  String invoke(AgentDefinition agent, String task);
}
