package com.ai.agent.domain.model;

import com.ai.agent.domain.vo.AgentType;

import java.util.List;
import java.util.Objects;

/**
 * Plan produced by the supervisor: primary worker plus optional parallel subtasks.
 */
public record RoutingPlan(
        AgentType primaryAgent,
        String reason,
        List<Subtask> subtasks) {

    public RoutingPlan {
        Objects.requireNonNull(primaryAgent, "primaryAgent");
        Objects.requireNonNull(reason, "reason");
        subtasks = subtasks == null ? List.of() : List.copyOf(subtasks);
        if (primaryAgent.isSupervisor()) {
            throw new IllegalArgumentException("primary agent must be a worker, not supervisor");
        }
    }

    public record Subtask(AgentType agentType, String instruction) {
        public Subtask {
            Objects.requireNonNull(agentType, "agentType");
            Objects.requireNonNull(instruction, "instruction");
            if (agentType.isSupervisor()) {
                throw new IllegalArgumentException("subtask agent must be a worker");
            }
        }
    }

    public static RoutingPlan single(AgentType agentType, String reason) {
        return new RoutingPlan(agentType, reason, List.of());
    }
}
