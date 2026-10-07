package com.ai.common.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

/**
 * Layer supertype of every entity: an assigned typed id and the time the row was inserted.
 *
 * <p>Ids are assigned before the first save, so the entity tells Spring Data whether it is new
 * instead of letting {@code save} merge, which would first select a row that does not exist.
 *
 * @see <a href="https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html">
 *     Spring Data JPA › Persisting Entities</a>
 */
@MappedSuperclass
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractImmutable<IdT extends AbstractEmbeddable>
    implements Persistable<IdT>, Serializable {

  @EqualsAndHashCode.Include
  @EmbeddedId
  @AttributeOverride(name = "value", column = @Column(name = "id"))
  protected IdT id;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  protected Instant createdAt = Instant.now();

  @Transient
  @Getter(AccessLevel.NONE)
  private boolean persisted;

  protected AbstractImmutable(IdT id) {
    this.id = Objects.requireNonNull(id, "id cannot be null");
  }

  @Override
  public boolean isNew() {
    return !persisted;
  }

  @PostPersist
  @PostLoad
  void markPersisted() {
    this.persisted = true;
  }
}
