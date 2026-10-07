package com.ai.pipeline.service;

import com.ai.pipeline.domain.model.AgentDefinition;
import reactor.core.publisher.Flux;

/** Invokes a specialized worker agent with its system prompt. */
public interface WorkerAgentInvoker {
  /** Streams the agent's answer to the task. */
  Flux<String> invokeStream(AgentDefinition agent, String task);

  /** Returns the agent's answer to the task. */
  String invokeAgent(AgentDefinition agent, String task);
}
