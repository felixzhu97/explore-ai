package com.ai.common.domain.model;

import com.ai.base.domain.model.AbstractImmutable;
import com.ai.base.domain.vo.AbstractUuidId;
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
public abstract class AbstractTimedRunEntity<IdT extends AbstractUuidId>
    extends AbstractImmutable<IdT> {

  @Column protected Instant finishedAt;

  /** Documentation. */
  protected AbstractTimedRunEntity(IdT id, Instant startedAt, Instant finishedAt) {
    super(id, Objects.requireNonNull(startedAt, "startedAt"));
    this.finishedAt = finishedAt;
  }

  public Instant getStartedAt() {
    return createdAt;
  }

  /** Documentation. */
  protected void markFinished(Instant finishedAt) {
    this.finishedAt = Objects.requireNonNull(finishedAt, "finishedAt");
  }

  /** Documentation. */
  public boolean isFinished() {
    return finishedAt != null;
  }
}
