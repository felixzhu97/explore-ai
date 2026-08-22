package com.ai.common.domain.model;

import com.ai.base.domain.model.AbstractEntity;
import com.ai.base.domain.vo.AbstractUuidId;
import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.domain.vo.OwnerKeyAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Aggregate base for rows partitioned by owner_key. */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractOwnerKeyedEntity<IdT extends AbstractUuidId>
    extends AbstractEntity<IdT> {

  @Convert(converter = OwnerKeyAttributeConverter.class)
  @Column(name = "owner_key", nullable = false, length = 80)
  protected OwnerKey ownerKey;

  /** Documentation. */
  protected AbstractOwnerKeyedEntity(
      IdT id, OwnerKey ownerKey, Instant createdAt, Instant updatedAt) {
    super(id, createdAt, updatedAt);
    this.ownerKey = Objects.requireNonNull(ownerKey, "ownerKey");
  }

  /** Documentation. */
  protected AbstractOwnerKeyedEntity(
      IdT id, String ownerKeyValue, Instant createdAt, Instant updatedAt) {
    this(id, OwnerKey.parse(ownerKeyValue), createdAt, updatedAt);
  }

  /** Documentation. */
  public boolean belongsTo(OwnerKey candidate) {
    return ownerKey.equals(candidate);
  }

  /** Documentation. */
  public boolean belongsToClient(String ownerKeyValue) {
    return ownerKey.value().equals(ownerKeyValue);
  }

  /** Returns the persisted owner_key value (c:… or u:…). */
  public String getClientId() {
    return ownerKey.value();
  }

  /** Documentation. */
  protected void rebindOwnerKey(OwnerKey nextOwnerKey) {
    this.ownerKey = Objects.requireNonNull(nextOwnerKey, "ownerKey");
  }

  /** Documentation. */
  public void rebindOwnerKey(String ownerKeyValue) {
    rebindOwnerKey(OwnerKey.parse(ownerKeyValue));
  }
}
