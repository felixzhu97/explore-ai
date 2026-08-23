package com.ai.pipeline.test.fixture;

import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.repository.WorkflowTemplateRepository;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import com.ai.testsupport.fake.AbstractClientIdOwnedFakeRepository;
import java.util.List;
import java.util.Optional;

/** In-memory {@link WorkflowTemplateRepository} for service-layer unit tests. */
public class FakeWorkflowTemplateRepository
    extends AbstractClientIdOwnedFakeRepository<SavedWorkflowTemplate, WorkflowTemplateId>
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
  public Optional<SavedWorkflowTemplate> findByIdAndClientId(
      WorkflowTemplateId id, String clientId) {
    return super.findByIdAndClientId(id, clientId);
  }

  @Override
  public List<SavedWorkflowTemplate> findAllByClientId(String clientId) {
    return super.findAllByClientId(clientId);
  }

  @Override
  public void deleteByIdAndClientId(WorkflowTemplateId id, String clientId) {
    super.deleteByIdAndClientId(id, clientId);
  }

  @Override
  public boolean existsByClientIdAndNameIgnoringId(
      String clientId, String name, WorkflowTemplateId excludeId) {
    return super.existsByClientIdAndNameIgnoringId(clientId, name, excludeId);
  }
}
