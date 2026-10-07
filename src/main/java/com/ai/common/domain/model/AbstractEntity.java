package com.ai.common.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

/** Layer supertype of mutable entities: update time and optimistic locking. */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractEntity<IdT extends AbstractEmbeddable>
    extends AbstractImmutable<IdT> {

  @UpdateTimestamp
  @Column(nullable = false)
  protected Instant updatedAt = createdAt;

  @Version protected Long version;

  protected AbstractEntity(IdT id) {
    super(id);
  }
}
