package com.ai.account.domain.repository;

import com.ai.common.domain.vo.OwnerKey;

/** Repository that reassigns or erases every row belonging to an owner partition. */
public interface OwnerPartitionRepository {
  /** Deletes all data stored for the owner. */
  void deleteAllForOwner(OwnerKey owner);

  /** Moves all data from one owner to another. */
  void reassignOwner(OwnerKey from, OwnerKey to);
}
