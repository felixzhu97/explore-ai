package com.ai.agents.domain;

import java.util.Objects;

/**
 * Routing decision value object.
 * Encapsulates the result of routing a message to an agent.
 */
public final class RoutingDecision {
    private final AgentType targetType;
    private final AgentId targetAgentId;
    private final double confidence;
    private final String reason;

    private RoutingDecision(
            AgentType targetType,
            AgentId targetAgentId,
            double confidence,
            String reason
    ) {
        if (targetType == null && targetAgentId == null) {
            throw new IllegalArgumentException("Either targetType or targetAgentId must be provided");
        }
        this.targetType = targetType;
        this.targetAgentId = targetAgentId;
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
        this.confidence = confidence;
        this.reason = reason != null ? reason : "";
    }

    public static RoutingDecision of(AgentType type) {
        return new RoutingDecision(type, null, 1.0, "Direct routing");
    }

    public static RoutingDecision to(AgentType type, String reason) {
        return new RoutingDecision(type, null, 1.0, reason);
    }

    public static RoutingDecision to(AgentType type, double confidence, String reason) {
        return new RoutingDecision(type, null, confidence, reason);
    }

    public static RoutingDecision to(AgentId agentId, double confidence, String reason) {
        return new RoutingDecision(null, agentId, confidence, reason);
    }

    public static RoutingDecision fallback() {
        return new RoutingDecision(AgentType.CHAT, null, 0.5, "Fallback to chat");
    }

    public AgentType targetType() { return targetType; }
    public AgentId targetAgentId() { return targetAgentId; }
    public double confidence() { return confidence; }
    public String reason() { return reason; }

    public boolean isConfident() {
        return confidence >= 0.8;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoutingDecision that = (RoutingDecision) o;
        return Double.compare(that.confidence, confidence) == 0
                && targetType == that.targetType
                && Objects.equals(targetAgentId, that.targetAgentId)
                && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetType, targetAgentId, confidence, reason);
    }
}
