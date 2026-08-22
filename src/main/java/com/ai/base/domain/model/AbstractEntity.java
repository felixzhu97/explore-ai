package com.ai.base.domain.model;

import com.ai.base.domain.vo.AbstractUuidId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Mutable aggregate root base with embedded typed ID, optimistic lock, and audit timestamps. */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractEntity<IdT extends AbstractUuidId> {

  @EqualsAndHashCode.Include
  @EmbeddedId
  protected IdT id;

  @Version
  protected Long version;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  protected Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
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
