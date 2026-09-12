package com.ai.base.domain.model;

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

/** Immutable entity base with embedded typed ID and creation timestamp. */
@MappedSuperclass
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractImmutable<IdT extends AbstractUuidId> {

  @EqualsAndHashCode.Include @EmbeddedId protected IdT id;

  @Column(nullable = false, updatable = false)
  protected Instant createdAt;

  /** Documentation. */
  protected AbstractImmutable(IdT id, Instant createdAt) {
    this.id = Objects.requireNonNull(id, "id cannot be null");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
  }
}
