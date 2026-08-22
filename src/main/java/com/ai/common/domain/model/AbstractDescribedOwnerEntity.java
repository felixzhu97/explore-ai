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

/** Named owner-keyed aggregate with normalized description. */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractDescribedOwnerEntity<IdT extends AbstractUuidId>
    extends AbstractNamedOwnerEntity<IdT> {

  @Column(name = "description", nullable = false, length = 500)
  protected String description;

  /** Documentation. */
  protected AbstractDescribedOwnerEntity(
      IdT id,
      OwnerKey ownerKey,
      String name,
      String description,
      Instant createdAt,
      Instant updatedAt) {
    super(id, ownerKey, name, createdAt, updatedAt);
    this.description = DomainStrings.normalizeDescription(description);
  }

  /** Documentation. */
  protected AbstractDescribedOwnerEntity(
      IdT id,
      String clientId,
      String name,
      String description,
      Instant createdAt,
      Instant updatedAt) {
    super(id, clientId, name, createdAt, updatedAt);
    this.description = DomainStrings.normalizeDescription(description);
  }

  /** Documentation. */
  protected void updateDescription(String nextDescription) {
    this.description = DomainStrings.normalizeDescription(nextDescription);
    touchUpdatedAt();
  }
}
