package com.ai.base.domain.model;

import com.ai.base.domain.vo.AbstractUuidId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Immutable entity base with embedded typed ID and creation timestamp. */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractImmutable<IdT extends AbstractUuidId> {

  @EqualsAndHashCode.Include
  @EmbeddedId
  protected IdT id;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  protected Instant createdAt;

  /** Documentation. */
  protected AbstractImmutable(IdT id, Instant createdAt) {
    this.id = Objects.requireNonNull(id, "id cannot be null");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
  }
}
