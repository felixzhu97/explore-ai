package com.ai.common.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;

/** Run record base partitioned by owner_key. */
@MappedSuperclass
@Filter(name = OwnerPartition.FILTER_NAME)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractOwnerKeyedRunEntity<IdT extends AbstractUuidId>
    extends AbstractTimedRunEntity<IdT> {

  @NotNull
  @Convert(converter = OwnerKeyAttributeConverter.class)
  @Column(nullable = false, length = 80)
  protected OwnerKey ownerKey;

  protected AbstractOwnerKeyedRunEntity(
      IdT id, OwnerKey ownerKey, Instant startedAt, Instant finishedAt) {
    super(id, startedAt, finishedAt);
    this.ownerKey = Objects.requireNonNull(ownerKey, "ownerKey");
  }

  /** Returns the persisted owner_key value (c:… or u:…). */
  public String getOwnerKeyValue() {
    return ownerKey.value();
  }
}
