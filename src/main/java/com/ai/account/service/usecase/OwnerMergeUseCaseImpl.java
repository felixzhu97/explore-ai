package com.ai.account.service.usecase;

import com.ai.account.domain.repository.OwnerPartitionRepository;
import com.ai.common.domain.vo.OwnerKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Documentation. */
@Service
public class OwnerMergeUseCaseImpl implements OwnerMergeUseCase {

  private final OwnerPartitionRepository ownerPartitionRepository;

  /** Documentation. */
  public OwnerMergeUseCaseImpl(OwnerPartitionRepository ownerPartitionRepository) {
    this.ownerPartitionRepository = ownerPartitionRepository;
  }

  @Override
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
