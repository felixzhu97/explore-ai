package com.ai.pipeline.infra.persistence;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.infra.persistence.OwnerPartitionScope;
import com.ai.pipeline.domain.model.SavedAgentDefinition;
import com.ai.pipeline.domain.repository.SavedAgentRepository;
import com.ai.pipeline.domain.vo.SavedAgentId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for saved pipeline agent definitions. */
@Repository
public class JpaSavedAgentRepository implements SavedAgentRepository {

  private final SpringDataSavedAgentRepository delegate;
  private static final Sort BY_NAME = Sort.by("name");

  private final OwnerPartitionScope ownerPartition;

  /** Documentation. */
  public JpaSavedAgentRepository(
      SpringDataSavedAgentRepository delegate, OwnerPartitionScope ownerPartition) {
    this.delegate = delegate;
    this.ownerPartition = ownerPartition;
  }

  @Override
  @Transactional
  public SavedAgentDefinition save(SavedAgentDefinition agent) {
    return delegate.saveAndFlush(agent);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SavedAgentDefinition> findByIdAndClientId(SavedAgentId id, String clientId) {
    return ownerPartition.findOne(OwnerKey.parse(clientId), () -> delegate.findById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SavedAgentDefinition> findAllByClientId(String clientId) {
    return ownerPartition.apply(OwnerKey.parse(clientId), () -> delegate.findAll(BY_NAME));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SavedAgentDefinition> findEnabledByClientId(String clientId) {
    return ownerPartition.apply(
        OwnerKey.parse(clientId), delegate::findAllByEnabledTrueOrderByNameAsc);
  }

  @Override
  @Transactional
  public void deleteByIdAndClientId(SavedAgentId id, String clientId) {
    ownerPartition.run(OwnerKey.parse(clientId), () -> delegate.deleteById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByClientIdAndTypeKeyIgnoringId(
      String clientId, String typeKey, SavedAgentId excludeId) {
    return ownerPartition.apply(
        OwnerKey.parse(clientId),
        () ->
            excludeId == null
                ? delegate.existsByTypeKey(typeKey)
                : delegate.existsByTypeKeyAndIdNot(typeKey, excludeId));
  }
}
