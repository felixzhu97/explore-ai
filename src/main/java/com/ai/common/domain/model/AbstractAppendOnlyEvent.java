package com.ai.common.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Append-only event base mapping creation audit to occurred_at column. */
@MappedSuperclass
@AttributeOverride(
    name = "createdAt",
    column = @Column(name = "occurred_at", nullable = false, updatable = false))
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractAppendOnlyEvent<IdT extends AbstractUuidId>
    extends AbstractImmutableEntity<IdT> {

  protected AbstractAppendOnlyEvent(IdT id, Instant occurredAt) {
    super(id, occurredAt);
  }

  /** Returns when the event happened. */
  public Instant getOccurredAt() {
    return createdAt;
  }
}
