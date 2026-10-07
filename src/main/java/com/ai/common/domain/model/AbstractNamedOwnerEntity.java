package com.ai.common.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Owner-keyed aggregate with a validated name column. */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractNamedOwnerEntity<IdT extends AbstractUuidId>
    extends AbstractOwnerKeyedEntity<IdT> {

  @NotBlank
  @Size(max = 120)
  @Column(nullable = false, length = 120)
  protected String name;

  protected AbstractNamedOwnerEntity(
      IdT id, OwnerKey ownerKey, String name, Instant createdAt, Instant updatedAt) {
    super(id, ownerKey, createdAt, updatedAt);
    this.name = DomainStrings.requireName(name);
  }

  protected AbstractNamedOwnerEntity(
      IdT id, String ownerKey, String name, Instant createdAt, Instant updatedAt) {
    super(id, ownerKey, createdAt, updatedAt);
    this.name = DomainStrings.requireName(name);
  }

  /** Renames the entity. */
  protected void rename(String nextName) {
    this.name = DomainStrings.requireName(nextName);
    touchUpdatedAt();
  }
}
