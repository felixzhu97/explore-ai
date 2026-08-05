package com.ai.agent.domain.model;

import java.util.List;
import java.util.Objects;

public record AgentPlan(String summary, String goal, List<String> steps, String thoughts) {

    public AgentPlan {
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(goal, "goal");
        steps = steps == null ? List.of() : List.copyOf(steps);
        thoughts = thoughts == null ? "" : thoughts;
    }

    public String executionPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("Goal: ").append(goal).append('\n');
        sb.append("Plan summary: ").append(summary).append('\n');
        if (!steps.isEmpty()) {
            sb.append("Steps:\n");
            for (int i = 0; i < steps.size(); i++) {
                sb.append(i + 1).append(". ").append(steps.get(i)).append('\n');
            }
        }
        return sb.toString();
    }
}
