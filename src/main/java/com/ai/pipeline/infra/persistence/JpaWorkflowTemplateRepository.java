package com.ai.pipeline.infra.persistence;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.repository.WorkflowTemplateRepository;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for saved workflow templates. */
@Repository
public class JpaWorkflowTemplateRepository implements WorkflowTemplateRepository {

  private final SpringDataWorkflowTemplateRepository delegate;

  /** Documentation. */
  public JpaWorkflowTemplateRepository(SpringDataWorkflowTemplateRepository delegate) {
    this.delegate = delegate;
  }

  @Override
  @Transactional
  public SavedWorkflowTemplate save(SavedWorkflowTemplate template) {
    return delegate.saveAndFlush(template);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SavedWorkflowTemplate> findByIdAndClientId(
      WorkflowTemplateId id, String clientId) {
    return delegate.findByIdAndOwnerKey(id, OwnerKey.parse(clientId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SavedWorkflowTemplate> findAllByClientId(String clientId) {
    return delegate.findAllByOwnerKeyOrderByNameAsc(OwnerKey.parse(clientId));
  }

  @Override
  @Transactional
  public void deleteByIdAndClientId(WorkflowTemplateId id, String clientId) {
    delegate.deleteByIdAndOwnerKey(id, OwnerKey.parse(clientId));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByClientIdAndNameIgnoringId(
      String clientId, String name, WorkflowTemplateId excludeId) {
    OwnerKey ownerKey = OwnerKey.parse(clientId);
    if (excludeId == null) {
      return delegate.existsByOwnerKeyAndName(ownerKey, name);
    }
    return delegate.existsByOwnerKeyAndNameAndIdNot(ownerKey, name, excludeId);
  }
}
