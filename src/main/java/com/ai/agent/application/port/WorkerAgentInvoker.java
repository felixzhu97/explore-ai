package com.ai.agent.application.port;

import com.ai.agent.domain.model.AgentDefinition;
import reactor.core.publisher.Flux;

/**
 * Invokes a specialized worker agent with its system prompt.
 */
public interface WorkerAgentInvoker {

    Flux<String> invokeStream(AgentDefinition agent, String task);

    String invoke(AgentDefinition agent, String task);
}
