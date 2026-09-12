package com.ai.base.domain.model;

import com.ai.base.domain.vo.AbstractUuidId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Mutable aggregate root base with embedded typed ID, optimistic lock, and audit timestamps. */
@MappedSuperclass
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractEntity<IdT extends AbstractUuidId> {

  @EqualsAndHashCode.Include @EmbeddedId protected IdT id;

  @Version protected Long version;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  protected Instant createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  protected Instant updatedAt;

  /** Documentation. */
  protected AbstractEntity(IdT id, Instant createdAt, Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "id cannot be null");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
  }

  /** Documentation. */
  protected void touchUpdatedAt() {
    this.updatedAt = Instant.now();
  }
}
