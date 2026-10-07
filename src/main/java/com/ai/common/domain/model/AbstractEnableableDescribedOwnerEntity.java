package com.ai.common.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Enableable named owner-aware entity with a normalized description. */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractEnableableDescribedOwnerEntity<IdT extends AbstractEmbeddable>
    extends AbstractEnableableNamedOwnerEntity<IdT> {

  /** Trimmed description; empty when the owner gave none. */
  @Size(max = 500)
  @Column(nullable = false, length = 500)
  protected String description;

  protected AbstractEnableableDescribedOwnerEntity(
      IdT id, String ownerKey, String name, String description) {
    super(id, ownerKey, name);
    this.description = DomainStrings.normalizeDescription(description);
  }

  /** Updates the description. */
  protected void updateDescription(String nextDescription) {
    this.description = DomainStrings.normalizeDescription(nextDescription);
  }

  /** Turns the entity on. */
  public void enable() {
    this.enabled = true;
  }

  /** Turns the entity on or off. */
  public void changeEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
