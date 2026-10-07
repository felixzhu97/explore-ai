package com.ai.pipeline.test.fixture;

import com.ai.pipeline.domain.model.PipelineTemplate;
import com.ai.pipeline.domain.model.PipelineTemplateId;
import com.ai.pipeline.domain.repository.PipelineTemplateRepository;
import com.ai.testsupport.fake.AbstractOwnerScopedFakeRepository;
import java.util.List;
import java.util.Optional;

/** In-memory {@link PipelineTemplateRepository} for service-layer unit tests. */
public class FakePipelineTemplateRepository
    extends AbstractOwnerScopedFakeRepository<PipelineTemplate, PipelineTemplateId>
    implements PipelineTemplateRepository {

  @Override
  protected PipelineTemplateId getId(PipelineTemplate entity) {
    return entity.getId();
  }

  @Override
  protected String getOwnerKeyValue(PipelineTemplate entity) {
    return entity.getOwnerKeyValue();
  }

  @Override
  protected String getName(PipelineTemplate entity) {
    return entity.getName();
  }

  @Override
  public PipelineTemplate save(PipelineTemplate template) {
    return super.save(template);
  }

  @Override
  public Optional<PipelineTemplate> findByIdAndOwnerKey(PipelineTemplateId id, String ownerKey) {
    return super.findByIdAndOwnerKey(id, ownerKey);
  }

  @Override
  public List<PipelineTemplate> findAllByOwnerKey(String ownerKey) {
    return super.findAllByOwnerKey(ownerKey);
  }

  @Override
  public void deleteByIdAndOwnerKey(PipelineTemplateId id, String ownerKey) {
    super.deleteByIdAndOwnerKey(id, ownerKey);
  }

  @Override
  public boolean existsByOwnerKeyAndNameIgnoringId(
      String ownerKey, String name, PipelineTemplateId excludeId) {
    return super.existsByOwnerKeyAndNameIgnoringId(ownerKey, name, excludeId);
  }
}
