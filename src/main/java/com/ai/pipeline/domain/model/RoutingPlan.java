package com.ai.pipeline.domain.model;

import java.util.List;
import java.util.Objects;
import lombok.Value;

/** Plan produced by the supervisor: primary worker plus optional parallel subtasks. */
@Value
public class RoutingPlan {
  AgentType primaryAgent;
  String reason;
  List<Subtask> subtasks;

  public RoutingPlan(AgentType primaryAgent, String reason, List<Subtask> subtasks) {
    Objects.requireNonNull(primaryAgent, "primaryAgent");
    Objects.requireNonNull(reason, "reason");
    subtasks = subtasks == null ? List.of() : List.copyOf(subtasks);
    if (primaryAgent.isSupervisor()) {
      throw new IllegalArgumentException("primary agent must be a worker, not supervisor");
    }
    this.primaryAgent = primaryAgent;
    this.reason = reason;
    this.subtasks = subtasks;
  }

  /** Instruction for one worker agent that runs alongside the primary agent. */
  @Value
  public static class Subtask {
    AgentType agentType;
    String instruction;

    public Subtask(AgentType agentType, String instruction) {
      Objects.requireNonNull(agentType, "agentType");
      Objects.requireNonNull(instruction, "instruction");
      if (agentType.isSupervisor()) {
        throw new IllegalArgumentException("subtask agent must be a worker");
      }
      this.agentType = agentType;
      this.instruction = instruction;
    }
  }

  /** Creates a plan that routes to one agent. */
  public static RoutingPlan createSingleAgentPlan(AgentType agentType, String reason) {
    return new RoutingPlan(agentType, reason, List.of());
  }
}
