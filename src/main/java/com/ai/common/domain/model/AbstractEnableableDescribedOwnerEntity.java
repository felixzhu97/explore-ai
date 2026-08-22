package com.ai.common.domain.model;

import com.ai.base.domain.vo.AbstractUuidId;
import com.ai.common.domain.vo.OwnerKey;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Described owner-keyed aggregate that can be enabled or disabled. */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractEnableableDescribedOwnerEntity<IdT extends AbstractUuidId>
    extends AbstractDescribedOwnerEntity<IdT> {

  @Column(name = "enabled", nullable = false)
  protected boolean enabled;

  /** Documentation. */
  protected AbstractEnableableDescribedOwnerEntity(
      IdT id,
      OwnerKey ownerKey,
      String name,
      String description,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    super(id, ownerKey, name, description, createdAt, updatedAt);
    this.enabled = enabled;
  }

  /** Documentation. */
  protected AbstractEnableableDescribedOwnerEntity(
      IdT id,
      String clientId,
      String name,
      String description,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    super(id, clientId, name, description, createdAt, updatedAt);
    this.enabled = enabled;
  }

  /** Documentation. */
  public void enable() {
    this.enabled = true;
    touchUpdatedAt();
  }

  /** Documentation. */
  public void disable() {
    this.enabled = false;
    touchUpdatedAt();
  }

  /** Documentation. */
  public boolean isEnabled() {
    return enabled;
  }
}
