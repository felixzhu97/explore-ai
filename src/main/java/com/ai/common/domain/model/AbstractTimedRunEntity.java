package com.ai.common.domain.model;

import com.ai.base.domain.vo.AbstractUuidId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Base for run records tracked by started and finished timestamps (no updated_at). */
@MappedSuperclass
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractTimedRunEntity<IdT extends AbstractUuidId> {

  @EqualsAndHashCode.Include @EmbeddedId protected IdT id;

  @Column(name = "started_at", nullable = false)
  protected Instant startedAt;

  @Column(name = "finished_at")
  protected Instant finishedAt;

  /** Documentation. */
  protected AbstractTimedRunEntity(IdT id, Instant startedAt, Instant finishedAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    this.finishedAt = finishedAt;
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
