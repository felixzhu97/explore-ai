package com.ai.common.domain.repository;

import com.ai.common.domain.vo.AbstractUuidId;
import java.util.List;
import java.util.Optional;

/** Shared contract for client-id-partitioned aggregates with unique names per client. */
public interface OwnerScopedRepository<E, I extends AbstractUuidId> {

  E save(E entity);

  Optional<E> findByIdAndOwnerKey(I id, String ownerKey);

  List<E> findAllByOwnerKey(String ownerKey);

  void deleteByIdAndOwnerKey(I id, String ownerKey);

  boolean existsByOwnerKeyAndNameIgnoringId(String ownerKey, String name, I excludeId);
}
