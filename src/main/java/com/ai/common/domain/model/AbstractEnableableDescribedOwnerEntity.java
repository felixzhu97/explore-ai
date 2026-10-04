package com.ai.common.domain.model;

import com.ai.common.domain.vo.AbstractUuidId;
import com.ai.common.domain.vo.DomainStrings;
import com.ai.common.domain.vo.OwnerKey;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Enableable named owner-keyed aggregate with normalized description. */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractEnableableDescribedOwnerEntity<IdT extends AbstractUuidId>
    extends AbstractEnableableNamedOwnerEntity<IdT> {

  /** Trimmed description; empty when the owner gave none. */
  @Size(max = 500)
  @Column(nullable = false, length = 500)
  protected String description;

  protected AbstractEnableableDescribedOwnerEntity(
      IdT id,
      OwnerKey ownerKey,
      String name,
      String description,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    super(id, ownerKey, name, enabled, createdAt, updatedAt);
    this.description = DomainStrings.normalizeDescription(description);
  }

  protected AbstractEnableableDescribedOwnerEntity(
      IdT id,
      String ownerKey,
      String name,
      String description,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    super(id, ownerKey, name, enabled, createdAt, updatedAt);
    this.description = DomainStrings.normalizeDescription(description);
  }

  protected void updateDescription(String nextDescription) {
    this.description = DomainStrings.normalizeDescription(nextDescription);
    touchUpdatedAt();
  }
}
