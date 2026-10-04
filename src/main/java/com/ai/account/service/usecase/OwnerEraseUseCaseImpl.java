package com.ai.account.service.usecase;

import com.ai.account.domain.repository.OwnerPartitionRepository;
import com.ai.common.domain.vo.OwnerKey;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deletes all data stored under an owner key in a single transaction. */
@Service
public class OwnerEraseUseCaseImpl implements OwnerEraseUseCase {

  private final OwnerPartitionRepository ownerPartitionRepository;

  public OwnerEraseUseCaseImpl(OwnerPartitionRepository ownerPartitionRepository) {
    this.ownerPartitionRepository = ownerPartitionRepository;
  }

  @Override
  @Transactional
  public void eraseAllForOwner(OwnerKey ownerKey) {
    Objects.requireNonNull(ownerKey, "ownerKey");
    ownerPartitionRepository.deleteAllForOwner(ownerKey);
  }
}
