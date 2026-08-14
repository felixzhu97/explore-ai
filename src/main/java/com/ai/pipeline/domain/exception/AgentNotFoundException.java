package com.ai.pipeline.domain.exception;

import com.ai.pipeline.domain.vo.AgentType;

/** Documentation. */
public class AgentNotFoundException extends RuntimeException {

  private final AgentType agentType;

  /** Documentation. */
  public AgentNotFoundException(AgentType agentType) {
    super("Unknown agent type: " + agentType.value());
    this.agentType = agentType;
  }

  /** Documentation. */
  public AgentType agentType() {
    return agentType;
  }
}
