package com.ai.common.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Immutable entity partitioned by owner_key. */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractOwnerAwareImmutable<IdT extends AbstractEmbeddable>
    extends AbstractImmutable<IdT> {

  @NotNull
  @Column(nullable = false, length = 80)
  protected OwnerKey ownerKey;

  protected AbstractOwnerAwareImmutable(IdT id, OwnerKey ownerKey) {
    super(id);
    this.ownerKey = Objects.requireNonNull(ownerKey, "ownerKey");
  }

  /** Returns the persisted owner_key value (c:… or u:…). */
  public String getOwnerKeyValue() {
    return ownerKey.value();
  }
}
