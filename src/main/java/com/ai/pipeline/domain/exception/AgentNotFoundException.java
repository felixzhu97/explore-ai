package com.ai.pipeline.domain.exception;

import com.ai.pipeline.domain.vo.AgentType;

/** Thrown when an agent type is neither built in nor in the client's enabled library. */
public class AgentNotFoundException extends RuntimeException {

  private final AgentType agentType;

  public AgentNotFoundException(AgentType agentType) {
    super("Unknown agent type: " + agentType.value());
    this.agentType = agentType;
  }

  /** Returns the agent type that was not found. */
  public AgentType agentType() {
    return agentType;
  }
}
