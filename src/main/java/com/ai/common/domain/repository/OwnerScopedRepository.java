package com.ai.common.domain.repository;

import com.ai.common.domain.model.AbstractEmbeddable;
import com.ai.common.domain.model.OwnerKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository of named aggregates partitioned by owner, with names unique per owner. Every read and
 * delete takes the owner key, so one owner never sees another owner's rows.
 */
@NoRepositoryBean
public interface OwnerScopedRepository<E, I extends AbstractEmbeddable> extends Repository<E, I> {

  /** Finds the owner's entity by id. */
  Optional<E> findByIdAndOwnerKey(I id, OwnerKey ownerKey);

  /** Lists the owner's entities by name. */
  List<E> findAllByOwnerKeyOrderByNameAsc(OwnerKey ownerKey);

  /** Tells whether the owner already has an entity with the name. */
  boolean existsByOwnerKeyAndName(OwnerKey ownerKey, String name);

  /** Tells whether another of the owner's entities already has the name. */
  boolean existsByOwnerKeyAndNameAndIdNot(OwnerKey ownerKey, String name, I id);

  /** Tells whether the owner has an entity with the name other than {@code excludeId}. */
  default boolean existsByOwnerKeyAndNameIgnoringId(OwnerKey ownerKey, String name, I excludeId) {
    return excludeId == null
        ? existsByOwnerKeyAndName(ownerKey, name)
        : existsByOwnerKeyAndNameAndIdNot(ownerKey, name, excludeId);
  }

  /** Saves the entity and returns the stored copy. */
  <S extends E> S save(S entity);

  /** Deletes the owner's entity by id. */
  @Transactional
  void deleteByIdAndOwnerKey(I id, OwnerKey ownerKey);
}
