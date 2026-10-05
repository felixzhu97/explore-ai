package com.ai.account.service;

import com.ai.account.domain.repository.OwnerPartitionRepository;
import com.ai.common.domain.vo.OwnerKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Moves a guest client's owner-partitioned data to the signed-in account's partition. */
@Service
@RequiredArgsConstructor
public class OwnerMergeService {

  private final OwnerPartitionRepository ownerPartitionRepository;

  /** Moves the guest client's owned rows to the signed-in account's Owner Key. */
  @Transactional
  public void mergeClientIntoAccount(String clientId, String accountUserId) {
    OwnerKey from = OwnerKey.forClient(clientId);
    OwnerKey to = OwnerKey.forAccount(accountUserId);
    if (from.equals(to)) {
      return;
    }
    ownerPartitionRepository.reassignOwner(from, to);
  }
}
