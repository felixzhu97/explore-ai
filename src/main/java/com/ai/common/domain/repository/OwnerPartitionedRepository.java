package com.ai.common.domain.repository;

import com.ai.common.domain.model.OwnerKey;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * Bulk operations over every row of one Owner Key partition, used by a feature when it handles
 * owner erasure or guest merge events. Callers own the transaction.
 *
 * @param <E> owner-aware entity type
 * @param <I> id type
 */
@NoRepositoryBean
public interface OwnerPartitionedRepository<E, I> extends Repository<E, I> {

  /**
   * Deletes every row of the owner.
   *
   * @param ownerKey owner partition
   * @return deleted row count
   */
  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM #{#entityName} e WHERE e.ownerKey = ?1")
  int deleteAllByOwnerKey(OwnerKey ownerKey);

  /**
   * Moves every row of one owner to another.
   *
   * @param from current owner partition
   * @param to new owner partition
   * @return moved row count
   */
  @Modifying(flushAutomatically = true)
  @Query("UPDATE #{#entityName} e SET e.ownerKey = ?2 WHERE e.ownerKey = ?1")
  int reassignOwnerKey(OwnerKey from, OwnerKey to);
}
