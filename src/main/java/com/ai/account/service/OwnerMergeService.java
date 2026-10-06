package com.ai.account.service;

import com.ai.account.domain.model.AccountUser;
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

  /** Moves the linked browser's guest rows to the account's Owner Key. */
  @Transactional
  public void mergeGuestIntoAccount(AccountUser user) {
    OwnerKey from =
        user.guestOwnerKey()
            .orElseThrow(() -> new IllegalStateException("account is not linked to a browser"));
    OwnerKey to = user.ownerKey();
    from.requireMergeableInto(to);
    ownerPartitionRepository.reassignOwner(from, to);
  }
}
