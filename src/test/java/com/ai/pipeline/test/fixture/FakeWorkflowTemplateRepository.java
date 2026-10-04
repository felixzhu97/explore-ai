package com.ai.pipeline.test.fixture;

import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.repository.WorkflowTemplateRepository;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import com.ai.testsupport.fake.AbstractOwnerScopedFakeRepository;
import java.util.List;
import java.util.Optional;

/** In-memory {@link WorkflowTemplateRepository} for service-layer unit tests. */
public class FakeWorkflowTemplateRepository
    extends AbstractOwnerScopedFakeRepository<SavedWorkflowTemplate, WorkflowTemplateId>
    implements WorkflowTemplateRepository {

  @Override
  protected WorkflowTemplateId getId(SavedWorkflowTemplate entity) {
    return entity.getId();
  }

  @Override
  protected String getClientId(SavedWorkflowTemplate entity) {
    return entity.getClientId();
  }

  @Override
  protected String getName(SavedWorkflowTemplate entity) {
    return entity.getName();
  }

  @Override
  public SavedWorkflowTemplate save(SavedWorkflowTemplate template) {
    return super.save(template);
  }

  @Override
  public Optional<SavedWorkflowTemplate> findByIdAndOwnerKey(
      WorkflowTemplateId id, String ownerKey) {
    return super.findByIdAndOwnerKey(id, ownerKey);
  }

  @Override
  public List<SavedWorkflowTemplate> findAllByOwnerKey(String ownerKey) {
    return super.findAllByOwnerKey(ownerKey);
  }

  @Override
  public void deleteByIdAndOwnerKey(WorkflowTemplateId id, String ownerKey) {
    super.deleteByIdAndOwnerKey(id, ownerKey);
  }

  @Override
  public boolean existsByOwnerKeyAndNameIgnoringId(
      String ownerKey, String name, WorkflowTemplateId excludeId) {
    return super.existsByOwnerKeyAndNameIgnoringId(ownerKey, name, excludeId);
  }
}
