package com.ai.account.domain.repository;

import com.ai.common.domain.vo.OwnerKey;

/** Documentation. */
public interface OwnerPartitionRepository {
  /** Documentation. */
  void reassignOwner(OwnerKey from, OwnerKey to);

  /** Documentation. */
  void deleteAllForOwner(OwnerKey owner);
}
