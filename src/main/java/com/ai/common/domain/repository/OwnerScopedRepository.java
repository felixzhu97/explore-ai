package com.ai.common.domain.repository;

import com.ai.common.domain.model.AbstractEmbeddable;
import java.util.List;
import java.util.Optional;

/** Shared contract for client-id-partitioned aggregates with unique names per client. */
public interface OwnerScopedRepository<E, I extends AbstractEmbeddable> {

  /** Finds the owner's entity by id. */
  Optional<E> findByIdAndOwnerKey(I id, String ownerKey);

  /** Lists all entities of the owner. */
  List<E> findAllByOwnerKey(String ownerKey);

  /** Tells whether another entity of the owner already uses the name. */
  boolean existsByOwnerKeyAndNameIgnoringId(String ownerKey, String name, I excludeId);

  /** Saves the entity and returns the stored copy. */
  E save(E entity);

  /** Deletes the owner's entity by id. */
  void deleteByIdAndOwnerKey(I id, String ownerKey);
}
