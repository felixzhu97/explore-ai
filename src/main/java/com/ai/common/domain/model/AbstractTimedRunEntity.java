package com.ai.common.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Base for run records tracked by started and finished timestamps (no updated_at). */
@MappedSuperclass
@AttributeOverride(
    name = "createdAt",
    column = @Column(name = "started_at", nullable = false, updatable = false))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractTimedRunEntity<IdT extends AbstractEmbeddable>
    extends AbstractImmutableEntity<IdT> {

  @Column protected Instant finishedAt;

  protected AbstractTimedRunEntity(IdT id, Instant startedAt, Instant finishedAt) {
    super(id, Objects.requireNonNull(startedAt, "startedAt"));
    this.finishedAt = finishedAt;
  }

  /** Returns when the run started. */
  public Instant getStartedAt() {
    return createdAt;
  }

  /** Records when the run finished. */
  protected void markFinished(Instant finishedAt) {
    this.finishedAt = Objects.requireNonNull(finishedAt, "finishedAt");
  }

  /** Tells whether the run has finished. */
  public boolean isFinished() {
    return finishedAt != null;
  }
}
