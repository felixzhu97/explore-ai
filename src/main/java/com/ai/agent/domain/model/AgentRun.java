package com.ai.agent.domain.model;

import java.util.Objects;
import lombok.Getter;

/** One completed baseline Agent invocation for a catalog type. */
@Getter
public final class AgentRun {

  private final String agentType;
  private final String message;
  private final String reply;

  private AgentRun(String agentType, String message, String reply) {
    this.agentType = Objects.requireNonNull(agentType, "agentType");
    this.message = Objects.requireNonNull(message, "message");
    this.reply = Objects.requireNonNull(reply, "reply");
  }

  /** Creates a completed run with the agent's reply. */
  public static AgentRun createCompleted(String agentType, String message, String reply) {
    return new AgentRun(agentType, message, reply == null ? "" : reply);
  }
}
