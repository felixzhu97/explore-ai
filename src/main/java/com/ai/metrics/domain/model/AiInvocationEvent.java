package com.ai.metrics.domain.model;

import com.ai.common.domain.model.AbstractAppendOnlyEvent;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationEventId;
import com.ai.metrics.domain.vo.InvocationOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Append-only record of a single AI invocation for metrics and drill-down. */
@Entity
@Table(name = "ai_invocation_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class AiInvocationEvent extends AbstractAppendOnlyEvent<InvocationEventId> {

  @Column(name = "domain", nullable = false, length = 32)
  private String domainValue;

  @Column(name = "operation", nullable = false, length = 64)
  private String operation;

  @Column(name = "outcome", nullable = false, length = 16)
  private String outcomeValue;

  @Column(name = "latency_ms", nullable = false)
  private long latencyMs;

  @Column(name = "provider", length = 64)
  private String provider;

  @Column(name = "model", length = 128)
  private String model;

  @Column(name = "session_id", length = 36)
  private String sessionId;

  @Column(name = "document_id", length = 36)
  private String documentId;

  @Column(name = "agent_type", length = 64)
  private String agentType;

  @Column(name = "tool_name", length = 128)
  private String toolName;

  @Column(name = "prompt_tokens")
  private Integer promptTokens;

  @Column(name = "completion_tokens")
  private Integer completionTokens;

  @Column(name = "error_code", length = 64)
  private String errorCode;

  @Column(name = "error_message", length = 512)
  private String errorMessage;

  @Column(name = "owner_key", nullable = false, length = 80)
  private String ownerKey;

  private AiInvocationEvent(Builder builder) {
    super(
        builder.id != null
            ? InvocationEventId.of(builder.id.toString())
            : InvocationEventId.generate(),
        Objects.requireNonNullElseGet(builder.occurredAt, Instant::now));
    this.domainValue = Objects.requireNonNull(builder.domain, "domain").value();
    this.operation = requireNonBlank(builder.operation, "operation");
    this.outcomeValue = Objects.requireNonNull(builder.outcome, "outcome").value();
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
    this.ownerKey =
        builder.ownerKey != null
            ? builder.ownerKey
            : com.ai.common.domain.vo.OwnerKey.LEGACY_ORPHAN.value();
  }

  /** Documentation. */
  public static Builder builder() {
    return new Builder();
  }

  public AiDomain getDomain() {
    return AiDomain.require(domainValue);
  }

  public InvocationOutcome getOutcome() {
    return InvocationOutcome.parse(outcomeValue);
  }

  /** Sets owner partition key before persistence. */
  public void assignOwnerKey(String ownerKey) {
    this.ownerKey =
        ownerKey != null && !ownerKey.isBlank()
            ? ownerKey.trim()
            : com.ai.common.domain.vo.OwnerKey.LEGACY_ORPHAN.value();
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

  /** Documentation. */
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
    private String ownerKey;

    /** Documentation. */
    public Builder id(UUID id) {
      this.id = id;
      return this;
    }

    /** Documentation. */
    public Builder occurredAt(Instant occurredAt) {
      this.occurredAt = occurredAt;
      return this;
    }

    /** Documentation. */
    public Builder domain(AiDomain domain) {
      this.domain = domain;
      return this;
    }

    /** Documentation. */
    public Builder operation(String operation) {
      this.operation = operation;
      return this;
    }

    /** Documentation. */
    public Builder outcome(InvocationOutcome outcome) {
      this.outcome = outcome;
      return this;
    }

    /** Documentation. */
    public Builder latencyMs(long latencyMs) {
      this.latencyMs = latencyMs;
      return this;
    }

    /** Documentation. */
    public Builder provider(String provider) {
      this.provider = provider;
      return this;
    }

    /** Documentation. */
    public Builder model(String model) {
      this.model = model;
      return this;
    }

    /** Documentation. */
    public Builder sessionId(String sessionId) {
      this.sessionId = sessionId;
      return this;
    }

    /** Documentation. */
    public Builder documentId(String documentId) {
      this.documentId = documentId;
      return this;
    }

    /** Documentation. */
    public Builder agentType(String agentType) {
      this.agentType = agentType;
      return this;
    }

    /** Documentation. */
    public Builder toolName(String toolName) {
      this.toolName = toolName;
      return this;
    }

    /** Documentation. */
    public Builder promptTokens(Integer promptTokens) {
      this.promptTokens = promptTokens;
      return this;
    }

    /** Documentation. */
    public Builder completionTokens(Integer completionTokens) {
      this.completionTokens = completionTokens;
      return this;
    }

    /** Documentation. */
    public Builder errorCode(String errorCode) {
      this.errorCode = errorCode;
      return this;
    }

    /** Documentation. */
    public Builder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    /** Documentation. */
    public Builder ownerKey(String ownerKey) {
      this.ownerKey = ownerKey;
      return this;
    }

    /** Documentation. */
    public AiInvocationEvent build() {
      return new AiInvocationEvent(this);
    }
  }
}
