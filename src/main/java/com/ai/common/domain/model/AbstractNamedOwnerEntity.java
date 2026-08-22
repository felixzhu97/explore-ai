package com.ai.common.domain.model;

import com.ai.base.domain.vo.AbstractUuidId;
import com.ai.common.domain.vo.DomainStrings;
import com.ai.common.domain.vo.OwnerKey;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
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

  @Column(name = "name", nullable = false, length = 120)
  protected String name;

  /** Documentation. */
  protected AbstractNamedOwnerEntity(
      IdT id, OwnerKey ownerKey, String name, Instant createdAt, Instant updatedAt) {
    super(id, ownerKey, createdAt, updatedAt);
    this.name = DomainStrings.requireName(name);
  }

  /** Documentation. */
  protected AbstractNamedOwnerEntity(
      IdT id, String clientId, String name, Instant createdAt, Instant updatedAt) {
    super(id, clientId, createdAt, updatedAt);
    this.name = DomainStrings.requireName(name);
  }

  /** Documentation. */
  protected void rename(String nextName) {
    this.name = DomainStrings.requireName(nextName);
    touchUpdatedAt();
  }
}
