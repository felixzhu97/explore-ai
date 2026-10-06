package com.ai.metrics.domain.model;

import com.ai.common.domain.model.AbstractAppendOnlyEvent;
import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.domain.vo.OwnerKeyAttributeConverter;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.AiDomainAttributeConverter;
import com.ai.metrics.domain.vo.ErrorSummary;
import com.ai.metrics.domain.vo.InvocationEventId;
import com.ai.metrics.domain.vo.InvocationOutcome;
import com.ai.metrics.domain.vo.InvocationOutcomeAttributeConverter;
import com.ai.metrics.domain.vo.Latency;
import com.ai.metrics.domain.vo.TokenUsage;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Append-only record of a single AI invocation for metrics and drill-down. Owner, outcome and error
 * are fixed at creation, so success events never carry an error and failures always do.
 */
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
    this.domain = builder.domain;
    this.operation = builder.operation;
    this.outcome = builder.outcome;
    this.latencyMs = builder.latency.millis();
    this.ownerKey = builder.ownerKey;
    this.errorCode = builder.error == null ? null : builder.error.code();
    this.errorMessage = builder.error == null ? null : builder.error.message();
    this.provider = toNullIfBlank(builder.provider);
    this.model = toNullIfBlank(builder.model);
    this.sessionId = toNullIfBlank(builder.sessionId);
    this.documentId = toNullIfBlank(builder.documentId);
    this.agentType = toNullIfBlank(builder.agentType);
    this.toolName = toNullIfBlank(builder.toolName);
    this.promptTokens = builder.tokens.prompt();
    this.completionTokens = builder.tokens.completion();
  }

  /** Starts a successful invocation event; optional context goes on the returned builder. */
  public static Builder succeeded(
      AiDomain domain, String operation, Latency latency, OwnerKey ownerKey) {
    return new Builder(domain, operation, InvocationOutcome.SUCCESS, latency, ownerKey, null);
  }

  /** Starts a failed invocation event; optional context goes on the returned builder. */
  public static Builder failed(
      AiDomain domain, String operation, Latency latency, OwnerKey ownerKey, ErrorSummary error) {
    return new Builder(
        domain,
        operation,
        InvocationOutcome.ERROR,
        latency,
        ownerKey,
        Objects.requireNonNull(error, "error"));
  }

  /** Returns how long the invocation took. */
  public Latency latency() {
    return Latency.ofMillis(latencyMs);
  }

  /** Returns the error of a failed invocation, or empty when it succeeded. */
  public Optional<ErrorSummary> error() {
    return outcome == InvocationOutcome.ERROR
        ? Optional.of(ErrorSummary.of(errorCode, errorMessage))
        : Optional.empty();
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

  /** Optional context of an event started by {@link #succeeded} or {@link #failed}. */
  public static final class Builder {
    private final AiDomain domain;
    private final String operation;
    private final InvocationOutcome outcome;
    private final Latency latency;
    private final OwnerKey ownerKey;
    private final ErrorSummary error;
    private UUID id;
    private Instant occurredAt;
    private String provider;
    private String model;
    private String sessionId;
    private String documentId;
    private String agentType;
    private String toolName;
    private TokenUsage tokens = TokenUsage.UNKNOWN;

    private Builder(
        AiDomain domain,
        String operation,
        InvocationOutcome outcome,
        Latency latency,
        OwnerKey ownerKey,
        ErrorSummary error) {
      this.domain = Objects.requireNonNull(domain, "domain");
      this.operation = requireNonBlank(operation, "operation");
      this.outcome = outcome;
      this.latency = Objects.requireNonNull(latency, "latency");
      this.ownerKey = Objects.requireNonNull(ownerKey, "ownerKey");
      this.error = error;
    }

    /** Sets the id of a stored event. */
    public Builder id(UUID id) {
      this.id = id;
      return this;
    }

    /** Sets when a stored event happened. */
    public Builder occurredAt(Instant occurredAt) {
      this.occurredAt = occurredAt;
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

    /** Sets the token counts. */
    public Builder tokens(TokenUsage tokens) {
      this.tokens = Objects.requireNonNull(tokens, "tokens");
      return this;
    }

    /** Builds the event. */
    public AiInvocationEvent build() {
      return new AiInvocationEvent(this);
    }
  }
}
