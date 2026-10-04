package com.ai.account.domain.repository;

import com.ai.common.domain.vo.OwnerKey;

/** Repository that reassigns or erases every row belonging to an owner partition. */
public interface OwnerPartitionRepository {
  void reassignOwner(OwnerKey from, OwnerKey to);

  void deleteAllForOwner(OwnerKey owner);
}
