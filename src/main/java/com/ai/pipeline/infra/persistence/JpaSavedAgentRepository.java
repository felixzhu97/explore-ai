package com.ai.pipeline.infra.persistence;

import com.ai.common.domain.model.OwnerKey;
import com.ai.common.infra.persistence.OwnerPartitionScope;
import com.ai.pipeline.domain.model.SavedAgent;
import com.ai.pipeline.domain.model.SavedAgentId;
import com.ai.pipeline.domain.repository.SavedAgentRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for saved pipeline agent definitions. */
@Repository
@RequiredArgsConstructor
public class JpaSavedAgentRepository implements SavedAgentRepository {

  private final SpringDataSavedAgentRepository delegate;
  private static final Sort BY_NAME = Sort.by("name");

  private final OwnerPartitionScope ownerPartition;

  @Override
  @Transactional(readOnly = true)
  public Optional<SavedAgent> findByIdAndOwnerKey(SavedAgentId id, String ownerKey) {
    return ownerPartition.findOne(OwnerKey.parse(ownerKey), () -> delegate.findById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SavedAgent> findAllByOwnerKey(String ownerKey) {
    return ownerPartition.apply(OwnerKey.parse(ownerKey), () -> delegate.findAll(BY_NAME));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SavedAgent> findEnabledByOwnerKey(String ownerKey) {
    return ownerPartition.apply(
        OwnerKey.parse(ownerKey), delegate::findAllByEnabledTrueOrderByNameAsc);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByOwnerKeyAndTypeKeyIgnoringId(
      String ownerKey, String typeKey, SavedAgentId excludeId) {
    return ownerPartition.apply(
        OwnerKey.parse(ownerKey),
        () ->
            excludeId == null
                ? delegate.existsByTypeKey(typeKey)
                : delegate.existsByTypeKeyAndIdNot(typeKey, excludeId));
  }

  @Override
  @Transactional
  public SavedAgent save(SavedAgent agent) {
    return delegate.saveAndFlush(agent);
  }

  @Override
  @Transactional
  public void deleteByIdAndOwnerKey(SavedAgentId id, String ownerKey) {
    ownerPartition.run(OwnerKey.parse(ownerKey), () -> delegate.deleteById(id));
  }
}
