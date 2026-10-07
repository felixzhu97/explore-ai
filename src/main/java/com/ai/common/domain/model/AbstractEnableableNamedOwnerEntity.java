package com.ai.common.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Owner-aware entity with a validated name that can be turned off. */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractEnableableNamedOwnerEntity<IdT extends AbstractEmbeddable>
    extends AbstractOwnerAwareEntity<IdT> {

  @NotBlank
  @Size(max = 120)
  @Column(nullable = false, length = 120)
  protected String name;

  @Column(nullable = false)
  protected boolean enabled;

  protected AbstractEnableableNamedOwnerEntity(IdT id, String ownerKey, String name) {
    super(id, OwnerKey.parse(ownerKey));
    this.name = DomainStrings.requireName(name);
    this.enabled = true;
  }

  /** Renames the entity. */
  protected void rename(String nextName) {
    this.name = DomainStrings.requireName(nextName);
  }

  /** Turns the entity off. */
  public void disable() {
    this.enabled = false;
  }
}
