package com.ai.account.domain.repository;

import com.ai.common.domain.vo.OwnerKey;

public interface OwnerPartitionRepository {

    void reassignOwner(OwnerKey from, OwnerKey to);

    void deleteAllForOwner(OwnerKey owner);
}
