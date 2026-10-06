package com.ai.metrics.domain.model;

import com.ai.common.domain.model.AbstractAppendOnlyEvent;
import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.domain.vo.OwnerKeyAttributeConverter;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.AiDomainAttributeConverter;
import com.ai.metrics.domain.vo.InvocationEventId;
import com.ai.metrics.domain.vo.InvocationOutcome;
import com.ai.metrics.domain.vo.InvocationOutcomeAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** Append-only record of a single AI invocation for metrics and drill-down. */
@Entity
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class AiInvocationEvent extends AbstractAppendOnlyEvent<InvocationEventId> {

  @NotNull
  @Convert(converter = AiDomainAttributeConverter.class)
  @Column(nullable = false, length = 32)
  private AiDomain domain;

  @NotBlank
  @Size(max = 64)
  @Column(nullable = false, length = 64)
  private String operation;

  @NotNull
  @Convert(converter = InvocationOutcomeAttributeConverter.class)
  @Column(nullable = false, length = 16)
  private InvocationOutcome outcome;

  @Column(nullable = false)
  private long latencyMs;

  @Size(max = 64)
  @Column(length = 64)
  private String provider;

  @Size(max = 128)
  @Column(length = 128)
  private String model;

  @Size(max = 36)
  @Column(length = 36)
  private String sessionId;

  @Size(max = 36)
  @Column(length = 36)
  private String documentId;

  @Size(max = 64)
  @Column(length = 64)
  private String agentType;

  @Size(max = 128)
  @Column(length = 128)
  private String toolName;

  @Column private Integer promptTokens;

  @Column private Integer completionTokens;

  @Size(max = 64)
  @Column(length = 64)
  private String errorCode;

  @Size(max = 512)
  @Column(length = 512)
  private String errorMessage;

  @NotNull
  @Convert(converter = OwnerKeyAttributeConverter.class)
  @Column(nullable = false, length = 80)
  private OwnerKey ownerKey;

  private AiInvocationEvent(Builder builder) {
    super(
        builder.id != null
            ? InvocationEventId.of(builder.id.toString())
            : InvocationEventId.generate(),
        Objects.requireNonNullElseGet(builder.occurredAt, Instant::now));
    this.domain = Objects.requireNonNull(builder.domain, "domain");
    this.operation = requireNonBlank(builder.operation, "operation");
    this.outcome = Objects.requireNonNull(builder.outcome, "outcome");
    this.latencyMs = Math.max(0L, builder.latencyMs);
    this.provider = toNullIfBlank(builder.provider);
    this.model = toNullIfBlank(builder.model);
    this.sessionId = toNullIfBlank(builder.sessionId);
    this.documentId = toNullIfBlank(builder.documentId);
    this.agentType = toNullIfBlank(builder.agentType);
    this.toolName = toNullIfBlank(builder.toolName);
    this.promptTokens = builder.promptTokens;
    this.completionTokens = builder.completionTokens;
    this.errorCode = toNullIfBlank(builder.errorCode);
    this.errorMessage = truncate(toNullIfBlank(builder.errorMessage), 512);
    this.ownerKey = toOwnerKey(builder.ownerKey);
  }

  /** Creates an event builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** Sets owner partition key before persistence. */
  public void assignOwnerKey(String ownerKey) {
    this.ownerKey = toOwnerKey(ownerKey);
  }

  private static OwnerKey toOwnerKey(String ownerKey) {
    return ownerKey == null || ownerKey.isBlank() ? OwnerKey.UNOWNED : OwnerKey.parse(ownerKey);
  }

  private static String requireNonBlank(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.trim();
  }

  private static String toNullIfBlank(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String truncate(String value, int max) {
    if (value == null || value.length() <= max) {
      return value;
    }
    return value.substring(0, max);
  }

  /** Fluent builder whose {@code build()} validates and normalizes the collected event fields. */
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

    /** Sets the event id. */
    public Builder id(UUID id) {
      this.id = id;
      return this;
    }

    /** Sets when the call happened. */
    public Builder occurredAt(Instant occurredAt) {
      this.occurredAt = occurredAt;
      return this;
    }

    /** Sets the AI domain. */
    public Builder domain(AiDomain domain) {
      this.domain = domain;
      return this;
    }

    /** Sets the operation name. */
    public Builder operation(String operation) {
      this.operation = operation;
      return this;
    }

    /** Sets the outcome. */
    public Builder outcome(InvocationOutcome outcome) {
      this.outcome = outcome;
      return this;
    }

    /** Sets the latency in milliseconds. */
    public Builder latencyMs(long latencyMs) {
      this.latencyMs = latencyMs;
      return this;
    }

    /** Sets the provider. */
    public Builder provider(String provider) {
      this.provider = provider;
      return this;
    }

    /** Sets the model. */
    public Builder model(String model) {
      this.model = model;
      return this;
    }

    /** Sets the chat session id. */
    public Builder sessionId(String sessionId) {
      this.sessionId = sessionId;
      return this;
    }

    /** Sets the document id. */
    public Builder documentId(String documentId) {
      this.documentId = documentId;
      return this;
    }

    /** Sets the agent type. */
    public Builder agentType(String agentType) {
      this.agentType = agentType;
      return this;
    }

    /** Sets the tool name. */
    public Builder toolName(String toolName) {
      this.toolName = toolName;
      return this;
    }

    /** Sets the prompt token count. */
    public Builder promptTokens(Integer promptTokens) {
      this.promptTokens = promptTokens;
      return this;
    }

    /** Sets the completion token count. */
    public Builder completionTokens(Integer completionTokens) {
      this.completionTokens = completionTokens;
      return this;
    }

    /** Sets the error code. */
    public Builder errorCode(String errorCode) {
      this.errorCode = errorCode;
      return this;
    }

    /** Sets the error message. */
    public Builder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    /** Sets the owner key. */
    public Builder ownerKey(String ownerKey) {
      this.ownerKey = ownerKey;
      return this;
    }

    /** Builds the event. */
    public AiInvocationEvent build() {
      return new AiInvocationEvent(this);
    }
  }
}
