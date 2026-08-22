package com.ai.pipeline.infra.persistence;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.pipeline.domain.model.SavedAgentDefinition;
import com.ai.pipeline.domain.repository.SavedAgentRepository;
import com.ai.pipeline.domain.vo.SavedAgentId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for saved pipeline agent definitions. */
@Repository
public class JpaSavedAgentRepository implements SavedAgentRepository {

  private final SpringDataSavedAgentRepository delegate;

  /** Documentation. */
  public JpaSavedAgentRepository(SpringDataSavedAgentRepository delegate) {
    this.delegate = delegate;
  }

  @Override
  @Transactional
  public SavedAgentDefinition save(SavedAgentDefinition agent) {
    return delegate.saveAndFlush(agent);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SavedAgentDefinition> findByIdAndClientId(SavedAgentId id, String clientId) {
    return delegate.findByIdAndOwnerKey(id, OwnerKey.parse(clientId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SavedAgentDefinition> findAllByClientId(String clientId) {
    return delegate.findAllByOwnerKeyOrderByNameAsc(OwnerKey.parse(clientId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SavedAgentDefinition> findEnabledByClientId(String clientId) {
    return delegate.findAllByOwnerKeyAndEnabledTrueOrderByNameAsc(OwnerKey.parse(clientId));
  }

  @Override
  @Transactional
  public void deleteByIdAndClientId(SavedAgentId id, String clientId) {
    delegate.deleteByIdAndOwnerKey(id, OwnerKey.parse(clientId));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByClientIdAndTypeKeyIgnoringId(
      String clientId, String typeKey, SavedAgentId excludeId) {
    OwnerKey ownerKey = OwnerKey.parse(clientId);
    if (excludeId == null) {
      return delegate.existsByOwnerKeyAndTypeKey(ownerKey, typeKey);
    }
    return delegate.existsByOwnerKeyAndTypeKeyAndIdNot(ownerKey, typeKey, excludeId);
  }
}
