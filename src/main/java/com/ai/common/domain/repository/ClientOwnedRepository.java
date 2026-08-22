package com.ai.common.domain.repository;

import com.ai.base.domain.vo.AbstractUuidId;
import com.ai.common.domain.vo.OwnerKey;
import java.util.List;
import java.util.Optional;

/** Shared contract for aggregates partitioned by owner_key. */
public interface ClientOwnedRepository<EntityT, IdT extends AbstractUuidId> {

  /** Documentation. */
  EntityT save(EntityT entity);

  /** Documentation. */
  Optional<EntityT> findByIdAndOwnerKey(IdT id, OwnerKey ownerKey);

  /** Documentation. */
  List<EntityT> findAllByOwnerKey(OwnerKey ownerKey);

  /** Documentation. */
  void deleteByIdAndOwnerKey(IdT id, OwnerKey ownerKey);
}
