package com.ai.common.domain.model;

import com.ai.common.domain.vo.AbstractUuidId;
import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.domain.vo.OwnerKeyAttributeConverter;
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

/** Aggregate base for rows partitioned by owner_key. */
@MappedSuperclass
@Filter(name = OwnerPartition.FILTER_NAME)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractOwnerKeyedEntity<IdT extends AbstractUuidId>
    extends AbstractEntity<IdT> {

  @NotNull
  @Convert(converter = OwnerKeyAttributeConverter.class)
  @Column(nullable = false, length = 80)
  protected OwnerKey ownerKey;

  protected AbstractOwnerKeyedEntity(
      IdT id, OwnerKey ownerKey, Instant createdAt, Instant updatedAt) {
    super(id, createdAt, updatedAt);
    this.ownerKey = Objects.requireNonNull(ownerKey, "ownerKey");
  }

  protected AbstractOwnerKeyedEntity(
      IdT id, String ownerKeyValue, Instant createdAt, Instant updatedAt) {
    this(id, OwnerKey.parse(ownerKeyValue), createdAt, updatedAt);
  }

  public boolean belongsTo(OwnerKey candidate) {
    return ownerKey.equals(candidate);
  }

  public boolean belongsTo(String ownerKeyValue) {
    return ownerKey.value().equals(ownerKeyValue);
  }

  /** Returns the persisted owner_key value (c:… or u:…). */
  public String getOwnerKeyValue() {
    return ownerKey.value();
  }

  protected void rebindOwnerKey(OwnerKey nextOwnerKey) {
    this.ownerKey = Objects.requireNonNull(nextOwnerKey, "ownerKey");
  }

  public void rebindOwnerKey(String ownerKeyValue) {
    rebindOwnerKey(OwnerKey.parse(ownerKeyValue));
  }
}
