package com.ai.common.domain.repository;

import com.ai.common.domain.vo.AbstractUuidId;
import com.ai.common.domain.vo.OwnerKey;
import java.util.List;
import java.util.Optional;

/** Shared contract for aggregates partitioned by owner_key. */
public interface ClientOwnedRepository<EntityT, IdT extends AbstractUuidId> {

  EntityT save(EntityT entity);

  Optional<EntityT> findByIdAndOwnerKey(IdT id, OwnerKey ownerKey);

  List<EntityT> findAllByOwnerKey(OwnerKey ownerKey);

  void deleteByIdAndOwnerKey(IdT id, OwnerKey ownerKey);
}
