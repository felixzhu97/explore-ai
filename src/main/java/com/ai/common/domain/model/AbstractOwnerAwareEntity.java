package com.ai.common.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;

/** Mutable entity partitioned by owner_key. */
@MappedSuperclass
@Filter(name = "ownerPartition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractOwnerAwareEntity<IdT extends AbstractEmbeddable>
    extends AbstractEntity<IdT> {

  @NotNull
  @Column(nullable = false, length = 80)
  protected OwnerKey ownerKey;

  protected AbstractOwnerAwareEntity(IdT id, OwnerKey ownerKey) {
    super(id);
    this.ownerKey = Objects.requireNonNull(ownerKey, "ownerKey");
  }

  /** Tells whether the entity belongs to the owner. */
  public boolean belongsTo(OwnerKey candidate) {
    return ownerKey.equals(candidate);
  }

  /** Tells whether the entity belongs to the owner key value; invalid values never match. */
  public boolean belongsTo(String ownerKeyValue) {
    if (ownerKeyValue == null || ownerKeyValue.isBlank()) {
      return false;
    }
    try {
      return belongsTo(OwnerKey.parse(ownerKeyValue));
    } catch (IllegalArgumentException invalid) {
      return false;
    }
  }

  /** Returns the persisted owner_key value (c:… or u:…). */
  public String getOwnerKeyValue() {
    return ownerKey.value();
  }

  /** Moves a guest row to the signed-in account that now owns it. */
  public void transferTo(OwnerKey accountOwnerKey) {
    ownerKey.requireMergeableInto(accountOwnerKey);
    this.ownerKey = accountOwnerKey;
  }
}
