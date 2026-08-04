package com.ai.metrics.domain;

import com.ai.metrics.domain.AiDomain;
import com.ai.metrics.domain.InvocationOutcome;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Append-only record of a single AI invocation for metrics and drill-down.
 */
public final class AiInvocationEvent {

    private final UUID id;
    private final Instant occurredAt;
    private final AiDomain domain;
    private final String operation;
    private final InvocationOutcome outcome;
    private final long latencyMs;
    private final String provider;
    private final String model;
    private final String sessionId;
    private final String documentId;
    private final String agentType;
    private final String toolName;
    private final Integer promptTokens;
    private final Integer completionTokens;
    private final String errorCode;
    private final String errorMessage;

    private AiInvocationEvent(Builder builder) {
        this.id = Objects.requireNonNullElseGet(builder.id, UUID::randomUUID);
        this.occurredAt = Objects.requireNonNullElseGet(builder.occurredAt, Instant::now);
        this.domain = Objects.requireNonNull(builder.domain, "domain");
        this.operation = requireNonBlank(builder.operation, "operation");
        this.outcome = Objects.requireNonNull(builder.outcome, "outcome");
        this.latencyMs = Math.max(0L, builder.latencyMs);
        this.provider = blankToNull(builder.provider);
        this.model = blankToNull(builder.model);
        this.sessionId = blankToNull(builder.sessionId);
        this.documentId = blankToNull(builder.documentId);
        this.agentType = blankToNull(builder.agentType);
        this.toolName = blankToNull(builder.toolName);
        this.promptTokens = builder.promptTokens;
        this.completionTokens = builder.completionTokens;
        this.errorCode = blankToNull(builder.errorCode);
        this.errorMessage = truncate(blankToNull(builder.errorMessage), 512);
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public AiDomain getDomain() {
        return domain;
    }

    public String getOperation() {
        return operation;
    }

    public InvocationOutcome getOutcome() {
        return outcome;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getAgentType() {
        return agentType;
    }

    public String getToolName() {
        return toolName;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    public static final class Builder {
        private UUID id;
        private Instant occurredAt;
        private AiDomain domain;
        private String operation;
        private InvocationOutcome outcome;
        private long latencyMs;
        private String provider;
        private String model;
        private String sessionId;
        private String documentId;
        private String agentType;
        private String toolName;
        private Integer promptTokens;
        private Integer completionTokens;
        private String errorCode;
        private String errorMessage;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder domain(AiDomain domain) {
            this.domain = domain;
            return this;
        }

        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        public Builder outcome(InvocationOutcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public Builder latencyMs(long latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder documentId(String documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder agentType(String agentType) {
            this.agentType = agentType;
            return this;
        }

        public Builder toolName(String toolName) {
            this.toolName = toolName;
            return this;
        }

        public Builder promptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
            return this;
        }

        public Builder completionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public AiInvocationEvent build() {
            return new AiInvocationEvent(this);
        }
    }
}
