package com.ai.common.domain.model;

import com.ai.common.domain.vo.AbstractUuidId;
import com.ai.common.domain.vo.OwnerKey;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Named owner-keyed aggregate that can be enabled or disabled. */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractEnableableNamedOwnerEntity<IdT extends AbstractUuidId>
    extends AbstractNamedOwnerEntity<IdT> {

  @Column(nullable = false)
  protected boolean enabled;

  protected AbstractEnableableNamedOwnerEntity(
      IdT id,
      OwnerKey ownerKey,
      String name,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    super(id, ownerKey, name, createdAt, updatedAt);
    this.enabled = enabled;
  }

  protected AbstractEnableableNamedOwnerEntity(
      IdT id, String ownerKey, String name, boolean enabled, Instant createdAt, Instant updatedAt) {
    super(id, ownerKey, name, createdAt, updatedAt);
    this.enabled = enabled;
  }

  public void enable() {
    this.enabled = true;
    touchUpdatedAt();
  }

  public void disable() {
    this.enabled = false;
    touchUpdatedAt();
  }

  public boolean isEnabled() {
    return enabled;
  }
}
