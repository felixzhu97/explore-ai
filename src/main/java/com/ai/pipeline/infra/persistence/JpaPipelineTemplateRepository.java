package com.ai.pipeline.infra.persistence;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.infra.persistence.OwnerPartitionScope;
import com.ai.pipeline.domain.model.PipelineTemplate;
import com.ai.pipeline.domain.repository.PipelineTemplateRepository;
import com.ai.pipeline.domain.vo.PipelineTemplateId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for saved pipeline templates. */
@Repository
public class JpaPipelineTemplateRepository implements PipelineTemplateRepository {

  private final SpringDataPipelineTemplateRepository delegate;
  private static final Sort BY_NAME = Sort.by("name");

  private final OwnerPartitionScope ownerPartition;

  public JpaPipelineTemplateRepository(
      SpringDataPipelineTemplateRepository delegate, OwnerPartitionScope ownerPartition) {
    this.delegate = delegate;
    this.ownerPartition = ownerPartition;
  }

  @Override
  @Transactional
  public PipelineTemplate save(PipelineTemplate template) {
    return delegate.saveAndFlush(template);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<PipelineTemplate> findByIdAndOwnerKey(PipelineTemplateId id, String ownerKey) {
    return ownerPartition.findOne(OwnerKey.parse(ownerKey), () -> delegate.findById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<PipelineTemplate> findAllByOwnerKey(String ownerKey) {
    return ownerPartition.apply(OwnerKey.parse(ownerKey), () -> delegate.findAll(BY_NAME));
  }

  @Override
  @Transactional
  public void deleteByIdAndOwnerKey(PipelineTemplateId id, String ownerKey) {
    ownerPartition.run(OwnerKey.parse(ownerKey), () -> delegate.deleteById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByOwnerKeyAndNameIgnoringId(
      String ownerKey, String name, PipelineTemplateId excludeId) {
    return ownerPartition.apply(
        OwnerKey.parse(ownerKey),
        () ->
            excludeId == null
                ? delegate.existsByName(name)
                : delegate.existsByNameAndIdNot(name, excludeId));
  }
}
