package com.ai.account.service;

import com.ai.account.domain.repository.OwnerPartitionRepository;
import com.ai.common.domain.vo.OwnerKey;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deletes all data stored under an owner key in a single transaction. */
@Service
@RequiredArgsConstructor
public class OwnerErasureService {

  private final OwnerPartitionRepository ownerPartitionRepository;

  @Transactional
  public void eraseAllForOwner(OwnerKey ownerKey) {
    Objects.requireNonNull(ownerKey, "ownerKey");
    ownerPartitionRepository.deleteAllForOwner(ownerKey);
  }
}
